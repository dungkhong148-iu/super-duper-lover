package com.superduperlover.finance.fxdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.superduperlover.finance.ui.components.SectionCard
import com.superduperlover.finance.ui.setChiLiContent
import com.superduperlover.finance.ui.theme.GlowLevel
import com.superduperlover.finance.ui.theme.Motion
import com.superduperlover.finance.ui.theme.PopDialog
import com.superduperlover.finance.ui.theme.PopSheet
import com.superduperlover.finance.ui.theme.glow
import com.superduperlover.finance.ui.theme.popIn
import com.superduperlover.finance.ui.theme.popItem
import com.superduperlover.finance.ui.theme.rememberPopNav

/** Debug-only playground for ui/theme/Motion.kt: pop dialog / sheet / screen / list rows and the two glow levels. */
class FxDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setChiLiContent { FxDemo() }
    }
}

@Composable
private fun FxDemo() {
    val nav = rememberNavController()
    var navBounce by remember { mutableFloatStateOf(Motion.BOUNCE) }
    val pop = rememberPopNav(navBounce)
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = "fx",
            modifier = Modifier.systemBarsPadding(),
            enterTransition = pop.enter,
            exitTransition = pop.exit,
            popEnterTransition = pop.popEnter,
            popExitTransition = pop.popExit,
        ) {
            composable("fx") { FxMain(onOpenScreen = { bounce -> navBounce = bounce; nav.navigate("sub") }) }
            composable("sub") {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Màn con giả", style = MaterialTheme.typography.titleLarge)
                    Text("Vào = trượt lên từ dưới + nảy + mờ dần. Ra = trượt xuống ngắn, không nảy.")
                    Button(onClick = { nav.popBackStack() }) { Text("Quay lại") }
                }
            }
        }
    }
}

@Composable
private fun FxMain(onOpenScreen: (Float) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    // Bounce of the dialog / sheet that was opened last.
    var dialogBounce by remember { mutableFloatStateOf(Motion.BOUNCE) }
    var sheetBounce by remember { mutableFloatStateOf(Motion.BOUNCE) }
    var counter by remember { mutableIntStateOf(3) }
    // bounce = null: row was there from the start (no pop).
    val rows = remember { (1..3).map { FxRow(it, null) }.toMutableStateList() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "title") { Text("ChiLi FX", style = MaterialTheme.typography.headlineMedium) }

        item(key = "pop") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BounceColumn("Nảy mạnh", Motion.BOUNCE_STRONG, Modifier.weight(1f),
                    onDialog = { dialogBounce = it; showDialog = true },
                    onSheet = { sheetBounce = it; showSheet = true },
                    onScreen = onOpenScreen,
                    onRow = { counter++; rows.add(0, FxRow(counter, it)) })
                BounceColumn("Nảy vừa", Motion.BOUNCE_MEDIUM, Modifier.weight(1f),
                    onDialog = { dialogBounce = it; showDialog = true },
                    onSheet = { sheetBounce = it; showSheet = true },
                    onScreen = onOpenScreen,
                    onRow = { counter++; rows.add(0, FxRow(counter, it)) })
            }
        }

        // Not inside a SectionCard: its clip would cut the halo.
        item(key = "glow-card") {
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Text("Glow trên thẻ", style = MaterialTheme.typography.titleMedium)
                SectionCard(title = "Không glow") { Text("Thẻ thường") }
                SectionCard(modifier = Modifier.glow(GlowLevel.SOFT), title = "Soft") { Text("Mục đang chọn / khối vừa hiện") }
                SectionCard(modifier = Modifier.glow(GlowLevel.STRONG), title = "Strong") { Text("Mạnh hơn") }
            }
        }

        item(key = "glow-btn") {
            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Text("Glow trên nút", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Lưu - không glow") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth().glow(GlowLevel.SOFT, cornerRadius = 100.dp)) { Text("Lưu - soft") }
                Button(onClick = {}, modifier = Modifier.fillMaxWidth().glow(GlowLevel.STRONG, cornerRadius = 100.dp)) { Text("Lưu - strong") }
            }
        }

        item(key = "rows-head") {
            OutlinedButton(onClick = { if (rows.isNotEmpty()) rows.removeAt(0) }) { Text("Xoá dòng trên cùng") }
        }

        items(rows, key = { it.id }) { row ->
            SectionCard(modifier = popItem().then(if (row.bounce != null) Modifier.popIn(row.bounce) else Modifier)) {
                Text("Dòng ${row.id}")
            }
        }
    }

    PopDialog(visible = showDialog, onDismissRequest = { showDialog = false }, bounce = dialogBounce) {
        SectionCard(modifier = Modifier.padding(24.dp), title = "Hộp thoại") {
            Text("Trượt lên + nảy, tắt = trượt xuống ngắn.")
            Button(onClick = { showDialog = false }, modifier = Modifier.fillMaxWidth().glow(GlowLevel.STRONG, cornerRadius = 100.dp)) { Text("Lưu") }
        }
    }

    if (showSheet) {
        PopSheet(onDismissRequest = { showSheet = false }, bounce = sheetBounce) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sheet", style = MaterialTheme.typography.titleLarge)
                Text("Nội dung nảy lên trên nền trượt.")
                Button(onClick = { showSheet = false }, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
            }
        }
    }
}

private class FxRow(val id: Int, val bounce: Float?)

@Composable
private fun BounceColumn(
    label: String,
    bounce: Float,
    modifier: Modifier,
    onDialog: (Float) -> Unit,
    onSheet: (Float) -> Unit,
    onScreen: (Float) -> Unit,
    onRow: (Float) -> Unit,
) {
    SectionCard(modifier = modifier, title = label) {
        Button(onClick = { onDialog(bounce) }, modifier = Modifier.fillMaxWidth()) { Text("Hộp thoại", maxLines = 1) }
        Button(onClick = { onSheet(bounce) }, modifier = Modifier.fillMaxWidth()) { Text("Sheet", maxLines = 1) }
        Button(onClick = { onScreen(bounce) }, modifier = Modifier.fillMaxWidth()) { Text("Màn con", maxLines = 1) }
        Button(onClick = { onRow(bounce) }, modifier = Modifier.fillMaxWidth()) { Text("Thêm dòng", maxLines = 1) }
    }
}
