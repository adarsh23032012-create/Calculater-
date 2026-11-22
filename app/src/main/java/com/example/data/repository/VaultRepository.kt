package com.example.data.repository

import com.example.data.apps.InstalledAppsDataSource
import com.example.data.local.CalcHistoryEntity
import com.example.data.local.HiddenAppEntity
import com.example.data.local.VaultDao
import com.example.data.preferences.VaultPreferences
import com.example.data.preferences.VaultSettingsState
import com.example.model.CalcHistoryItem
import com.example.model.HiddenAppItem
import com.example.model.InstalledAppInfo
import com.example.security.CryptoDiagnostics
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

class VaultRepository(
  private val vaultDao: VaultDao,
  private val preferences: VaultPreferences,
  private val cryptoManager: VaultCryptoManager,
  private val appsDataSource: InstalledAppsDataSource,
) {

  private val _isVaultUnlocked = MutableStateFlow(false)
  val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

  val settingsFlow: Flow<VaultSettingsState> = preferences.settingsFlow

  val calcHistoryFlow: Flow<List<CalcHistoryItem>> =
    vaultDao.observeCalcHistory().map { list ->
      list.map {
        CalcHistoryItem(
          id = it.id,
          expression = it.expression,
          result = it.result,
          timestampEpochMs = it.timestampEpochMs,
        )
      }
    }

  val hiddenAppsFlow: Flow<List<HiddenAppItem>> =
    combine(vaultDao.observeHiddenApps(), _isVaultUnlocked) { entities, unlocked ->
      if (!unlocked) {
        emptyList()
      } else {
        withContext(Dispatchers.Default) {
          entities.mapNotNull { entity -> decryptEntityToModel(entity) }
        }
      }
    }

  val hiddenPackageHashesFlow: Flow<Set<String>> =
    vaultDao.observeHiddenApps().map { list -> list.map { it.packageNameHash }.toSet() }

  suspend fun tryUnlockWithEquation(rawExpression: String): Boolean =
    withContext(Dispatchers.Default) {
      val (storedHash, storedSalt) = preferences.getStoredEquationHashAndSalt() ?: return@withContext false
      val matched = cryptoManager.verifyEquation(rawExpression, storedHash, storedSalt)
      if (matched) {
        _isVaultUnlocked.value = true
      }
      matched
    }

  fun lockVault() {
    _isVaultUnlocked.value = false
  }

  suspend fun updateSecretEquation(newEquation: String) {
    withContext(Dispatchers.Default) { preferences.updateEquation(newEquation) }
  }

  suspend fun setStealthMode(enabled: Boolean) = preferences.setStealthMode(enabled)

  suspend fun setAutoLockOnBackground(enabled: Boolean) =
    preferences.setAutoLockOnBackground(enabled)

  suspend fun loadInstalledApps(): List<InstalledAppInfo> = appsDataSource.loadLaunchableApps()

  fun packageHash(packageName: String): String = cryptoManager.sha256Hex(packageName)

  suspend fun hideApps(apps: List<InstalledAppInfo>) =
    withContext(Dispatchers.Default) {
      val now = System.currentTimeMillis()
      apps.forEachIndexed { idx, app ->
        val hash = cryptoManager.sha256Hex(app.packageName)
        val existing = vaultDao.getHiddenAppByHash(hash)
        if (existing == null) {
          val json =
            JSONObject().apply {
              put("packageName", app.packageName)
              put("originalLabel", app.label)
              put("cloakAlias", "")
              put("securityNote", "")
              put("hiddenAtEpochMs", now)
              put("launchCount", 0)
              put("lastLaunchedEpochMs", JSONObject.NULL)
            }
          val encrypted = cryptoManager.encryptString(json.toString())
          vaultDao.upsertHiddenApp(
            HiddenAppEntity(
              packageNameHash = hash,
              encryptedPayloadB64 = encrypted.ciphertextB64,
              ivB64 = encrypted.ivB64,
              addedAtEpochMs = now + idx,
            )
          )
        }
      }
    }

  suspend fun updateCloakAndNote(packageName: String, cloakAlias: String, securityNote: String) =
    withContext(Dispatchers.Default) {
      val hash = cryptoManager.sha256Hex(packageName)
      val existing = vaultDao.getHiddenAppByHash(hash) ?: return@withContext
      val decrypted = cryptoManager.decryptString(existing.encryptedPayloadB64, existing.ivB64)
      val json = JSONObject(decrypted)
      json.put("cloakAlias", cloakAlias.trim())
      json.put("securityNote", securityNote.trim())
      val reEncrypted = cryptoManager.encryptString(json.toString())
      vaultDao.upsertHiddenApp(
        existing.copy(encryptedPayloadB64 = reEncrypted.ciphertextB64, ivB64 = reEncrypted.ivB64)
      )
    }

  suspend fun launchHiddenApp(packageName: String): Boolean {
    val launched = appsDataSource.launchApp(packageName)
    if (launched) {
      withContext(Dispatchers.Default) {
        val hash = cryptoManager.sha256Hex(packageName)
        val existing = vaultDao.getHiddenAppByHash(hash) ?: return@withContext
        val decrypted = cryptoManager.decryptString(existing.encryptedPayloadB64, existing.ivB64)
        val json = JSONObject(decrypted)
        val count = json.optInt("launchCount", 0) + 1
        json.put("launchCount", count)
        json.put("lastLaunchedEpochMs", System.currentTimeMillis())
        val reEncrypted = cryptoManager.encryptString(json.toString())
        vaultDao.upsertHiddenApp(
          existing.copy(
            encryptedPayloadB64 = reEncrypted.ciphertextB64,
            ivB64 = reEncrypted.ivB64,
          )
        )
      }
    }
    return launched
  }

  fun openSystemAppInfo(packageName: String): Boolean =
    appsDataSource.openAppSystemInfo(packageName)

  suspend fun unhideApp(packageName: String) {
    val hash = cryptoManager.sha256Hex(packageName)
    vaultDao.deleteHiddenAppByHash(hash)
  }

  suspend fun wipeVault() {
    vaultDao.wipeAllHiddenApps()
    preferences.resetPreferences()
    lockVault()
  }

  suspend fun recordCalculation(expression: String, result: String) {
    vaultDao.insertCalcHistory(
      CalcHistoryEntity(
        expression = expression,
        result = result,
        timestampEpochMs = System.currentTimeMillis(),
      )
    )
  }

  suspend fun clearCalcHistory() {
    vaultDao.clearCalcHistory()
  }

  fun getCryptoDiagnostics(): CryptoDiagnostics = cryptoManager.diagnostics()

  private fun decryptEntityToModel(entity: HiddenAppEntity): HiddenAppItem? {
    return try {
      val plaintext = cryptoManager.decryptString(entity.encryptedPayloadB64, entity.ivB64)
      val json = JSONObject(plaintext)
      val packageName = json.getString("packageName")
      val originalLabel = json.optString("originalLabel", packageName)
      val cloakAlias = json.optString("cloakAlias", "")
      val securityNote = json.optString("securityNote", "")
      val hiddenAt = json.optLong("hiddenAtEpochMs", entity.addedAtEpochMs)
      val launchCount = json.optInt("launchCount", 0)
      val lastLaunched =
        if (json.isNull("lastLaunchedEpochMs")) null else json.optLong("lastLaunchedEpochMs")
      val cipherPreview = entity.encryptedPayloadB64.take(24) + "…"
      val ivPreview = entity.ivB64.take(12) + "…"

      HiddenAppItem(
        packageName = packageName,
        originalLabel = originalLabel,
        cloakAlias = cloakAlias,
        securityNote = securityNote,
        hiddenAtEpochMs = hiddenAt,
        launchCount = launchCount,
        lastLaunchedEpochMs = lastLaunched,
        encryptedCipherPreview = cipherPreview,
        ivPreview = ivPreview,
        isInstalled = appsDataSource.isAppInstalled(packageName),
        icon = appsDataSource.loadAppIconSync(packageName),
      )
    } catch (_: Exception) {
      null
    }
  }
}
