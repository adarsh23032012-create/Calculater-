package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun SetupPasswordScreen(
  onCompleteSetup: (password: String, confirm: String) -> String?,
) {
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var activeField by remember { mutableIntStateOf(0) } // 0 = password, 1 = confirm
  var revealCharacters by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  fun appendChar(ch: Char) {
    errorMessage = null
    if (activeField == 0) {
      if (password.length < 18) password += ch
    } else {
      if (confirmPassword.length < 18) confirmPassword += ch
    }
  }

  fun backspace() {
    errorMessage = null
    if (activeField == 0) {
      if (password.isNotEmpty()) password = password.dropLast(1)
    } else {
      if (confirmPassword.isNotEmpty()) confirmPassword = confirmPassword.dropLast(1)
    }
  }

  fun clearActive() {
    errorMessage = null
    if (activeField == 0) password = "" else confirmPassword = ""
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = 520.dp)
            .align(Alignment.Center)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          // Header Banner
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
          ) {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .background(
                    Brush.linearGradient(
                      colors =
                        listOf(
                          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                          MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        )
                    )
                  )
                  .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(10.dp).size(24.dp),
                )
              }
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.setup_title),
                  style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                  text = stringResource(R.string.setup_subtitle),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // Passcode Fields
          SetupPasscodeField(
            label = stringResource(R.string.setup_enter_label),
            value = password,
            isActive = activeField == 0,
            revealed = revealCharacters,
            onClick = { activeField = 0 },
            onToggleReveal = { revealCharacters = !revealCharacters },
            testTag = "setup_password_field",
          )

          SetupPasscodeField(
            label = stringResource(R.string.setup_confirm_label),
            value = confirmPassword,
            isActive = activeField == 1,
            revealed = revealCharacters,
            onClick = { activeField = 1 },
            onToggleReveal = null,
            testTag = "setup_confirm_field",
          )

          AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            errorMessage?.let { err ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = err,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
              }
            }
          }
        }

        Spacer(Modifier.height(12.dp))

        // Embedded Calculator-Style Keypad + Transform Button
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SetupKeypad(
            onChar = ::appendChar,
            onBackspace = ::backspace,
            onClear = ::clearActive,
            onNextOrDone = {
              if (activeField == 0) {
                activeField = 1
              } else {
                errorMessage = onCompleteSetup(password, confirmPassword)
              }
            },
            isConfirmStep = activeField == 1,
          )

          Button(
            onClick = {
              if (activeField == 0 && confirmPassword.isEmpty() && password.length >= 3) {
                activeField = 1
              } else {
                errorMessage = onCompleteSetup(password, confirmPassword)
              }
            },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            shape = RoundedCornerShape(18.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(54.dp)
                .minimumInteractiveComponentSize()
                .testTag("setup_confirm_button"),
          ) {
            Icon(Icons.Rounded.Calculate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
              text =
                if (activeField == 0 && confirmPassword.isEmpty()) "Next: Confirm Password"
                else stringResource(R.string.setup_cta),
              style = MaterialTheme.typography.titleMedium,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SetupPasscodeField(
  label: String,
  value: String,
  isActive: Boolean,
  revealed: Boolean,
  onClick: () -> Unit,
  onToggleReveal: (() -> Unit)?,
  testTag: String,
) {
  val shape = RoundedCornerShape(18.dp)
  val borderColor =
    if (isActive) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .clip(shape)
        .border(width = if (isActive) 2.dp else 1.dp, color = borderColor, shape = shape)
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = shape,
    color = MaterialTheme.colorScheme.surface,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color =
            if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        val displayed =
          when {
            value.isEmpty() -> if (isActive) "Tap keys below…" else "Tap to confirm"
            revealed -> value
            else -> "•".repeat(value.length)
          }
        Text(
          text = displayed,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 22.sp,
          fontWeight = FontWeight.SemiBold,
          color =
            if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (onToggleReveal != null) {
        IconButton(
          onClick = onToggleReveal,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("setup_toggle_visibility"),
        ) {
          Icon(
            imageVector =
              if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = if (revealed) "Hide password" else "Show password",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun SetupKeypad(
  onChar: (Char) -> Unit,
  onBackspace: () -> Unit,
  onClear: () -> Unit,
  onNextOrDone: () -> Unit,
  isConfirmStep: Boolean,
) {
  val rows =
    listOf(
      listOf("7", "8", "9", "÷"),
      listOf("4", "5", "6", "×"),
      listOf("1", "2", "3", "-"),
      listOf("AC", "0", "⌫", "+"),
    )

  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val gap: Dp = 8.dp
    val keyHeight: Dp = ((maxWidth - gap * 3) / 4f).coerceIn(48.dp, 62.dp)
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
      rows.forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
          row.forEach { label ->
            val isOperator = label in setOf("÷", "×", "-", "+")
            val isAction = label == "AC" || label == "⌫"
            val bgColor =
              when {
                isOperator -> MaterialTheme.colorScheme.primaryContainer
                isAction -> MaterialTheme.colorScheme.surfaceContainerHighest
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
              }
            val fgColor =
              when {
                label == "AC" -> MaterialTheme.colorScheme.error
                isOperator -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
              }

            val tag =
              when (label) {
                "÷" -> "setup_key_divide"
                "×" -> "setup_key_multiply"
                "-" -> "setup_key_minus"
                "+" -> "setup_key_plus"
                "AC" -> "setup_key_clear"
                "⌫" -> "setup_key_backspace"
                else -> "setup_key_$label"
              }

            Surface(
              modifier =
                Modifier.weight(1f)
                  .height(keyHeight)
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(18.dp))
                  .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    when (label) {
                      "AC" -> onClear()
                      "⌫" -> onBackspace()
                      else -> onChar(label.first())
                    }
                  }
                  .testTag(tag),
              shape = RoundedCornerShape(18.dp),
              color = bgColor,
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (label == "⌫") {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = stringResource(R.string.calc_backspace),
                    tint = fgColor,
                    modifier = Modifier.size(22.dp),
                  )
                } else {
                  Text(
                    text = label,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (label == "AC") 17.sp else 22.sp,
                    fontWeight = if (isOperator) FontWeight.Bold else FontWeight.Medium,
                    color = fgColor,
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
