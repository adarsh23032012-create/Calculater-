package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.InstalledAppInfo
import com.example.ui.viewmodel.AppFilterMode
import com.example.ui.viewmodel.AppPickerUiState
import com.example.ui.viewmodel.PickerLoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
  uiState: AppPickerUiState,
  isPackageHidden: (String, Set<String>) -> Boolean,
  onLoadApps: () -> Unit,
  onSearchChanged: (String) -> Unit,
  onFilterChanged: (AppFilterMode) -> Unit,
  onTogglePackage: (String) -> Unit,
  onConfirmHide: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler { onBack() }

  LaunchedEffect(Unit) { onLoadApps() }

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
            modifier = Modifier.minimumInteractiveComponentSize().testTag("picker_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.back),
            )
          }
        },
        title = {
          Text(
            text = stringResource(R.string.picker_title),
            style = MaterialTheme.typography.titleLarge,
          )
        },
      )
    },
    bottomBar = {
      if (uiState.selectedPackages.isNotEmpty()) {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
          ) {
            Button(
              onClick = onConfirmHide,
              enabled = !uiState.isEncrypting,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondary,
                  contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .widthIn(max = 520.dp)
                  .minimumInteractiveComponentSize()
                  .testTag("picker_confirm_button"),
            ) {
              Icon(Icons.Rounded.EnhancedEncryption, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(
                text =
                  if (uiState.isEncrypting) "Encrypting…"
                  else stringResource(R.string.picker_encrypt_action, uiState.selectedPackages.size)
              )
            }
          }
        }
      }
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
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
              text =
                "Selected apps are AES-256 encrypted inside this Calculator Vault for private launching. On standard Android, use App Info inside Vault to disable or hide system shortcuts.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
          value = uiState.searchQuery,
          onValueChange = onSearchChanged,
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          trailingIcon = {
            if (uiState.searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchChanged("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
              }
            }
          },
          placeholder = { Text(stringResource(R.string.picker_search_placeholder)) },
          modifier = Modifier.fillMaxWidth().testTag("picker_search_input"),
        )

        Spacer(Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = uiState.filterMode == AppFilterMode.ALL,
            onClick = { onFilterChanged(AppFilterMode.ALL) },
            label = { Text(stringResource(R.string.picker_filter_all)) },
            modifier = Modifier.testTag("picker_filter_all"),
          )
          FilterChip(
            selected = uiState.filterMode == AppFilterMode.USER,
            onClick = { onFilterChanged(AppFilterMode.USER) },
            label = { Text(stringResource(R.string.picker_filter_user)) },
            modifier = Modifier.testTag("picker_filter_user"),
          )
          FilterChip(
            selected = uiState.filterMode == AppFilterMode.SYSTEM,
            onClick = { onFilterChanged(AppFilterMode.SYSTEM) },
            label = { Text(stringResource(R.string.picker_filter_system)) },
            modifier = Modifier.testTag("picker_filter_system"),
          )
        }

        Spacer(Modifier.height(8.dp))

        when (val load = uiState.loadState) {
          PickerLoadState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
          }
          is PickerLoadState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                text = load.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
          is PickerLoadState.Ready -> {
            val filtered =
              load.apps.filter { app ->
                val matchesFilter =
                  when (uiState.filterMode) {
                    AppFilterMode.ALL -> true
                    AppFilterMode.USER -> !app.isSystemApp
                    AppFilterMode.SYSTEM -> app.isSystemApp
                  }
                val q = uiState.searchQuery.trim().lowercase()
                val matchesQuery =
                  q.isEmpty() ||
                    app.label.lowercase().contains(q) ||
                    app.packageName.lowercase().contains(q)
                matchesFilter && matchesQuery
              }

            if (filtered.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                  text = "No matching apps found",
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            } else {
              LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                items(filtered, key = { it.packageName }) { app ->
                  val alreadyHidden = isPackageHidden(app.packageName, load.hiddenHashes)
                  val isSelected = app.packageName in uiState.selectedPackages

                  PickerAppRow(
                    app = app,
                    alreadyHidden = alreadyHidden,
                    isSelected = isSelected,
                    onToggle = {
                      if (!alreadyHidden) onTogglePackage(app.packageName)
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PickerAppRow(
  app: InstalledAppInfo,
  alreadyHidden: Boolean,
  isSelected: Boolean,
  onToggle: () -> Unit,
) {
  val shape = RoundedCornerShape(18.dp)
  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clip(shape)
        .clickable(enabled = !alreadyHidden, onClick = onToggle)
        .testTag("picker_app_${app.packageName}"),
    shape = shape,
    color =
      if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
      else MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (app.icon != null) {
        Image(
          bitmap = app.icon,
          contentDescription = "${app.label} icon",
          modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
          contentScale = ContentScale.Crop,
        )
      } else {
        Surface(
          modifier = Modifier.size(44.dp),
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = app.label.take(1).uppercase(),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = app.label,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            if (alreadyHidden) stringResource(R.string.picker_already_hidden)
            else "${app.packageName} · v${app.versionName}",
          style = MaterialTheme.typography.labelSmall,
          color =
            if (alreadyHidden) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(Modifier.width(8.dp))

      if (alreadyHidden) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = stringResource(R.string.picker_already_hidden),
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(24.dp),
        )
      } else {
        Checkbox(
          checked = isSelected,
          onCheckedChange = { onToggle() },
          modifier = Modifier.testTag("picker_checkbox_${app.packageName}"),
        )
      }
    }
  }
}
