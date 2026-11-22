package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.HiddenAppItem
import com.example.security.CryptoDiagnostics
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.VaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
  uiState: VaultUiState,
  onSearchChanged: (String) -> Unit,
  onOpenAppPicker: () -> Unit,
  onOpenSettings: () -> Unit,
  onLockAndExit: () -> Unit,
  onLaunchApp: (HiddenAppItem) -> Unit,
  onOpenSystemAppInfo: (String) -> Unit,
  onUpdateCloakAndNote: (packageName: String, alias: String, note: String) -> Unit,
  onUnhideApp: (HiddenAppItem) -> Unit,
) {
  BackHandler { onLockAndExit() }

  var inspectorExpanded by remember { mutableStateOf(false) }
  var editingApp by remember { mutableStateOf<HiddenAppItem?>(null) }

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
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
              text = stringResource(R.string.vault_title),
              style = MaterialTheme.typography.titleLarge,
            )
          }
        },
        actions = {
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("vault_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = stringResource(R.string.vault_settings),
            )
          }
          IconButton(
            onClick = onLockAndExit,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("vault_lock_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = stringResource(R.string.vault_lock_now),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        },
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onOpenAppPicker,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.vault_hide_apps_cta)) },
        modifier = Modifier.testTag("vault_hide_apps_fab"),
      )
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      val gridColumns = if (maxWidth >= 660.dp) 2 else 1

      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 960.dp)
            .align(Alignment.TopCenter)
            .padding(horizontal = 16.dp),
      ) {
        // Security Status Hero Card
        SecurityHeroCard(
          hiddenCount = uiState.hiddenApps.size,
          diagnostics = uiState.diagnostics,
          expanded = inspectorExpanded,
          onToggleExpanded = { inspectorExpanded = !inspectorExpanded },
        )

        Spacer(Modifier.height(12.dp))

        if (uiState.hiddenApps.isNotEmpty()) {
          OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
              Icon(Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
              if (uiState.searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChanged("") }) {
                  Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
              }
            },
            placeholder = { Text(stringResource(R.string.vault_search_placeholder)) },
            modifier = Modifier.fillMaxWidth().testTag("vault_search_input"),
          )
          Spacer(Modifier.height(12.dp))
        }

        if (uiState.hiddenApps.isEmpty()) {
          VaultEmptyState(
            onHideAppsClick = onOpenAppPicker,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          val list = uiState.filteredApps
          if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                text = "No hidden apps match \"${uiState.searchQuery}\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LazyVerticalGrid(
              columns = GridCells.Fixed(gridColumns),
              contentPadding = PaddingValues(bottom = 96.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(list, key = { it.packageName }) { item ->
                HiddenAppCard(
                  item = item,
                  onLaunch = { onLaunchApp(item) },
                  onEditCloak = { editingApp = item },
                  onUnhide = { onUnhideApp(item) },
                )
              }
            }
          }
        }
      }
    }
  }

  editingApp?.let { target ->
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var aliasInput by remember(target.packageName) { mutableStateOf(target.cloakAlias) }
    var noteInput by remember(target.packageName) { mutableStateOf(target.securityNote) }

    ModalBottomSheet(
      onDismissRequest = { editingApp = null },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Cloak & Encrypted Metadata",
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = "Package: ${target.packageName}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = aliasInput,
          onValueChange = { aliasInput = it },
          label = { Text("Vault Display Alias (Optional)") },
          placeholder = { Text(target.originalLabel) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("cloak_alias_input"),
        )

        OutlinedTextField(
          value = noteInput,
          onValueChange = { noteInput = it },
          label = { Text("AES-256 Encrypted Note") },
          placeholder = { Text("Private PIN, backup note, or reminder…") },
          minLines = 2,
          maxLines = 4,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("cloak_note_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              onOpenSystemAppInfo(target.packageName)
              editingApp = null
            },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.vault_system_app_info))
          }

          Button(
            onClick = {
              onUpdateCloakAndNote(target.packageName, aliasInput, noteInput)
              editingApp = null
            },
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("cloak_save_button"),
          ) {
            Text("Encrypt & Save")
          }
        }
      }
    }
  }
}

@Composable
private fun SecurityHeroCard(
  hiddenCount: Int,
  diagnostics: CryptoDiagnostics?,
  expanded: Boolean,
  onToggleExpanded: () -> Unit,
) {
  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .clickable(onClick = onToggleExpanded)
        .testTag("vault_security_hero_card"),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
  ) {
    Column(
      modifier =
        Modifier.background(
            Brush.linearGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                  MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                )
            )
          )
          .padding(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
        ) {
          Icon(
            imageVector = Icons.Rounded.EnhancedEncryption,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(10.dp).size(24.dp),
          )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              if (hiddenCount == 1) "1 App Encrypted" else "$hiddenCount Apps Encrypted",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Protected by AndroidKeyStore AES-256-GCM",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
          contentDescription = "Toggle cipher diagnostics",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      AnimatedVisibility(
        visible = expanded && diagnostics != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
      ) {
        diagnostics?.let { diag ->
          Column(modifier = Modifier.padding(top = 12.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(Modifier.height(10.dp))
            DiagnosticRow("Keystore Alias", diag.keystoreAlias)
            DiagnosticRow("Cipher Suite", diag.cipherSuite)
            DiagnosticRow("Equation KDF", diag.keyDerivation)
            DiagnosticRow("Provider", diag.providerName)
          }
        }
      }
    }
  }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      fontFamily = JetBrainsMonoFontFamily,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.secondary,
    )
  }
}

@Composable
private fun HiddenAppCard(
  item: HiddenAppItem,
  onLaunch: () -> Unit,
  onEditCloak: () -> Unit,
  onUnhide: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("hidden_app_card_${item.packageName}"),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AppIconTile(item = item)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.displayTitle,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (item.cloakAlias.isNotBlank() && item.cloakAlias != item.originalLabel) {
            Text(
              text = "Cloaked · ${item.originalLabel}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.tertiary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          } else {
            Text(
              text = item.packageName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Key,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = "GCM",
              fontFamily = JetBrainsMonoFontFamily,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
          Text(
            text = "IV: ${item.ivPreview}  ·  Opens: ${item.launchCount}",
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (item.securityNote.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
              text = item.securityNote,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Spacer(Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilledTonalButton(
          onClick = onLaunch,
          enabled = item.isInstalled,
          colors =
            ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("launch_app_${item.packageName}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(if (item.isInstalled) stringResource(R.string.vault_launch_app) else "Uninstalled")
        }

        IconButton(
          onClick = onEditCloak,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("edit_cloak_${item.packageName}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.vault_edit_cloak),
          )
        }

        IconButton(
          onClick = onUnhide,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("unhide_app_${item.packageName}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Visibility,
            contentDescription = stringResource(R.string.vault_unhide_app),
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun AppIconTile(item: HiddenAppItem) {
  if (item.icon != null) {
    Image(
      bitmap = item.icon,
      contentDescription = "${item.displayTitle} icon",
      modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)),
      contentScale = ContentScale.Crop,
    )
  } else {
    Surface(
      modifier = Modifier.size(48.dp),
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = item.displayTitle.take(1).uppercase(),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun VaultEmptyState(
  onHideAppsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(horizontal = 16.dp, vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_vault_empty),
      contentDescription = "Encrypted app vault shield",
      contentScale = ContentScale.Crop,
      modifier =
        Modifier.widthIn(max = 280.dp)
          .height(190.dp)
          .clip(RoundedCornerShape(24.dp)),
    )
    Spacer(Modifier.height(20.dp))
    Text(
      text = stringResource(R.string.vault_empty_title),
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.vault_empty_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 360.dp),
    )
    Spacer(Modifier.height(20.dp))
    Button(
      onClick = onHideAppsClick,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.secondary,
          contentColor = MaterialTheme.colorScheme.onSecondary,
        ),
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .testTag("vault_empty_hide_button"),
    ) {
      Icon(Icons.Rounded.Add, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text(stringResource(R.string.vault_hide_apps_cta))
    }
  }
}
