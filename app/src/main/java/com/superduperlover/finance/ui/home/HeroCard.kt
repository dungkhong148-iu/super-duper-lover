package com.superduperlover.finance.ui.home

import com.superduperlover.finance.ui.theme.tapScale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

/** Amount text for Home: masked when the balance is hidden. */
fun moneyText(amount: Long, currency: Currency, hidden: Boolean): String = if (hidden) MASK else formatMoney(amount, currency)

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
    /** Account name when one account is selected, else "Tổng số dư". */
    title: String,
    /** False when one account is selected: free / jar money is not per account. */
    showFree: Boolean,
    periodLabel: String,
    income: Long,
    expense: Long,
    currency: Currency,
    /** Thu nhập / Chi tiêu pill that filters the yellow block; null = none. */
    typeFilter: TxnKind?,
    onTypeFilter: (TxnKind) -> Unit,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenJars: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = ChiLiV3.colors
    fun money(v: Long) = moneyText(v, currency, hidden)
    val outer = RoundedCornerShape(28.dp)
    Box(modifier.v3Shadow(c.shadowHero, outer).clip(outer).background(c.heroOuter).padding(5.dp)) {
        Column(
            Modifier
                .clip(RoundedCornerShape(23.dp))
                .background(Brush.linearGradient(listOf(c.heroStart, c.heroMid, c.heroEnd)))
                .padding(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = c.onHero, style = MaterialTheme.typography.labelLarge)
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(c.heroButton).clickable(onClick = onToggleHidden),
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
            Text(
                money(balance),
                color = if (balance < 0) c.expense else c.heroBalance,
                style = MaterialTheme.typography.headlineLarge.copy(shadow = c.heroBalanceShadow),
            )
            Spacer(Modifier.height(10.dp))

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.heroWell).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Còn được tiêu $periodLabel", Modifier.weight(1f), color = c.onHero, style = MaterialTheme.typography.labelLarge)
                    Row(
                        Modifier.clip(CircleShape).background(c.heroChip).clickable(onClick = onOpenBudgets)
                            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (budgetLeft == null) "Tạo ngân sách" else money(budgetLeft),
                            color = if (budgetLeft != null && budgetLeft < 0) c.expense else c.onHeroChip,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Icon(Icons.Filled.KeyboardArrowRight, null, Modifier.size(16.dp), tint = c.onHeroChip)
                    }
                }
                if (showFree) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                TypePill("Thu nhập", "+" + money(income), TxnKind.INCOME, c.incomePill, typeFilter == TxnKind.INCOME, Modifier.weight(1f)) { onTypeFilter(TxnKind.INCOME) }
                TypePill("Chi tiêu", "-" + money(expense), TxnKind.EXPENSE, c.expensePill, typeFilter == TxnKind.EXPENSE, Modifier.weight(1f)) { onTypeFilter(TxnKind.EXPENSE) }
            }
        }
    }
}

@Composable
private fun TypePill(label: String, value: String, kind: TxnKind, fill: Color, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = ChiLiV3.colors
    Row(
        modifier.tapScale(onClick).v3Shadow(c.shadow2, CircleShape).clip(CircleShape).background(fill)
            .border(2.dp, if (active) Color.White else Color.Transparent, CircleShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
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
