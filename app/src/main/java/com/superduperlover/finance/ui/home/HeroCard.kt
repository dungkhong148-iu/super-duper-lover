package com.superduperlover.finance.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.ui.components.icon
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.v3Shadow

private const val MASK = "•••••"

/**
 * v3 Hero (docs/ui-layout.md section 1): total balance with the eye, "Còn tiêu được kỳ này", Tự do / Trong hũ and the
 * Thu nhập / Chi tiêu pills. Text always wraps instead of being cut off ([hidden] masks every amount).
 */
@Composable
fun HeroCard(
    balance: Long,
    free: Long,
    reserved: Long,
    /** Remaining of the total budget(s) of this period; null = no total budget yet. */
    budgetLeft: Long?,
    periodLabel: String,
    income: Long,
    expense: Long,
    currency: Currency,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenJars: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ChiLiV3.colors
    fun money(v: Long) = if (hidden) MASK else formatMoney(v, currency)
    val outer = RoundedCornerShape(28.dp)
    Box(modifier.v3Shadow(c.shadowHero, outer).clip(outer).background(c.heroOuter).padding(5.dp)) {
        Column(
            Modifier
                .clip(RoundedCornerShape(23.dp))
                .background(Brush.linearGradient(listOf(c.heroStart, c.heroMid, c.heroEnd)))
                .padding(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tổng số dư", color = c.onHero, style = MaterialTheme.typography.labelLarge)
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color(0x1A000000)).clickable(onClick = onToggleHidden),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(if (hidden) R.drawable.ic_eye_off else R.drawable.ic_eye),
                        contentDescription = if (hidden) "Hiện số dư" else "Ẩn số dư",
                        modifier = Modifier.size(20.dp),
                        tint = c.onHero,
                    )
                }
            }
            Text(money(balance), color = if (balance < 0) c.expense else c.heroBalance, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(10.dp))

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.heroWell).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Còn được tiêu $periodLabel", Modifier.weight(1f), color = c.onHero, style = MaterialTheme.typography.labelLarge)
                    Row(
                        Modifier.clip(CircleShape).background(Color(0xBFFFFFFF)).clickable(onClick = onOpenBudgets)
                            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (budgetLeft == null) "Tạo ngân sách" else money(budgetLeft),
                            color = if (budgetLeft != null && budgetLeft < 0) c.expense else Color(0xFF0F172A),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Icon(Icons.Filled.KeyboardArrowRight, null, Modifier.size(16.dp), tint = Color(0xFF0F172A))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Tự do", color = c.onHero, style = MaterialTheme.typography.labelSmall)
                        Text(money(free), color = if (free < 0) c.expense else c.onHero, style = MaterialTheme.typography.labelLarge)
                    }
                    Column(Modifier.weight(1f).clickable(onClick = onOpenJars)) {
                        Text("Trong hũ", color = c.onHero, style = MaterialTheme.typography.labelSmall)
                        Text(money(reserved), color = c.onHero, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypePill("Thu nhập", "+" + money(income), TxnKind.INCOME, c.incomePill, Modifier.weight(1f))
                TypePill("Chi tiêu", "-" + money(expense), TxnKind.EXPENSE, c.expensePill, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TypePill(label: String, value: String, kind: TxnKind, fill: Color, modifier: Modifier) {
    val c = ChiLiV3.colors
    Row(
        modifier.v3Shadow(c.shadow2, CircleShape).clip(CircleShape).background(fill).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0x59CBD5E1)), contentAlignment = Alignment.Center) {
            Icon(kind.icon(), null, Modifier.size(16.dp), tint = c.onPill)
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = c.onPill, style = MaterialTheme.typography.labelSmall)
            Text(value, color = c.onPill, style = MaterialTheme.typography.labelLarge)
        }
    }
}
