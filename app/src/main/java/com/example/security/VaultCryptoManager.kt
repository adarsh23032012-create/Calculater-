package com.example.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.calculator.CalculatorEngine
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class EncryptedPayload(
  val ciphertextB64: String,
  val ivB64: String,
)

data class EquationHashResult(
  val hashB64: String,
  val saltB64: String,
)

data class CryptoDiagnostics(
  val keystoreAlias: String,
  val cipherSuite: String,
  val keyDerivation: String,
  val providerName: String,
)

@OptIn(ExperimentalEncodingApi::class)
class VaultCryptoManager(
  private val fallbackKeyOverride: SecretKey? = null,
) {

  private val secureRandom = SecureRandom()

  fun getOrCreateAesKey(): Pair<SecretKey, String> {
    if (fallbackKeyOverride != null) {
      return fallbackKeyOverride to "InMemoryTestProvider"
    }

    return try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
      val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
      if (existingKey != null) {
        existingKey to ANDROID_KEYSTORE
      } else {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec =
          KeyGenParameterSpec.Builder(
              KEY_ALIAS,
              KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey() to ANDROID_KEYSTORE
      }
    } catch (e: Exception) {
      // Robolectric / JVM fallback only when AndroidKeyStore is absent in unit tests
      if (isRobolectricOrJvmTest()) {
        getOrCreateTestKey() to "JVM-AES-256-Fallback"
      } else {
        throw SecurityException("AndroidKeyStore unavailable for vault encryption", e)
      }
    }
  }

  fun encryptString(plaintext: String): EncryptedPayload {
    val (secretKey, _) = getOrCreateAesKey()
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    val iv = cipher.iv
    return EncryptedPayload(
      ciphertextB64 = Base64.encode(ciphertext),
      ivB64 = Base64.encode(iv),
    )
  }

  fun decryptString(ciphertextB64: String, ivB64: String): String {
    val (secretKey, _) = getOrCreateAesKey()
    val ciphertext = Base64.decode(ciphertextB64)
    val iv = Base64.decode(ivB64)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
    val plaintextBytes = cipher.doFinal(ciphertext)
    return String(plaintextBytes, Charsets.UTF_8)
  }

  fun hashEquation(equation: String, existingSaltB64: String? = null): EquationHashResult {
    val normalized = CalculatorEngine.normalizeEquation(equation)
    val salt =
      if (existingSaltB64 != null) {
        Base64.decode(existingSaltB64)
      } else {
        ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
      }
    val spec = PBEKeySpec(normalized.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
    val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
    val derived = factory.generateSecret(spec).encoded
    spec.clearPassword()
    return EquationHashResult(
      hashB64 = Base64.encode(derived),
      saltB64 = Base64.encode(salt),
    )
  }

  fun verifyEquation(inputEquation: String, storedHashB64: String, storedSaltB64: String): Boolean {
    val normalized = CalculatorEngine.normalizeEquation(inputEquation)
    if (normalized.isEmpty()) return false
    val candidate = hashEquation(normalized, storedSaltB64)
    val expectedBytes = Base64.decode(storedHashB64)
    val actualBytes = Base64.decode(candidate.hashB64)
    return MessageDigest.isEqual(expectedBytes, actualBytes)
  }

  fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun diagnostics(): CryptoDiagnostics {
    val (_, provider) = getOrCreateAesKey()
    return CryptoDiagnostics(
      keystoreAlias = KEY_ALIAS,
      cipherSuite = "AES/GCM/NoPadding (256-bit)",
      keyDerivation = "PBKDF2-HMAC-SHA256 ($PBKDF2_ITERATIONS rounds)",
      providerName = provider,
    )
  }

  private fun isRobolectricOrJvmTest(): Boolean =
    Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

  private fun getOrCreateTestKey(): SecretKey {
    val bytes = ByteArray(32) { index -> (index * 7 + 13).toByte() }
    return SecretKeySpec(bytes, "AES")
  }

  companion object {
    const val DEFAULT_EQUATION = "1234+0"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "calc_vault_aes256_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SALT_BYTES = 16
    private const val PBKDF2_ITERATIONS = 65_536
    private const val PBKDF2_KEY_BITS = 256
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
  }
}
