package com.superduperlover.finance.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.R
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.formatVnd
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.theme.OnAccountTile
import com.superduperlover.finance.ui.theme.accountTileColor

private val TileShape = RoundedCornerShape(16.dp)

/** Placeholder until accounts carry their own colour: one colour per account type. */
private fun colorKeyOf(account: Account): String = when (account.type) {
    AccountType.CASH -> "emerald"
    AccountType.BANK -> "sky"
    AccountType.CARD -> "indigo"
    AccountType.EWALLET -> "violet"
}

private fun iconOf(type: AccountType): Int = when (type) {
    AccountType.CASH -> R.drawable.ic_account_cash
    AccountType.BANK -> R.drawable.ic_account_bank
    AccountType.CARD -> R.drawable.ic_account_card
    AccountType.EWALLET -> R.drawable.ic_account_ewallet
}

/**
 * Account tile (docs/ui-layout.md section 2): filled with the account colour, type icon, name, balance.
 * Tap = transactions of this account, long-press = edit.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountTile(
    account: Account,
    balance: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(TileShape)
            .background(accountTileColor(colorKeyOf(account)))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            painter = painterResource(iconOf(account.type)),
            contentDescription = null,
            tint = OnAccountTile,
            modifier = Modifier.size(24.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            OneLineText(
                text = account.name,
                style = MaterialTheme.typography.labelLarge,
                color = OnAccountTile,
            )
            OneLineText(
                text = formatVnd(balance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnAccountTile,
            )
        }
    }
}

/** Last tile of the grid: outlined "+ Thêm tài khoản". */
@Composable
fun AddAccountTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp),
        shape = TileShape,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            OneLineText(
                text = "Thêm tài khoản",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
