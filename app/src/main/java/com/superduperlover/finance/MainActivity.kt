package com.superduperlover.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.superduperlover.finance.ui.FinanceApp
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.theme.FinanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repo = (application as FinanceApplication).container.repo
            CompositionLocalProvider(LocalRepo provides repo) {
                FinanceTheme {
                    FinanceApp()
                }
            }
        }
    }
}
