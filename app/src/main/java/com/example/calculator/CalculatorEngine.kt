package com.example.calculator

import kotlin.math.*

object CalculatorEngine {

    /**
     * Preprocesses the user expression to handle display operators and implicit multiplication.
     */
    fun preprocessExpression(expr: String): String {
        val formatted = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("–", "-")
            .replace("·", "*")

        val result = StringBuilder()
        for (i in 0 until formatted.length) {
            val current = formatted[i]
            result.append(current)
            if (i < formatted.length - 1) {
                val next = formatted[i + 1]
                if (shouldInsertMultiplication(current, next)) {
                    result.append("*")
                }
            }
        }
        return result.toString()
    }

    private fun shouldInsertMultiplication(curr: Char, next: Char): Boolean {
        val isDigitOrPostfix = curr.isDigit() || curr == ')' || curr == '%' || curr == 'π' || curr == 'e'
        val isNextStart = next.isLetter() || next == '(' || next == '√' || next == 'π' || next == 'e'

        if (isDigitOrPostfix && isNextStart) {
            return true
        }

        if (curr == ')' && next.isDigit()) {
            return true
        }

        return false
    }

    /**
     * Evaluates a mathematical expression string and returns the double result.
     */
    fun evaluate(expression: String, useRadians: Boolean = false): Double {
        if (expression.isBlank()) throw IllegalArgumentException("Empty expression")
        val preprocessed = preprocessExpression(expression)
        return ExpressionParser(preprocessed, useRadians).parse()
    }

    /**
     * Nicely formats a Double result for display on the calculator.
     */
    fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value < 0) "-Infinity" else "Infinity"

        // If it's a whole number, represent as integer
        if (value % 1.0 == 0.0 && value < 1e12 && value > -1e12) {
            return value.toLong().toString()
        }

        // Avoid scientific notation for simple decimals, but use for extremely large/small values
        if (abs(value) > 1e12 || (abs(value) < 1e-6 && value != 0.0)) {
            return String.format("%.6e", value)
        }

        // Round of 10 digits for floating point precision corrections
        val rounded = (value * 1e10).roundToLong() / 1e10
        if (rounded % 1.0 == 0.0) {
            return rounded.toLong().toString()
        }
        
        // Remove trailing zeroes from decimal representation
        val formatted = String.format("%.10f", value).replace(Regex("0+$"), "")
        return if (formatted.endsWith(".")) {
            formatted.substring(0, formatted.length - 1)
        } else {
            formatted
        }
    }

    private class ExpressionParser(private val input: String, val useRadians: Boolean) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) {
                throw IllegalArgumentException("Unexpected character: " + ch.toChar())
            }
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else break
            }
            return x
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) {
                    val d = parseFactor()
                    if (d == 0.0) throw ArithmeticException("Division by Zero")
                    x /= d
                } else break
            }
            return x
        }

        private fun parseFactor(): Double {
            var x = parsePrimary()
            while (true) {
                if (eat('%'.code)) {
                    x /= 100.0
                } else if (eat('!'.code)) {
                    x = factorial(x)
                } else if (eat('^'.code)) {
                    val exponent = parseFactor()
                    x = x.pow(exponent)
                } else {
                    break
                }
            }
            return x
        }

        private fun parsePrimary(): Double {
            if (eat('+'.code)) return parsePrimary()
            if (eat('-'.code)) return -parsePrimary()

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing closing bracket")
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                val numStr = input.substring(startPos, this.pos)
                x = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
            } else if (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code || ch == 'π'.code) {
                val funcName: String
                if (ch == '√'.code) {
                    nextChar()
                    funcName = "sqrt"
                } else if (ch == 'π'.code) {
                    nextChar()
                    funcName = "pi"
                } else {
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    funcName = input.substring(startPos, this.pos)
                }

                if (funcName == "pi") {
                    x = PI
                } else if (funcName == "e") {
                    x = E
                } else {
                    if (!eat('('.code)) {
                        val arg = parsePrimary()
                        x = evaluateFunction(funcName, arg)
                    } else {
                        val arg = parseExpression()
                        if (!eat(')'.code)) throw IllegalArgumentException("Missing closing bracket for $funcName")
                        x = evaluateFunction(funcName, arg)
                    }
                }
            } else {
                throw IllegalArgumentException("Unexpected character: " + ch.toChar())
            }

            return x
        }

        private fun evaluateFunction(name: String, arg: Double): Double {
            return when (name) {
                "sin" -> {
                    val angle = if (useRadians) arg else Math.toRadians(arg)
                    sin(angle)
                }
                "cos" -> {
                    val angle = if (useRadians) arg else Math.toRadians(arg)
                    cos(angle)
                }
                "tan" -> {
                    val angle = if (useRadians) arg else Math.toRadians(arg)
                    // Domain error: tan(90) has infinite error in degrees
                    if (!useRadians && abs(arg % 180) == 90.0) {
                        throw ArithmeticException("Tangent Undefined")
                    }
                    tan(angle)
                }
                "asin" -> {
                    if (arg < -1.0 || arg > 1.0) throw ArithmeticException("Domain Error")
                    val rad = asin(arg)
                    if (useRadians) rad else Math.toDegrees(rad)
                }
                "acos" -> {
                    if (arg < -1.0 || arg > 1.0) throw ArithmeticException("Domain Error")
                    val rad = acos(arg)
                    if (useRadians) rad else Math.toDegrees(rad)
                }
                "atan" -> {
                    val rad = atan(arg)
                    if (useRadians) rad else Math.toDegrees(rad)
                }
                "log" -> {
                    if (arg <= 0.0) throw ArithmeticException("Domain Error")
                    log10(arg)
                }
                "ln" -> {
                    if (arg <= 0.0) throw ArithmeticException("Domain Error")
                    ln(arg)
                }
                "sqrt" -> {
                    if (arg < 0.0) throw ArithmeticException("Root Undefined")
                    sqrt(arg)
                }
                else -> throw IllegalArgumentException("Unknown function: $name")
            }
        }

        private fun factorial(n: Double): Double {
            if (n < 0.0) throw ArithmeticException("Negative Factorial")
            if (n % 1.0 != 0.0) throw ArithmeticException("Factorial is integer-only")
            val intN = n.toInt()
            if (intN > 170) throw ArithmeticException("Overflow")
            var result = 1.0
            for (i in 1..intN) {
                result *= i
            }
            return result
        }
    }
}
