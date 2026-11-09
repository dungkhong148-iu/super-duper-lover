package com.superduperlover.finance.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.CURRENCY_DISPLAY_ORDER
import com.superduperlover.finance.core.Currency
import com.superduperlover.finance.core.currencySymbol
import com.superduperlover.finance.ui.theme.chiLiChipBorder
import com.superduperlover.finance.ui.theme.chiLiChipColors

/** "TWD · NT$" style label of a currency. */
fun Currency.chipLabel(): String = "$name · ${currencySymbol(this)}"

/** Row of chips to pick a currency (main currency first). [enabled] = false locks the choice. */
@Composable
fun CurrencyChips(
    selected: Currency,
    onSelect: (Currency) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Loại tiền",
) {
    Column(modifier) {
        OneLineText(label, style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CURRENCY_DISPLAY_ORDER.forEach { currency ->
                FilterChip(
                    selected = currency == selected,
                    onClick = { onSelect(currency) },
                    enabled = enabled,
                    label = { OneLineText(currency.chipLabel()) },
                    colors = chiLiChipColors(),
                    border = chiLiChipBorder(selected = currency == selected, enabled = enabled),
                )
            }
        }
    }
}
