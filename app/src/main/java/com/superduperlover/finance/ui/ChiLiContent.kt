package com.superduperlover.finance.ui

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.superduperlover.finance.FinanceApplication
import com.superduperlover.finance.ui.theme.AppPrefs
import com.superduperlover.finance.ui.theme.FinanceTheme
import com.superduperlover.finance.ui.theme.LocalAppPrefs

/** True while the screen draws a green header under the status bar (Home): the status bar then needs light icons. */
val LocalGreenStatusBar = staticCompositionLocalOf { mutableStateOf(false) }

/** Edge-to-edge content with the app theme, Repo and prefs provided. Shared by every Compose activity. */
fun ComponentActivity.setChiLiContent(content: @Composable () -> Unit) {
    val prefs = AppPrefs(applicationContext)
    setContent {
        val dark = prefs.dark
        val green = remember { mutableStateOf(false) }
        DisposableEffect(dark, green.value) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark || green.value },
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            )
            onDispose {}
        }
        val repo = (application as FinanceApplication).container.repo
        CompositionLocalProvider(
            LocalRepo provides repo,
            LocalAppPrefs provides prefs,
            LocalGreenStatusBar provides green,
        ) {
            FinanceTheme {
                content()
            }
        }
    }
}
