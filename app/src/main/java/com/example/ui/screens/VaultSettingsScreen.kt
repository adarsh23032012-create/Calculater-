package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.VaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsScreen(
  uiState: VaultUiState,
  onUpdateSecretEquation: (String) -> String?,
  onStealthModeChanged: (Boolean) -> Unit,
  onAutoLockChanged: (Boolean) -> Unit,
  onWipeVault: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler { onBack() }

  var equationInput by remember { mutableStateOf("") }
  var equationError by remember { mutableStateOf<String?>(null) }
  var showWipeDialog by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
          ),
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.back),
            )
          }
        },
        title = {
          Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
          )
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Secret Equation Card
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.settings_change_equation),
                style = MaterialTheme.typography.titleMedium,
              )
            }

            Text(
              text =
                "Your secret unlock password is PBKDF2-SHA256 hashed. Enter a new PIN or math equation below to replace it.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
              value = equationInput,
              onValueChange = {
                equationInput = it
                equationError = null
              },
              label = { Text("New Secret Equation") },
              placeholder = { Text("e.g. 42*2 or 2580+1") },
              singleLine = true,
              isError = equationError != null,
              supportingText = equationError?.let { err -> { Text(err) } },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("settings_equation_input"),
            )

            Button(
              onClick = {
                val err = onUpdateSecretEquation(equationInput)
                if (err != null) {
                  equationError = err
                } else {
                  equationInput = ""
                  equationError = null
                }
              },
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("settings_save_equation_button"),
            ) {
              Text("Save Secret Equation")
            }
          }
        }

        // Protection Toggles Card
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.VisibilityOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
              )
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.settings_stealth_mode),
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "Hide all vault tips on the Calculator screen",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = uiState.settings.stealthMode,
                onCheckedChange = onStealthModeChanged,
                modifier = Modifier.testTag("settings_stealth_switch"),
              )
            }

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 14.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.settings_auto_lock),
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "Lock vault & revert to Calculator when leaving app",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = uiState.settings.autoLockOnBackground,
                onCheckedChange = onAutoLockChanged,
                modifier = Modifier.testTag("settings_autolock_switch"),
              )
            }
          }
        }

        // Keystore Diagnostics Card
        uiState.diagnostics?.let { diag ->
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "Encryption Engine",
                style = MaterialTheme.typography.titleMedium,
              )
              Spacer(Modifier.height(2.dp))
              KeySpecRow("Keystore Alias", diag.keystoreAlias)
              KeySpecRow("Cipher", diag.cipherSuite)
              KeySpecRow("KDF", diag.keyDerivation)
              KeySpecRow("Provider", diag.providerName)
            }
          }
        }

        // Danger Zone
        OutlinedButton(
          onClick = { showWipeDialog = true },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("settings_wipe_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteForever,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.settings_wipe_vault))
        }

        Spacer(Modifier.height(16.dp))
      }
    }
  }

  if (showWipeDialog) {
    AlertDialog(
      onDismissRequest = { showWipeDialog = false },
      title = { Text("Wipe Encrypted Vault?") },
      text = {
        Text(
          "This permanently deletes all encrypted app entries, cloak aliases, and clears your secret password so you can set a new one. Installed apps on your device are not uninstalled."
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            showWipeDialog = false
            onWipeVault()
          },
          modifier = Modifier.testTag("confirm_wipe_button"),
        ) {
          Text("Wipe Vault", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showWipeDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun KeySpecRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      fontFamily = JetBrainsMonoFontFamily,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.secondary,
    )
  }
}
