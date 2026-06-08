package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculatorRepository(private val calculationDao: CalculationDao) {
    val allHistory: Flow<List<Calculation>> = calculationDao.getAllHistory()

    suspend fun saveCalculation(expression: String, result: String) {
        val calculation = Calculation(expression = expression, result = result)
        calculationDao.insertCalculation(calculation)
    }

    suspend fun deleteHistoryItem(id: Long) {
        calculationDao.deleteHistoryItem(id)
    }

    suspend fun clearHistory() {
        calculationDao.clearHistory()
    }
}
