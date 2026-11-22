package com.example.model

import androidx.compose.ui.graphics.ImageBitmap

data class InstalledAppInfo(
  val packageName: String,
  val label: String,
  val versionName: String,
  val isSystemApp: Boolean,
  val icon: ImageBitmap?,
)

data class HiddenAppItem(
  val packageName: String,
  val originalLabel: String,
  val cloakAlias: String,
  val securityNote: String,
  val hiddenAtEpochMs: Long,
  val launchCount: Int,
  val lastLaunchedEpochMs: Long?,
  val encryptedCipherPreview: String,
  val ivPreview: String,
  val isInstalled: Boolean,
  val icon: ImageBitmap?,
) {
  val displayTitle: String
    get() = cloakAlias.ifBlank { originalLabel }
}

data class CalcHistoryItem(
  val id: Long,
  val expression: String,
  val result: String,
  val timestampEpochMs: Long,
)
