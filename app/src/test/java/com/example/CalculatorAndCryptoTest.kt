package com.example

import com.example.calculator.CalculatorEngine
import com.example.security.VaultCryptoManager
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorAndCryptoTest {

  @Test
  fun `calculator evaluates PEMDAS arithmetic and edge cases accurately`() {
    assertEquals(CalculatorEngine.EvalResult.Success("14"), CalculatorEngine.evaluate("2+3×4"))
    assertEquals(CalculatorEngine.EvalResult.Success("2.5"), CalculatorEngine.evaluate("5÷2"))
    assertEquals(CalculatorEngine.EvalResult.Success("1234"), CalculatorEngine.evaluate("1234+0"))
    assertEquals(CalculatorEngine.EvalResult.Success("-6"), CalculatorEngine.evaluate("-2×3"))
    assertEquals(CalculatorEngine.EvalResult.Success("0.5"), CalculatorEngine.evaluate("50%"))
    assertEquals(CalculatorEngine.EvalResult.Error, CalculatorEngine.evaluate("10÷0"))
    assertEquals("14", CalculatorEngine.preview("2+3×4"))
    assertEquals("", CalculatorEngine.preview("1234"))
  }

  @Test
  fun `vault crypto encrypts and decrypts payloads with unique IVs and rejects tampering`() {
    val aesKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    val crypto = VaultCryptoManager(fallbackKeyOverride = aesKey)

    val plaintext = """{"packageName":"com.secret.app","cloakAlias":"Notes"}"""
    val first = crypto.encryptString(plaintext)
    val second = crypto.encryptString(plaintext)

    assertNotEquals(first.ivB64, second.ivB64)
    assertNotEquals(first.ciphertextB64, second.ciphertextB64)
    assertEquals(plaintext, crypto.decryptString(first.ciphertextB64, first.ivB64))
    assertEquals(plaintext, crypto.decryptString(second.ciphertextB64, second.ivB64))

    // GCM tag rejects cross-IV decryption
    assertThrows(AEADBadTagException::class.java) {
      crypto.decryptString(first.ciphertextB64, second.ivB64)
    }
  }

  @Test
  fun `pbkdf2 equation hash verifies normalized equations and rejects wrong ones`() {
    val aesKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    val crypto = VaultCryptoManager(fallbackKeyOverride = aesKey)

    val stored = crypto.hashEquation("42×2")
    assertTrue(crypto.verifyEquation("42*2", stored.hashB64, stored.saltB64))
    assertTrue(crypto.verifyEquation("42×2", stored.hashB64, stored.saltB64))
    assertFalse(crypto.verifyEquation("42+2", stored.hashB64, stored.saltB64))
  }
}
