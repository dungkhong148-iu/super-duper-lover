package com.superduperlover.finance.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.superduperlover.finance.ui.theme.ChiLiV3

/** "Lịch" page of Home (page 1 of the pager): placeholder until task N4 builds it. */
@Composable
fun CalendarPage() {
    val c = ChiLiV3.colors
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.headerTop, c.headerBottom)))) {
        Box(Modifier.fillMaxSize().clip(SheetShape).background(c.sheet), contentAlignment = Alignment.Center) {
            Text("Lịch: sắp có", color = c.onSheetMuted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
