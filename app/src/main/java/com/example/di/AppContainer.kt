package com.example.di

import android.content.Context
import com.example.data.apps.InstalledAppsDataSource
import com.example.data.local.VaultDatabase
import com.example.data.preferences.VaultPreferences
import com.example.data.repository.VaultRepository
import com.example.security.VaultCryptoManager

class AppContainer(context: Context) {
  private val appContext = context.applicationContext
  val cryptoManager: VaultCryptoManager by lazy { VaultCryptoManager() }
  private val database: VaultDatabase by lazy { VaultDatabase.getInstance(appContext) }
  private val preferences: VaultPreferences by lazy { VaultPreferences(appContext, cryptoManager) }
  private val appsDataSource: InstalledAppsDataSource by lazy { InstalledAppsDataSource(appContext) }

  val vaultRepository: VaultRepository by lazy {
    VaultRepository(
      vaultDao = database.vaultDao(),
      preferences = preferences,
      cryptoManager = cryptoManager,
      appsDataSource = appsDataSource,
    )
  }
}
