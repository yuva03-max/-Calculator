package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalculatorDatabase
import com.example.data.CalculatorRepository
import com.example.ui.CalculatorScreen
import com.example.ui.CalculatorViewModel
import com.example.ui.CalculatorViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val database by lazy { CalculatorDatabase.getDatabase(this) }
    private val repository by lazy { CalculatorRepository(database.calculationDao) }
    private val viewModel: CalculatorViewModel by viewModels {
        CalculatorViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkModeByPref by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val useDarkTheme = when (isDarkModeByPref) {
                null -> isSystemInDarkTheme()
                else -> isDarkModeByPref == true
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CalculatorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
