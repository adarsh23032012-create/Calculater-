package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.calculator.CalculatorEngine
import com.example.data.repository.VaultRepository
import com.example.model.CalcHistoryItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalculatorUiState(
  val expression: String = "",
  val previewResult: String = "",
  val isError: Boolean = false,
  val justEvaluated: Boolean = false,
  val history: List<CalcHistoryItem> = emptyList(),
)

class CalculatorViewModel(
  private val repository: VaultRepository,
) : ViewModel() {

  private val internalState = MutableStateFlow(CalculatorUiState())

  val uiState: StateFlow<CalculatorUiState> =
    combine(internalState, repository.calcHistoryFlow) { calc, history ->
        calc.copy(history = history)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalculatorUiState(),
      )

  private val _vaultUnlockedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val vaultUnlockedEvents: SharedFlow<Unit> = _vaultUnlockedEvents.asSharedFlow()

  fun onDigit(digit: Char) {
    val cur = internalState.value
    val base = if (cur.justEvaluated || cur.isError) "" else cur.expression
    val next =
      if (base == "0" && digit == '0') {
        "0"
      } else if (base == "0" && digit != '0') {
        digit.toString()
      } else {
        base + digit
      }
    updateExpression(next, justEvaluated = false)
  }

  fun onOperator(op: Char) {
    val cur = internalState.value
    val base = if (cur.isError) "" else cur.expression
    if (base.isEmpty()) {
      if (op == '-') {
        updateExpression("-", justEvaluated = false)
      }
      return
    }
    val operators = setOf('+', '-', '×', '÷')
    val next =
      if (base.last() in operators) {
        if (base.length == 1 && base[0] == '-' && op != '-') {
          base
        } else {
          base.dropLast(1) + op
        }
      } else {
        base + op
      }
    updateExpression(next, justEvaluated = false)
  }

  fun onDecimal() {
    val cur = internalState.value
    val base = if (cur.justEvaluated || cur.isError) "" else cur.expression
    val lastOpIndex = base.indexOfLast { it in setOf('+', '-', '×', '÷', '%') }
    val currentToken = if (lastOpIndex >= 0) base.substring(lastOpIndex + 1) else base
    if ('.' in currentToken) return

    val next =
      if (currentToken.isEmpty()) {
        base + "0."
      } else {
        base + "."
      }
    updateExpression(next, justEvaluated = false)
  }

  fun onPercent() {
    val cur = internalState.value
    if (cur.isError || cur.expression.isEmpty()) return
    val last = cur.expression.last()
    if (last.isDigit()) {
      updateExpression(cur.expression + "%", justEvaluated = false)
    }
  }

  fun onToggleSign() {
    val cur = internalState.value
    if (cur.isError) {
      updateExpression("-", justEvaluated = false)
      return
    }
    val expr = cur.expression
    if (expr.isEmpty()) {
      updateExpression("-", justEvaluated = false)
      return
    }
    if (expr.startsWith("-") && expr.drop(1).none { it in setOf('+', '-', '×', '÷') }) {
      updateExpression(expr.drop(1), justEvaluated = false)
    } else if (expr.none { it in setOf('+', '-', '×', '÷') }) {
      updateExpression("-$expr", justEvaluated = false)
    }
  }

  fun onClear() {
    internalState.value = CalculatorUiState()
  }

  fun onBackspace() {
    val cur = internalState.value
    if (cur.isError || cur.justEvaluated) {
      internalState.value = CalculatorUiState()
      return
    }
    if (cur.expression.isNotEmpty()) {
      updateExpression(cur.expression.dropLast(1), justEvaluated = false)
    }
  }

  fun onEquals() {
    val expr = internalState.value.expression
    if (expr.isBlank()) return

    viewModelScope.launch {
      val unlocked = repository.tryUnlockWithEquation(expr)
      if (unlocked) {
        internalState.value = CalculatorUiState()
        _vaultUnlockedEvents.tryEmit(Unit)
        return@launch
      }

      when (val result = CalculatorEngine.evaluate(expr)) {
        is CalculatorEngine.EvalResult.Success -> {
          val normalizedDisplay = expr
          if (normalizedDisplay != result.formatted) {
            repository.recordCalculation(normalizedDisplay, result.formatted)
          }
          internalState.value =
            internalState.value.copy(
              expression = result.formatted,
              previewResult = "",
              isError = false,
              justEvaluated = true,
            )
        }
        CalculatorEngine.EvalResult.Error -> {
          internalState.value =
            internalState.value.copy(
              previewResult = "",
              isError = true,
              justEvaluated = false,
            )
        }
      }
    }
  }

  fun onHistoryItemClick(item: CalcHistoryItem) {
    updateExpression(item.result, justEvaluated = false)
  }

  fun onClearHistory() {
    viewModelScope.launch { repository.clearCalcHistory() }
  }

  private fun updateExpression(next: String, justEvaluated: Boolean) {
    val preview = CalculatorEngine.preview(next)
    internalState.value =
      internalState.value.copy(
        expression = next,
        previewResult = preview,
        isError = false,
        justEvaluated = justEvaluated,
      )
  }

  class Factory(private val repository: VaultRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return CalculatorViewModel(repository) as T
    }
  }
}
