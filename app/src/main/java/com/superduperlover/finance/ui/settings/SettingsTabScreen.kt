package com.superduperlover.finance.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.netWorthByCurrency
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.CheckRow
import com.superduperlover.finance.ui.components.ChoiceButtons
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.home.moneyText
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.LocalAppPrefs
import com.superduperlover.finance.ui.theme.v3Shadow

/** "Khác" tab: a list of settings entries (docs/ui-layout.md section 1, specs/N5.txt). */
@Composable
fun SettingsTabScreen(
    onOpenTransactions: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenArchive: () -> Unit,
) {
    val prefs = LocalAppPrefs.current
    val repo = LocalRepo.current
    val c = ChiLiV3.colors

    val accounts by remember(repo) { repo.accounts(includeArchived = false) }.collectAsState(initial = emptyList())
    val txns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    val debts by remember(repo) { repo.debts() }.collectAsState(initial = emptyList())
    val debtPayments by remember(repo) { repo.debtPayments() }.collectAsState(initial = emptyList())
    // Net worth: balances + what is lent - what is owed (same number as the Hero total).
    val balances = remember(accounts, txns, debts, debtPayments) { netWorthByCurrency(accounts, txns, debts, debtPayments) }
    val mainBalance = balances[MAIN_CURRENCY] ?: 0L

    Column(Modifier.fillMaxSize()) {
        ChiLiTopBar("Khác")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val netWorthShape = RoundedCornerShape(24.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .v3Shadow(c.shadow2, netWorthShape)
                    .clip(netWorthShape)
                    .background(c.cardActive)
                    .border(1.dp, c.cardBorder, netWorthShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Tài sản ròng",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.onCard,
                    )
                    Text(
                        text = "${accounts.size} tài khoản",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSheetMuted,
                    )
                }
                Text(
                    text = moneyText(mainBalance, MAIN_CURRENCY, prefs.hideBalance),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (mainBalance < 0) c.expense else c.income,
                )
                val secondaryCurrencies = balances.filterKeys { it != MAIN_CURRENCY }
                if (secondaryCurrencies.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        secondaryCurrencies.forEach { (curr, amount) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c.card)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "${curr.name}: ${moneyText(amount, curr, prefs.hideBalance)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = c.onSheetMuted,
                                )
                            }
                        }
                    }
                }
            }

            val settingsShape = RoundedCornerShape(24.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .v3Shadow(c.shadow2, settingsShape)
                    .clip(settingsShape)
                    .background(c.cardActive)
                    .border(1.dp, c.cardBorder, settingsShape)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                SettingsRow("Tra cứu sổ giao dịch", "Tìm, lọc theo ngày, tài khoản, danh mục", onOpenTransactions)
                HorizontalDivider(color = c.cardBorder)
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OneLineText(text = "Giao diện", style = MaterialTheme.typography.titleMedium, color = c.onCard)
                    ChoiceButtons(options = listOf("Sáng", "Tối"), selectedIndex = if (prefs.dark) 1 else 0, onSelect = { prefs.dark = it == 1 })
                }
                HorizontalDivider(color = c.cardBorder)
                CheckRow(text = "Ẩn số dư", subtitle = "Che số tiền trên Trang chủ (cùng nút con mắt)", checked = prefs.hideBalance, onChange = { prefs.hideBalance = it })
                HorizontalDivider(color = c.cardBorder)
                SettingsRow("Danh mục", "Thêm, đổi tên danh mục chi tiêu và thu nhập", onOpenCategories)
                HorizontalDivider(color = c.cardBorder)
                SettingsRow("Lưu trữ", "Khôi phục hoặc xóa hẳn tài khoản, danh mục, hũ", onOpenArchive)
                HorizontalDivider(color = c.cardBorder)
                SettingsRow("Sao lưu và khôi phục", "Xuất hoặc nhập toàn bộ dữ liệu ra tệp", onOpenBackup)
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    val c = ChiLiV3.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            OneLineText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = c.onCard,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = c.onSheetMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = c.onSheetMuted,
        )
    }
}
