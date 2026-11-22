package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calculator.CalculatorEngine
import com.example.data.preferences.VaultSettingsState
import com.example.data.repository.VaultRepository
import com.example.model.HiddenAppItem
import com.example.model.InstalledAppInfo
import com.example.security.CryptoDiagnostics
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppFilterMode {
  ALL,
  USER,
  SYSTEM,
}

data class VaultUiState(
  val isUnlocked: Boolean = false,
  val hiddenApps: List<HiddenAppItem> = emptyList(),
  val searchQuery: String = "",
  val settings: VaultSettingsState = VaultSettingsState(),
  val diagnostics: CryptoDiagnostics? = null,
) {
  val filteredApps: List<HiddenAppItem>
    get() =
      if (searchQuery.isBlank()) {
        hiddenApps
      } else {
        val q = searchQuery.trim().lowercase()
        hiddenApps.filter {
          it.displayTitle.lowercase().contains(q) ||
            it.originalLabel.lowercase().contains(q) ||
            it.packageName.lowercase().contains(q)
        }
      }
}

sealed class PickerLoadState {
  data object Loading : PickerLoadState()

  data class Ready(
    val apps: List<InstalledAppInfo>,
    val hiddenHashes: Set<String>,
  ) : PickerLoadState()

  data class Error(val message: String) : PickerLoadState()
}

data class AppPickerUiState(
  val loadState: PickerLoadState = PickerLoadState.Loading,
  val searchQuery: String = "",
  val filterMode: AppFilterMode = AppFilterMode.ALL,
  val selectedPackages: Set<String> = emptySet(),
  val isEncrypting: Boolean = false,
)

sealed class VaultBootstrapState {
  data object Loading : VaultBootstrapState()

  data object NeedsPasswordSetup : VaultBootstrapState()

  data object Ready : VaultBootstrapState()
}

class VaultViewModel(
  private val repository: VaultRepository,
) : ViewModel() {

  private val searchQuery = MutableStateFlow("")
  private val diagnosticsState = MutableStateFlow(repository.getCryptoDiagnostics())

  val bootstrapState: StateFlow<VaultBootstrapState> =
    repository.settingsFlow
      .combine(MutableStateFlow(Unit)) { settings, _ ->
        if (settings.hasCustomizedVault) VaultBootstrapState.Ready
        else VaultBootstrapState.NeedsPasswordSetup
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = VaultBootstrapState.Loading,
      )

  val uiState: StateFlow<VaultUiState> =
    combine(
        repository.isVaultUnlocked,
        repository.hiddenAppsFlow,
        searchQuery,
        repository.settingsFlow,
        diagnosticsState,
      ) { unlocked, apps, query, settings, diagnostics ->
        VaultUiState(
          isUnlocked = unlocked,
          hiddenApps = apps,
          searchQuery = query,
          settings = settings,
          diagnostics = diagnostics,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultUiState(diagnostics = repository.getCryptoDiagnostics()),
      )

  private val pickerInternal = MutableStateFlow(AppPickerUiState())
  val pickerState: StateFlow<AppPickerUiState> = pickerInternal.asStateFlow()
  private var pickerJob: kotlinx.coroutines.Job? = null

  private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

  fun onSearchQueryChanged(query: String) {
    searchQuery.value = query
  }

  fun lockVault() {
    searchQuery.value = ""
    pickerJob?.cancel()
    pickerInternal.value = AppPickerUiState()
    repository.lockVault()
  }

  fun onAppBackgrounded() {
    if (uiState.value.isUnlocked && uiState.value.settings.autoLockOnBackground) {
      lockVault()
    }
  }

  fun loadAppsForPicker() {
    pickerJob?.cancel()
    pickerInternal.value = pickerInternal.value.copy(loadState = PickerLoadState.Loading)
    pickerJob =
      viewModelScope.launch {
        try {
          val apps = repository.loadInstalledApps()
          repository.hiddenPackageHashesFlow.collect { hashes ->
            pickerInternal.value =
              pickerInternal.value.copy(
                loadState = PickerLoadState.Ready(apps = apps, hiddenHashes = hashes)
              )
          }
        } catch (e: Exception) {
          pickerInternal.value =
            pickerInternal.value.copy(
              loadState = PickerLoadState.Error(e.message ?: "Unable to query installed apps")
            )
        }
      }
  }

  fun onPickerSearchChanged(query: String) {
    pickerInternal.value = pickerInternal.value.copy(searchQuery = query)
  }

  fun onPickerFilterChanged(mode: AppFilterMode) {
    pickerInternal.value = pickerInternal.value.copy(filterMode = mode)
  }

  fun togglePackageSelection(packageName: String) {
    val current = pickerInternal.value.selectedPackages
    val next = if (packageName in current) current - packageName else current + packageName
    pickerInternal.value = pickerInternal.value.copy(selectedPackages = next)
  }

  fun isPackageHidden(packageName: String, hiddenHashes: Set<String>): Boolean =
    repository.packageHash(packageName) in hiddenHashes

  fun encryptAndHideSelected(onComplete: () -> Unit) {
    val ready = pickerInternal.value.loadState as? PickerLoadState.Ready ?: return
    val selected = pickerInternal.value.selectedPackages
    if (selected.isEmpty()) return

    val appsToHide = ready.apps.filter { it.packageName in selected }
    pickerInternal.value = pickerInternal.value.copy(isEncrypting = true)

    viewModelScope.launch {
      repository.hideApps(appsToHide)
      pickerInternal.value =
        pickerInternal.value.copy(selectedPackages = emptySet(), isEncrypting = false)
      _toastMessages.tryEmit(
        if (appsToHide.size == 1) {
          "${appsToHide.first().label} encrypted & hidden"
        } else {
          "${appsToHide.size} apps encrypted & hidden"
        }
      )
      onComplete()
    }
  }

  fun launchHiddenApp(item: HiddenAppItem) {
    viewModelScope.launch {
      val launched = repository.launchHiddenApp(item.packageName)
      if (!launched) {
        _toastMessages.tryEmit("Unable to open ${item.displayTitle} on this device")
      }
    }
  }

  fun openSystemAppInfo(packageName: String) {
    val opened = repository.openSystemAppInfo(packageName)
    if (!opened) {
      _toastMessages.tryEmit("Unable to open system App Info settings")
    }
  }

  fun updateCloakAndNote(packageName: String, cloakAlias: String, note: String) {
    viewModelScope.launch {
      repository.updateCloakAndNote(packageName, cloakAlias, note)
      _toastMessages.tryEmit("Encrypted app metadata updated")
    }
  }

  fun unhideApp(item: HiddenAppItem) {
    viewModelScope.launch {
      repository.unhideApp(item.packageName)
      _toastMessages.tryEmit("${item.originalLabel} removed from Vault")
    }
  }

  fun completeInitialPasswordSetup(
    rawPassword: String,
    confirmPassword: String,
    onSuccess: () -> Unit,
  ): String? {
    val validationError = validateSecretEquation(rawPassword)
    if (validationError != null) return validationError

    val normalized = CalculatorEngine.normalizeEquation(rawPassword)
    val normalizedConfirm = CalculatorEngine.normalizeEquation(confirmPassword)
    if (normalized != normalizedConfirm) {
      return "Passwords do not match. Re-enter to confirm."
    }

    viewModelScope.launch {
      repository.updateSecretEquation(normalized)
      onSuccess()
    }
    return null
  }

  fun updateSecretEquation(rawEquation: String): String? {
    val validationError = validateSecretEquation(rawEquation)
    if (validationError != null) return validationError

    val normalized = CalculatorEngine.normalizeEquation(rawEquation)
    viewModelScope.launch {
      repository.updateSecretEquation(normalized)
      _toastMessages.tryEmit("Secret unlock password updated")
    }
    return null
  }

  private fun validateSecretEquation(rawEquation: String): String? {
    val normalized = CalculatorEngine.normalizeEquation(rawEquation)
    if (normalized.length < 3) {
      return "Enter at least 3 characters (e.g. 2580 or 42×2)."
    }
    val allowedChars = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '-', '*', '/', '%', '.')
    if (normalized.any { it !in allowedChars }) {
      return "Use only calculator keys: digits 0-9 and + - × ÷ % ."
    }
    if (normalized.none { it.isDigit() }) {
      return "Password must include at least one digit."
    }
    return null
  }

  fun setStealthMode(enabled: Boolean) {
    viewModelScope.launch { repository.setStealthMode(enabled) }
  }

  fun setAutoLockOnBackground(enabled: Boolean) {
    viewModelScope.launch { repository.setAutoLockOnBackground(enabled) }
  }

  fun wipeVault(onWiped: () -> Unit) {
    viewModelScope.launch {
      repository.wipeVault()
      _toastMessages.tryEmit("Vault wiped and reset to defaults")
      onWiped()
    }
  }

  class Factory(private val repository: VaultRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return VaultViewModel(repository) as T
    }
  }
}
