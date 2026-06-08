package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalculatorRepository
import com.example.calculator.CalculatorEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CalculatorViewModel(private val repository: CalculatorRepository) : ViewModel() {

    private val _displayState = MutableStateFlow("")
    val displayState: StateFlow<String> = _displayState.asStateFlow()

    private val _previewState = MutableStateFlow("")
    val previewState: StateFlow<String> = _previewState.asStateFlow()

    private val _useRadians = MutableStateFlow(false)
    val useRadians: StateFlow<Boolean> = _useRadians.asStateFlow()

    private val _memoryValue = MutableStateFlow(0.0)
    val memoryValue: StateFlow<Double> = _memoryValue.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null means follow system theme
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    val historyList = repository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // Flag to track if the current display is a completed result
    private var isResultDisplayed = false

    fun onInput(char: String) {
        val current = _displayState.value

        // Helper check for operators
        val operators = setOf("+", "−", "×", "÷", "^")
        val isNewOperator = operators.contains(char)

        if (isResultDisplayed) {
            if (isNewOperator) {
                // If an operator is selected right after a result, perform operations on that result
                isResultDisplayed = false
            } else {
                // If a new number is pressed, start a new expression
                _displayState.value = ""
                isResultDisplayed = false
            }
        }

        val updated = _displayState.value
        val lastChar = if (updated.isNotEmpty()) updated.last().toString() else ""

        if (isNewOperator && operators.contains(lastChar)) {
            // Replace the last operator with the new one
            _displayState.value = updated.dropLast(1) + char
        } else if (isNewOperator && updated.isEmpty()) {
            // Avoid starting an expression with standard operator except minus
            if (char == "−") {
                _displayState.value = char
            }
        } else {
            _displayState.value += char
        }

        calculatePreview()
    }

    fun onClear() {
        _displayState.value = ""
        _previewState.value = ""
        isResultDisplayed = false
    }

    fun onDelete() {
        val current = _displayState.value
        if (current.isEmpty()) return

        if (isResultDisplayed) {
            _displayState.value = ""
            _previewState.value = ""
            isResultDisplayed = false
            return
        }

        val functions = listOf("sin(", "cos(", "tan(", "log(", "ln(", "√(", "asin(", "acos(", "atan(")
        var deleted = false
        for (func in functions) {
            if (current.endsWith(func)) {
                _displayState.value = current.substring(0, current.length - func.length)
                deleted = true
                break
            }
        }
        if (!deleted) {
            _displayState.value = current.dropLast(1)
        }
        calculatePreview()
    }

    fun toggleAngleMode() {
        _useRadians.value = !_useRadians.value
        calculatePreview()
        viewModelScope.launch {
            _toastMessage.emit(if (_useRadians.value) "Switched to Radians" else "Switched to Degrees")
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = when (_isDarkMode.value) {
            null -> true
            true -> false
            false -> null // cycle back to follow system
        }
    }

    fun onMemoryAction(action: String) {
        val currentInput = _displayState.value
        viewModelScope.launch {
            try {
                when (action) {
                    "MC" -> {
                        _memoryValue.value = 0.0
                        _toastMessage.emit("Memory Cleared")
                    }
                    "MR" -> {
                        val formattedMem = CalculatorEngine.formatResult(_memoryValue.value)
                        onInput(formattedMem)
                        _toastMessage.emit("Memory Recalled: $formattedMem")
                    }
                    "M+" -> {
                        val valToSave = if (currentInput.isNotEmpty()) {
                            CalculatorEngine.evaluate(currentInput, _useRadians.value)
                        } else {
                            0.0
                        }
                        _memoryValue.value += valToSave
                        _toastMessage.emit("Added to Memory: +${CalculatorEngine.formatResult(valToSave)}")
                    }
                    "M-" -> {
                        val valToSubtract = if (currentInput.isNotEmpty()) {
                            CalculatorEngine.evaluate(currentInput, _useRadians.value)
                        } else {
                            0.0
                        }
                        _memoryValue.value -= valToSubtract
                        _toastMessage.emit("Subtracted from Memory: -${CalculatorEngine.formatResult(valToSubtract)}")
                    }
                }
            } catch (e: Exception) {
                _toastMessage.emit("Invalid calculation to update memory")
            }
        }
    }

    fun onHistoryItemSelect(expression: String, result: String) {
        _displayState.value = expression
        _previewState.value = result
        isResultDisplayed = false
    }

    fun onDeleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _toastMessage.emit("History Cleared")
        }
    }

    fun onEvaluate() {
        val expression = _displayState.value
        if (expression.isBlank()) return

        try {
            val rawResult = CalculatorEngine.evaluate(expression, _useRadians.value)
            val formattedResult = CalculatorEngine.formatResult(rawResult)
            
            _previewState.value = expression // Show equation as historic preview in the upper line
            _displayState.value = formattedResult
            isResultDisplayed = true

            // Save to Room DB History
            viewModelScope.launch {
                repository.saveCalculation(expression, formattedResult)
            }
        } catch (e: ArithmeticException) {
            _previewState.value = expression
            _displayState.value = e.message ?: "Error"
            isResultDisplayed = true
        } catch (e: Exception) {
            _previewState.value = expression
            _displayState.value = "Error"
            isResultDisplayed = true
        }
    }

    private fun calculatePreview() {
        val expression = _displayState.value
        if (expression.isBlank()) {
            _previewState.value = ""
            return
        }

        try {
            // Evaluates real-time preview (only updates if expr is valid and doesn't throw)
            val rawResult = CalculatorEngine.evaluate(expression, _useRadians.value)
            _previewState.value = CalculatorEngine.formatResult(rawResult)
        } catch (e: Exception) {
            // Fails silently for intermediate draft expressions
            _previewState.value = ""
        }
    }
}

class CalculatorViewModelFactory(private val repository: CalculatorRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
