package com.example

import com.example.calculator.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBasicArithmetic() {
        // Test operator precedence and formats
        val result = CalculatorEngine.evaluate("3 + 4 * 2 - 6 / 3")
        assertEquals(9.0, result, 1e-9)
    }

    @Test
    fun testTrigonometers() {
        // sin(30) in degree mode is 0.5
        val sinResult = CalculatorEngine.evaluate("sin(30)", useRadians = false)
        assertEquals(0.5, sinResult, 1e-9)

        // cos(pi) in radian mode is -1.0
        val cosResult = CalculatorEngine.evaluate("cos(3.141592653589793)", useRadians = true)
        assertEquals(-1.0, cosResult, 1e-9)
    }

    @Test
    fun testImplicitMultiplication() {
        // 2(3+4) should expand to 2*(3+4) -> 14
        val result1 = CalculatorEngine.evaluate("2(3+4)")
        assertEquals(14.0, result1, 1e-9)

        // 3π should expand to 3 * PI
        val result2 = CalculatorEngine.evaluate("3π")
        assertEquals(3 * Math.PI, result2, 1e-9)
    }

    @Test
    fun testScientificFunctions() {
        val logResult = CalculatorEngine.evaluate("log(100)")
        assertEquals(2.0, logResult, 1e-9)

        val lnResult = CalculatorEngine.evaluate("ln(e)")
        assertEquals(1.0, lnResult, 1e-9)

        val sqrtResult = CalculatorEngine.evaluate("√25")
        assertEquals(5.0, sqrtResult, 1e-9)

        val factResult = CalculatorEngine.evaluate("5!")
        assertEquals(120.0, factResult, 1e-9)
    }
}
