package com.superduperlover.finance.ui

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import com.superduperlover.finance.FinanceApplication
import com.superduperlover.finance.ui.theme.AppPrefs
import com.superduperlover.finance.ui.theme.FinanceTheme
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.ThemeMode

/** Edge-to-edge content with the app theme, Repo and prefs provided. Shared by every Compose activity. */
fun ComponentActivity.setChiLiContent(content: @Composable () -> Unit) {
    val prefs = AppPrefs(applicationContext)
    setContent {
        val dark = when (prefs.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        DisposableEffect(dark) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            )
            onDispose {}
        }
        val repo = (application as FinanceApplication).container.repo
        CompositionLocalProvider(
            LocalRepo provides repo,
            LocalAppPrefs provides prefs,
        ) {
            FinanceTheme {
                content()
            }
        }
    }
}
