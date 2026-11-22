package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.ui.screens.AppPickerScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.SetupPasswordScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.VaultSettingsScreen
import com.example.ui.theme.CalcVaultTheme
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.VaultBootstrapState
import com.example.ui.viewmodel.VaultViewModel

object Routes {
  const val SETUP = "setup"
  const val CALCULATOR = "calculator"
  const val VAULT = "vault"
  const val APP_PICKER = "app_picker"
  const val VAULT_SETTINGS = "vault_settings"
}

class MainActivity : ComponentActivity() {
  private val appContainer: AppContainer by lazy { AppContainer(applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CalcVaultTheme {
        CalcVaultApp(appContainer = appContainer)
      }
    }
  }
}

@Composable
fun CalcVaultApp(appContainer: AppContainer) {
  val calculatorViewModel: CalculatorViewModel =
    viewModel(factory = CalculatorViewModel.Factory(appContainer.vaultRepository))
  val vaultViewModel: VaultViewModel =
    viewModel(factory = VaultViewModel.Factory(appContainer.vaultRepository))

  val bootstrapState by vaultViewModel.bootstrapState.collectAsStateWithLifecycle()

  var initialRoute by androidx.compose.runtime.mutableStateOf<String?>(null)
  LaunchedEffect(bootstrapState) {
    if (initialRoute == null && bootstrapState != VaultBootstrapState.Loading) {
      initialRoute =
        if (bootstrapState == VaultBootstrapState.NeedsPasswordSetup) Routes.SETUP
        else Routes.CALCULATOR
    }
  }

  val resolvedStart = initialRoute
  if (resolvedStart == null) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
  } else {
    CalcVaultNavHost(
      startDestination = resolvedStart,
      calculatorViewModel = calculatorViewModel,
      vaultViewModel = vaultViewModel,
    )
  }
}

@Composable
private fun CalcVaultNavHost(
  startDestination: String,
  calculatorViewModel: CalculatorViewModel,
  vaultViewModel: VaultViewModel,
) {
  val navController = rememberNavController()

  val calcState by calculatorViewModel.uiState.collectAsStateWithLifecycle()
  val vaultState by vaultViewModel.uiState.collectAsStateWithLifecycle()
  val pickerState by vaultViewModel.pickerState.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  val lifecycleOwner = LocalLifecycleOwner.current

  LaunchedEffect(Unit) {
    calculatorViewModel.vaultUnlockedEvents.collect {
      navController.navigate(Routes.VAULT) { launchSingleTop = true }
    }
  }

  LaunchedEffect(Unit) {
    vaultViewModel.toastMessages.collect { message -> snackbarHostState.showSnackbar(message) }
  }

  LaunchedEffect(vaultState.isUnlocked) {
    val currentRoute = navController.currentDestination?.route
    if (!vaultState.isUnlocked && currentRoute != Routes.CALCULATOR && currentRoute != Routes.SETUP) {
      navController.popBackStack(Routes.CALCULATOR, inclusive = false)
    }
  }

  // Auto-lock vault when app goes to background (if enabled)
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) {
        vaultViewModel.onAppBackgrounded()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
      navController = navController,
      startDestination = startDestination,
      enterTransition = {
        fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.97f, animationSpec = tween(260))
      },
      exitTransition = { fadeOut(animationSpec = tween(180)) },
    ) {
      composable(Routes.SETUP) {
        SetupPasswordScreen(
          onCompleteSetup = { password, confirm ->
            vaultViewModel.completeInitialPasswordSetup(password, confirm) {
              navController.navigate(Routes.CALCULATOR) {
                popUpTo(Routes.SETUP) { inclusive = true }
              }
            }
          }
        )
      }

      composable(Routes.CALCULATOR) {
        CalculatorScreen(
          uiState = calcState,
          onDigit = calculatorViewModel::onDigit,
          onOperator = calculatorViewModel::onOperator,
          onDecimal = calculatorViewModel::onDecimal,
          onPercent = calculatorViewModel::onPercent,
          onToggleSign = calculatorViewModel::onToggleSign,
          onClear = calculatorViewModel::onClear,
          onBackspace = calculatorViewModel::onBackspace,
          onEquals = calculatorViewModel::onEquals,
          onHistoryItemClick = calculatorViewModel::onHistoryItemClick,
          onClearHistory = calculatorViewModel::onClearHistory,
        )
      }

      composable(Routes.VAULT) {
        VaultScreen(
          uiState = vaultState,
          onSearchChanged = vaultViewModel::onSearchQueryChanged,
          onOpenAppPicker = { navController.navigate(Routes.APP_PICKER) },
          onOpenSettings = { navController.navigate(Routes.VAULT_SETTINGS) },
          onLockAndExit = {
            vaultViewModel.lockVault()
            navController.popBackStack(Routes.CALCULATOR, inclusive = false)
          },
          onLaunchApp = vaultViewModel::launchHiddenApp,
          onOpenSystemAppInfo = vaultViewModel::openSystemAppInfo,
          onUpdateCloakAndNote = vaultViewModel::updateCloakAndNote,
          onUnhideApp = vaultViewModel::unhideApp,
        )
      }

      composable(Routes.APP_PICKER) {
        AppPickerScreen(
          uiState = pickerState,
          isPackageHidden = vaultViewModel::isPackageHidden,
          onLoadApps = vaultViewModel::loadAppsForPicker,
          onSearchChanged = vaultViewModel::onPickerSearchChanged,
          onFilterChanged = vaultViewModel::onPickerFilterChanged,
          onTogglePackage = vaultViewModel::togglePackageSelection,
          onConfirmHide = {
            vaultViewModel.encryptAndHideSelected { navController.popBackStack() }
          },
          onBack = { navController.popBackStack() },
        )
      }

      composable(Routes.VAULT_SETTINGS) {
        VaultSettingsScreen(
          uiState = vaultState,
          onUpdateSecretEquation = vaultViewModel::updateSecretEquation,
          onStealthModeChanged = vaultViewModel::setStealthMode,
          onAutoLockChanged = vaultViewModel::setAutoLockOnBackground,
          onWipeVault = {
            vaultViewModel.wipeVault {
              navController.navigate(Routes.SETUP) {
                popUpTo(0) { inclusive = true }
              }
            }
          },
          onBack = { navController.popBackStack() },
        )
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
    )
  }
}
