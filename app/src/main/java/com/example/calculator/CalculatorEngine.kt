package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object CalculatorEngine {

  private val mathContext = MathContext(15, RoundingMode.HALF_UP)

  sealed class EvalResult {
    data class Success(val formatted: String) : EvalResult()

    data object Error : EvalResult()
  }

  fun normalizeEquation(raw: String): String =
    raw.trim()
      .replace(" ", "")
      .replace("×", "*")
      .replace("÷", "/")
      .replace("−", "-")

  fun evaluate(expression: String): EvalResult {
    val sanitized = normalizeEquation(expression)
    if (sanitized.isEmpty()) return EvalResult.Error
    return try {
      val parser = Parser(sanitized)
      val value = parser.parseExpression()
      if (!parser.isAtEnd()) return EvalResult.Error
      EvalResult.Success(formatResult(value))
    } catch (_: Exception) {
      EvalResult.Error
    }
  }

  fun preview(expression: String): String {
    val sanitized = normalizeEquation(expression)
    if (sanitized.isEmpty()) return ""
    // Only show preview if there is an operator beyond a leading sign
    val hasBinaryOp =
      sanitized.drop(1).any { it == '+' || it == '-' || it == '*' || it == '/' || it == '%' }
    if (!hasBinaryOp) return ""
    return when (val res = evaluate(sanitized)) {
      is EvalResult.Success -> if (res.formatted == sanitized) "" else res.formatted
      EvalResult.Error -> ""
    }
  }

  fun formatResult(value: BigDecimal): String {
    val stripped = value.stripTrailingZeros()
    if (stripped.compareTo(BigDecimal.ZERO) == 0) return "0"
    val plain =
      if (stripped.scale() <= 0 && stripped.precision() <= 12) {
        stripped.toPlainString()
      } else if (stripped.abs() >= BigDecimal("1e12") || stripped.abs() < BigDecimal("1e-7")) {
        stripped.round(MathContext(8, RoundingMode.HALF_UP)).toString()
      } else {
        stripped.setScale(minOf(8, maxOf(0, stripped.scale())), RoundingMode.HALF_UP)
          .stripTrailingZeros()
          .toPlainString()
      }
    return plain
  }

  private class Parser(private val input: String) {
    private var pos = 0

    fun isAtEnd(): Boolean = pos >= input.length

    fun parseExpression(): BigDecimal {
      var x = parseTerm()
      while (pos < input.length) {
        when (input[pos]) {
          '+' -> {
            pos++
            x = x.add(parseTerm(), mathContext)
          }
          '-' -> {
            pos++
            x = x.subtract(parseTerm(), mathContext)
          }
          else -> return x
        }
      }
      return x
    }

    private fun parseTerm(): BigDecimal {
      var x = parseFactor()
      while (pos < input.length) {
        when (input[pos]) {
          '*' -> {
            pos++
            x = x.multiply(parseFactor(), mathContext)
          }
          '/' -> {
            pos++
            val divisor = parseFactor()
            if (divisor.compareTo(BigDecimal.ZERO) == 0) {
              throw ArithmeticException("Division by zero")
            }
            x = x.divide(divisor, mathContext)
          }
          '%' -> {
            pos++
            x = x.divide(BigDecimal(100), mathContext)
          }
          else -> return x
        }
      }
      return x
    }

    private fun parseFactor(): BigDecimal {
      if (pos < input.length && input[pos] == '+') {
        pos++
        return parseFactor()
      }
      if (pos < input.length && input[pos] == '-') {
        pos++
        return parseFactor().negate()
      }

      if (pos < input.length && input[pos] == '(') {
        pos++
        val x = parseExpression()
        if (pos >= input.length || input[pos] != ')') {
          throw IllegalArgumentException("Unclosed parenthesis")
        }
        pos++
        return x
      }

      val start = pos
      while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
        pos++
      }
      if (start == pos) {
        throw IllegalArgumentException("Unexpected character at $pos")
      }
      return BigDecimal(input.substring(start, pos))
    }
  }
}
