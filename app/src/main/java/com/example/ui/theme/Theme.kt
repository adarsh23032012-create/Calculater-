package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = AmberOperator,
    onPrimary = AmberOnOperator,
    primaryContainer = AmberOperatorContainer,
    onPrimaryContainer = AmberOperator,
    secondary = EmeraldCipher,
    onSecondary = EmeraldOnCipher,
    secondaryContainer = EmeraldCipherContainer,
    onSecondaryContainer = EmeraldCipher,
    tertiary = CyanTelemetry,
    onTertiary = ObsidianBg,
    tertiaryContainer = CyanTelemetryContainer,
    onTertiaryContainer = CyanTelemetry,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianKeySurface,
    surfaceContainerHighest = ObsidianKeyMuted,
    outline = ObsidianOutline,
    error = CoralAlertDark,
    onError = ObsidianBg,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AmberOperatorLight,
    onPrimary = Color.White,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = Color(0xFF451A03),
    secondary = EmeraldCipherLight,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = CyanTelemetryLight,
    onTertiary = Color.White,
    tertiaryContainer = CyanContainerLight,
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = SlateBg,
    onBackground = TextPrimaryLight,
    surface = SlateSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = SlateSurfaceElevated,
    surfaceContainerHigh = SlateKeySurface,
    surfaceContainerHighest = Color(0xFFD6DFED),
    outline = SlateOutline,
    error = CoralAlertLight,
    onError = Color.White,
  )

val AppShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )

@Composable
fun CalcVaultTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
