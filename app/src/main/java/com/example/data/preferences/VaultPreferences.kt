package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.VaultCryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vaultDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "calc_vault_preferences")

data class VaultSettingsState(
  val hasCustomizedVault: Boolean = false,
  val stealthMode: Boolean = true,
  val autoLockOnBackground: Boolean = true,
  val showCipherInspector: Boolean = true,
)

class VaultPreferences(
  private val context: Context,
  private val cryptoManager: VaultCryptoManager,
) {
  private val dataStore = context.vaultDataStore

  val settingsFlow: Flow<VaultSettingsState> =
    dataStore.data.map { prefs ->
      val hasHash = !prefs[KEY_EQUATION_HASH].isNullOrBlank()
      VaultSettingsState(
        hasCustomizedVault = (prefs[KEY_HAS_CUSTOMIZED_VAULT] ?: false) && hasHash,
        stealthMode = prefs[KEY_STEALTH_MODE] ?: true,
        autoLockOnBackground = prefs[KEY_AUTO_LOCK] ?: true,
        showCipherInspector = prefs[KEY_SHOW_CIPHER_INSPECTOR] ?: true,
      )
    }

  suspend fun getStoredEquationHashAndSalt(): Pair<String, String>? {
    val current = dataStore.data.first()
    val existingHash = current[KEY_EQUATION_HASH]
    val existingSalt = current[KEY_EQUATION_SALT]
    if (existingHash.isNullOrBlank() || existingSalt.isNullOrBlank()) {
      return null
    }
    return existingHash to existingSalt
  }

  suspend fun updateEquation(newEquation: String) {
    val generated = cryptoManager.hashEquation(newEquation)
    dataStore.edit { prefs ->
      prefs[KEY_EQUATION_HASH] = generated.hashB64
      prefs[KEY_EQUATION_SALT] = generated.saltB64
      prefs[KEY_HAS_CUSTOMIZED_VAULT] = true
      prefs[KEY_STEALTH_MODE] = true
    }
  }

  suspend fun setStealthMode(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[KEY_STEALTH_MODE] = enabled }
  }

  suspend fun setAutoLockOnBackground(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[KEY_AUTO_LOCK] = enabled }
  }

  suspend fun resetPreferences() {
    dataStore.edit { prefs ->
      prefs.remove(KEY_EQUATION_HASH)
      prefs.remove(KEY_EQUATION_SALT)
      prefs[KEY_HAS_CUSTOMIZED_VAULT] = false
      prefs[KEY_STEALTH_MODE] = true
      prefs[KEY_AUTO_LOCK] = true
    }
  }

  companion object {
    private val KEY_EQUATION_HASH = stringPreferencesKey("equation_hash_b64")
    private val KEY_EQUATION_SALT = stringPreferencesKey("equation_salt_b64")
    private val KEY_HAS_CUSTOMIZED_VAULT = booleanPreferencesKey("has_customized_vault")
    private val KEY_STEALTH_MODE = booleanPreferencesKey("stealth_mode")
    private val KEY_AUTO_LOCK = booleanPreferencesKey("auto_lock_on_background")
    private val KEY_SHOW_CIPHER_INSPECTOR = booleanPreferencesKey("show_cipher_inspector")
  }
}
