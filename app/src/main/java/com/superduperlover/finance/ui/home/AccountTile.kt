package com.superduperlover.finance.ui.home

import com.superduperlover.finance.ui.theme.tapScale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.ui.components.iconRes
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.theme.accountTileColor
import com.superduperlover.finance.ui.theme.v3Shadow

private val CardShape = RoundedCornerShape(22.dp)

/**
 * v3 account carousel (docs/ui-layout.md section 2): one card per account + a "+" tile. Tap = select / unselect
 * (filters Home in place), long-press = edit. Cards have no fixed height, so text is never cut at large font scales.
 */
@Composable
fun AccountCarousel(
    accounts: List<Pair<Account, Long>>,
    selectedId: Long?,
    hidden: Boolean,
    onSelect: (Long) -> Unit,
    onEdit: (Account) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(accounts, key = { "account-${it.first.id}" }) { (account, balance) ->
            AccountCard(account, balance, active = account.id == selectedId, hidden = hidden, onClick = { onSelect(account.id) }, onLongClick = { onEdit(account) })
        }
        item(key = "add-account") { AddAccountCard(onAdd) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountCard(account: Account, balance: Long, active: Boolean, hidden: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = ChiLiV3.colors
    Box(
        Modifier
            .width(122.dp)
            .tapScale(onClick, onLongClick)
            .v3Shadow(c.shadow2, CardShape)
            .clip(CardShape)
            .background(if (active) c.cardActive else c.card)
            .border(2.dp, if (active) c.accent else Color.Transparent, CardShape)
            .padding(12.dp),
    ) {
        if (active) Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(c.accent))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(width = 44.dp, height = 36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(account.type.iconRes()), null, Modifier.size(24.dp), tint = accountTileColor(account.colorKey))
            }
            Text(account.name, color = c.onCard, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(moneyText(balance, account.currency, hidden), color = if (balance < 0) c.expense else c.accent, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AddAccountCard(onClick: () -> Unit) {
    val c = ChiLiV3.colors
    Column(
        Modifier
            .width(76.dp)
            .tapScale(onClick)
            .v3Shadow(c.shadow2, CardShape)
            .clip(CardShape)
            .background(c.card)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.heroMid), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, null, tint = c.onHero)
        }
        Text("Thêm", color = c.onCard, style = MaterialTheme.typography.labelSmall)
    }
}
