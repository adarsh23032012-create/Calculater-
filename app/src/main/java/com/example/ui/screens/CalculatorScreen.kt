package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CalcHistoryItem
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.CalculatorUiState

private enum class KeyStyle {
  DIGIT,
  FUNCTION,
  OPERATOR,
  EQUALS,
}

private data class CalcKeySpec(
  val label: String,
  val tag: String,
  val style: KeyStyle,
  val isBackspace: Boolean = false,
  val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
  uiState: CalculatorUiState,
  onDigit: (Char) -> Unit,
  onOperator: (Char) -> Unit,
  onDecimal: () -> Unit,
  onPercent: () -> Unit,
  onToggleSign: () -> Unit,
  onClear: () -> Unit,
  onBackspace: () -> Unit,
  onEquals: () -> Unit,
  onHistoryItemClick: (CalcHistoryItem) -> Unit,
  onClearHistory: () -> Unit,
) {
  var showHistorySheet by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    BoxWithConstraints(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
    ) {
      val isExpandedLayout = maxWidth >= 720.dp

      if (isExpandedLayout) {
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
          CalculatorMainPane(
            uiState = uiState,
            showHistoryButton = false,
            onOpenHistory = {},
            onDigit = onDigit,
            onOperator = onOperator,
            onDecimal = onDecimal,
            onPercent = onPercent,
            onToggleSign = onToggleSign,
            onClear = onClear,
            onBackspace = onBackspace,
            onEquals = onEquals,
            modifier = Modifier.weight(1.25f).fillMaxHeight(),
          )

          Surface(
            modifier = Modifier.weight(0.85f).fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
          ) {
            HistoryPaneContent(
              history = uiState.history,
              onSelect = onHistoryItemClick,
              onClear = onClearHistory,
            )
          }
        }
      } else {
        CalculatorMainPane(
          uiState = uiState,
          showHistoryButton = true,
          onOpenHistory = { showHistorySheet = true },
          onDigit = onDigit,
          onOperator = onOperator,
          onDecimal = onDecimal,
          onPercent = onPercent,
          onToggleSign = onToggleSign,
          onClear = onClear,
          onBackspace = onBackspace,
          onEquals = onEquals,
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = 520.dp)
              .align(Alignment.Center)
              .padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }
  }

  if (showHistorySheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showHistorySheet = false },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      HistoryPaneContent(
        history = uiState.history,
        onSelect = { item ->
          onHistoryItemClick(item)
          showHistorySheet = false
        },
        onClear = onClearHistory,
        modifier = Modifier.fillMaxWidth().height(380.dp).navigationBarsPadding(),
      )
    }
  }
}

@Composable
private fun CalculatorMainPane(
  uiState: CalculatorUiState,
  showHistoryButton: Boolean,
  onOpenHistory: () -> Unit,
  onDigit: (Char) -> Unit,
  onOperator: (Char) -> Unit,
  onDecimal: () -> Unit,
  onPercent: () -> Unit,
  onToggleSign: () -> Unit,
  onClear: () -> Unit,
  onBackspace: () -> Unit,
  onEquals: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    // Top bar
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
      ) {
        Text(
          text = "STD",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }

      if (showHistoryButton) {
        IconButton(
          onClick = onOpenHistory,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("calc_history_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = stringResource(R.string.calc_history),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Expression & Live Preview Display
    CalculatorDisplay(
      expression = uiState.expression,
      previewResult = uiState.previewResult,
      isError = uiState.isError,
      modifier = Modifier.fillMaxWidth().weight(1f),
    )

    Spacer(Modifier.height(12.dp))

    // Tactile Keypad
    CalculatorKeypad(
      onDigit = onDigit,
      onOperator = onOperator,
      onDecimal = onDecimal,
      onPercent = onPercent,
      onToggleSign = onToggleSign,
      onClear = onClear,
      onBackspace = onBackspace,
      onEquals = onEquals,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun CalculatorDisplay(
  expression: String,
  previewResult: String,
  isError: Boolean,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  val displayText =
    when {
      isError -> stringResource(R.string.calc_error)
      expression.isEmpty() -> "0"
      else -> expression
    }

  LaunchedEffect(displayText) { scrollState.animateScrollTo(scrollState.maxValue) }

  val fontSize =
    when {
      displayText.length > 16 -> 30.sp
      displayText.length > 11 -> 38.sp
      displayText.length > 8 -> 46.sp
      else -> 56.sp
    }

  Surface(
    modifier = modifier.padding(vertical = 4.dp),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.Bottom,
      horizontalAlignment = Alignment.End,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.End,
      ) {
        Text(
          text = displayText,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = fontSize,
          lineHeight = fontSize * 1.15f,
          color =
            if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          textAlign = TextAlign.End,
          modifier = Modifier.testTag("calc_display_expression"),
        )
      }

      Spacer(Modifier.height(8.dp))

      Text(
        text = if (previewResult.isNotEmpty() && !isError) "= $previewResult" else " ",
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        color = MaterialTheme.colorScheme.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("calc_display_preview"),
      )
    }
  }
}

@Composable
private fun CalculatorKeypad(
  onDigit: (Char) -> Unit,
  onOperator: (Char) -> Unit,
  onDecimal: () -> Unit,
  onPercent: () -> Unit,
  onToggleSign: () -> Unit,
  onClear: () -> Unit,
  onBackspace: () -> Unit,
  onEquals: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val rows =
    remember(onDigit, onOperator, onDecimal, onPercent, onToggleSign, onClear, onBackspace, onEquals) {
      listOf(
        listOf(
          CalcKeySpec("AC", "calc_key_clear", KeyStyle.FUNCTION, onClick = onClear),
          CalcKeySpec("±", "calc_key_sign", KeyStyle.FUNCTION, onClick = onToggleSign),
          CalcKeySpec("%", "calc_key_percent", KeyStyle.FUNCTION, onClick = onPercent),
          CalcKeySpec("÷", "calc_key_divide", KeyStyle.OPERATOR) { onOperator('÷') },
        ),
        listOf(
          CalcKeySpec("7", "calc_key_7", KeyStyle.DIGIT) { onDigit('7') },
          CalcKeySpec("8", "calc_key_8", KeyStyle.DIGIT) { onDigit('8') },
          CalcKeySpec("9", "calc_key_9", KeyStyle.DIGIT) { onDigit('9') },
          CalcKeySpec("×", "calc_key_multiply", KeyStyle.OPERATOR) { onOperator('×') },
        ),
        listOf(
          CalcKeySpec("4", "calc_key_4", KeyStyle.DIGIT) { onDigit('4') },
          CalcKeySpec("5", "calc_key_5", KeyStyle.DIGIT) { onDigit('5') },
          CalcKeySpec("6", "calc_key_6", KeyStyle.DIGIT) { onDigit('6') },
          CalcKeySpec("-", "calc_key_minus", KeyStyle.OPERATOR) { onOperator('-') },
        ),
        listOf(
          CalcKeySpec("1", "calc_key_1", KeyStyle.DIGIT) { onDigit('1') },
          CalcKeySpec("2", "calc_key_2", KeyStyle.DIGIT) { onDigit('2') },
          CalcKeySpec("3", "calc_key_3", KeyStyle.DIGIT) { onDigit('3') },
          CalcKeySpec("+", "calc_key_plus", KeyStyle.OPERATOR) { onOperator('+') },
        ),
        listOf(
          CalcKeySpec("⌫", "calc_key_backspace", KeyStyle.FUNCTION, isBackspace = true, onClick = onBackspace),
          CalcKeySpec("0", "calc_key_0", KeyStyle.DIGIT) { onDigit('0') },
          CalcKeySpec(".", "calc_key_dot", KeyStyle.DIGIT, onClick = onDecimal),
          CalcKeySpec("=", "calc_key_equals", KeyStyle.EQUALS, onClick = onEquals),
        ),
      )
    }

  BoxWithConstraints(modifier = modifier) {
    val gap: Dp = if (maxWidth < 360.dp) 8.dp else 12.dp
    val keyHeight: Dp = ((maxWidth - gap * 3) / 4f).coerceIn(52.dp, 76.dp)

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
      rows.forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
          row.forEach { key ->
            CalcButton(
              spec = key,
              modifier = Modifier.weight(1f).height(keyHeight),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CalcButton(
  spec: CalcKeySpec,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val containerColor =
    when (spec.style) {
      KeyStyle.DIGIT -> MaterialTheme.colorScheme.surfaceContainerHigh
      KeyStyle.FUNCTION -> MaterialTheme.colorScheme.surfaceContainerHighest
      KeyStyle.OPERATOR -> MaterialTheme.colorScheme.primaryContainer
      KeyStyle.EQUALS -> MaterialTheme.colorScheme.primary
    }
  val contentColor =
    when (spec.style) {
      KeyStyle.DIGIT -> MaterialTheme.colorScheme.onSurface
      KeyStyle.FUNCTION ->
        if (spec.label == "AC") MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurface
      KeyStyle.OPERATOR -> MaterialTheme.colorScheme.primary
      KeyStyle.EQUALS -> MaterialTheme.colorScheme.onPrimary
    }

  val shape = RoundedCornerShape(22.dp)

  Surface(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(shape)
        .clickable {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          spec.onClick()
        }
        .testTag(spec.tag),
    shape = shape,
    color = containerColor,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize().let { base ->
          if (spec.style == KeyStyle.EQUALS) {
            base.background(
              Brush.linearGradient(
                colors =
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                  )
              )
            )
          } else {
            base
          }
        },
      contentAlignment = Alignment.Center,
    ) {
      if (spec.isBackspace) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.Backspace,
          contentDescription = stringResource(R.string.calc_backspace),
          tint = contentColor,
          modifier = Modifier.size(24.dp),
        )
      } else {
        Text(
          text = spec.label,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = if (spec.style == KeyStyle.EQUALS || spec.style == KeyStyle.OPERATOR) FontWeight.Bold else FontWeight.Medium,
          fontSize = if (spec.label == "AC") 20.sp else 26.sp,
          color = contentColor,
        )
      }
    }
  }
}

@Composable
private fun HistoryPaneContent(
  history: List<CalcHistoryItem>,
  onSelect: (CalcHistoryItem) -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = stringResource(R.string.calc_history),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (history.isNotEmpty()) {
        TextButton(
          onClick = onClear,
          modifier = Modifier.testTag("clear_history_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteSweep,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.calc_clear_history))
        }
      }
    }

    HorizontalDivider(
      modifier = Modifier.padding(vertical = 8.dp),
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    )

    if (history.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(R.string.calc_no_history),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(history, key = { it.id }) { item ->
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelect(item) }
                .testTag("history_item_${item.id}"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalAlignment = Alignment.End,
            ) {
              Text(
                text = item.expression,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(Modifier.height(2.dp))
              Text(
                text = "= ${item.result}",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }
  }
}
