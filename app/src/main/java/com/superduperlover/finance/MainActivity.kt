package com.superduperlover.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.superduperlover.finance.ui.FinanceApp
import com.superduperlover.finance.ui.theme.FinanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceTheme {
                FinanceApp()
            }
        }
    }
}
