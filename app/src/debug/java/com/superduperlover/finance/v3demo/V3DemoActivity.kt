package com.superduperlover.finance.v3demo

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.ui.setChiLiContent
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.V3Theme
import com.superduperlover.finance.ui.theme.V3_FONT_CALIBRATION

/**
 * Debug-only "ChiLi v3" (task N2): font + colour calibration samples, Home "Tài khoản" mock-up (light / dark) and the
 * collapsing white sheet over the account carousel. Real ChiLi debug data, read-only. Nothing is wired into the real app.
 */
class V3DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setChiLiContent { V3Demo() }
    }
}

private val Options = listOf("Chốt ×0,98" to V3_FONT_CALIBRATION, "A ×1,15" to 1.15f)

@Composable
private fun V3Demo() {
    var dark by remember { mutableStateOf(false) }
    var big by remember { mutableStateOf(false) }
    var collapsing by remember { mutableStateOf(true) }
    var calib by remember { mutableFloatStateOf(V3_FONT_CALIBRATION) }
    var tab by remember { mutableStateOf(0) }

    // Header is green in both modes -> light status icons; nav bar follows the sheet colour.
    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(dark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
        )
        onDispose {}
    }
    val density = LocalDensity.current
    // "Cỡ chữ 1,25" simulates the system font scale the user will run the app at.
    val shown = if (big) Density(density.density, 1.25f) else density
    CompositionLocalProvider(LocalDensity provides shown) {
        V3Theme(dark = dark, calibration = calib) {
            val c = ChiLiV3.colors
            Column(Modifier.fillMaxSize().background(c.headerTop)) {
                Column(Modifier.fillMaxWidth().background(c.sheet).statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Toggle("Tối", dark) { dark = it }
                        Toggle("Chữ 1,25", big) { big = it }
                        Toggle("Sheet co lại", collapsing) { collapsing = it }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip("Chữ", tab == 0) { tab = 0 }
                        Chip("Home", tab == 1) { tab = 1 }
                        Text("|", color = c.onSheetMuted)
                        Options.forEach { (label, v) -> Chip(label, calib == v) { calib = v } }
                    }
                }
                Box(Modifier.weight(1f)) {
                    if (tab == 0) TypeTab(dark) else V3HomeFrame(collapsing)
                }
            }
        }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = ChiLiV3.colors.onSheet, style = MaterialTheme.typography.labelMedium)
        Switch(on, onChange)
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = ChiLiV3.colors
    Box(
        Modifier.clip(CircleShape).background(if (selected) c.accent else c.card).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(label, color = if (selected) c.sheet else c.onCard, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
}

/** (a) Type scale: each calibration option + today's Roboto for comparison. */
@Composable
private fun TypeTab(dark: Boolean) {
    val c = ChiLiV3.colors
    Column(
        Modifier.fillMaxSize().background(c.sheet).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Mỗi thẻ cùng một nội dung. Chọn cỡ nào nhìn giống app hiện tại nhất (thẻ \"Hiện tại\"). Hàng cuối (nền hồng) cắt phần thừa: dấu chồng trên chữ hoa không được bị cụt (bật \"Chữ 1,25\").",
            color = c.onSheetMuted, style = MaterialTheme.typography.bodySmall,
        )
        MaterialTheme(colorScheme = MaterialTheme.colorScheme, typography = Typography()) { SampleCard("Hiện tại (Roboto)") }
        Options.forEach { (label, v) ->
            V3Theme(dark = dark, calibration = v) { SampleCard("$label (UVN Ai Cập Nhẹ)") }
        }
    }
}

@Composable
private fun SampleCard(title: String) {
    val c = ChiLiV3.colors
    val t = MaterialTheme.typography
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = c.accent, style = t.labelLarge)
        Text(formatMoney(12_345_678, Currency.VND), color = c.heroBalance, style = t.headlineLarge, maxLines = 1)
        Text(formatMoney(-45_000, Currency.TWD), color = c.expense, style = t.titleLarge, maxLines = 1)
        Text("Tổng số dư · Còn tiêu được kỳ này", color = c.onCard, style = t.titleMedium)
        Text("Chuyển khoản, ghi chú giao dịch dài một chút để xem xuống dòng.", color = c.onCard, style = t.bodyMedium)
        Text("THU NHẬP · CHI TIÊU · NGÂN SÁCH · KHO HŨ", color = c.onCard, style = t.labelMedium)
        Text("Ngày 14/11 · Thứ Hai · Tuần 3", color = c.onSheetMuted, style = t.bodySmall)
        Text(
            "ỆỔẶỮẪỨỀ Ệ Ộ Ậ Ặ Ễ",
            Modifier.fillMaxWidth().clipToBounds().background(c.expense.copy(alpha = 0.15f)),
            color = c.onCard, style = t.titleLarge,
        )
    }
}
