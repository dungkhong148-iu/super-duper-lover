package com.superduperlover.finance.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.R
import com.superduperlover.finance.core.BudgetPeriod
import com.superduperlover.finance.core.CategoryKind
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.ui.theme.ChiLiTheme
import java.time.DayOfWeek
import java.time.LocalDate

/** Category icon keys (stored in the DB) -> vector icons from material-icons-core. */
object CategoryIcons {
    val all: Map<String, ImageVector> = linkedMapOf(
        "food" to Icons.Filled.Favorite,
        "transport" to Icons.Filled.LocationOn,
        "home" to Icons.Filled.Home,
        "bill" to Icons.Filled.Notifications,
        "shopping" to Icons.Filled.ShoppingCart,
        "fun" to Icons.Filled.ThumbUp,
        "health" to Icons.Filled.Face,
        "education" to Icons.Filled.Create,
        "other" to Icons.Filled.MoreVert,
        "salary" to Icons.Filled.AccountBox,
        "bonus" to Icons.Filled.Star,
        "income_other" to Icons.Filled.AddCircle,
        "tool" to Icons.Filled.Build,
        "plus" to Icons.Filled.Add,
    )

    fun get(key: String): ImageVector = all[key] ?: Icons.Filled.Star
}

@Composable
fun expenseColor(): Color = ChiLiTheme.colors.expense

@Composable
fun incomeColor(): Color = ChiLiTheme.colors.income

/** Red for expense, green for income, neutral for transfer. Never the only cue: icon and label go with it. */
@Composable
fun TxnKind.color(): Color = when (this) {
    TxnKind.EXPENSE -> expenseColor()
    TxnKind.INCOME -> incomeColor()
    TxnKind.TRANSFER -> ChiLiTheme.colors.transfer
}

@Composable
fun CategoryKind.color(): Color = when (this) {
    CategoryKind.EXPENSE -> expenseColor()
    CategoryKind.INCOME -> incomeColor()
}

fun TxnKind.label(): String = when (this) {
    TxnKind.EXPENSE -> "Chi tiêu"
    TxnKind.INCOME -> "Thu nhập"
    TxnKind.TRANSFER -> "Chuyển khoản"
}

fun TxnKind.icon(): ImageVector = when (this) {
    TxnKind.EXPENSE -> Icons.Filled.KeyboardArrowDown
    TxnKind.INCOME -> Icons.Filled.KeyboardArrowUp
    TxnKind.TRANSFER -> Icons.AutoMirrored.Filled.ArrowForward
}

/** "hôm nay", "tuần này", "tháng này", "quý này", "năm nay": names the current period of a budget. */
fun BudgetPeriod.thisLabel(): String = when (this) {
    BudgetPeriod.DAY -> "hôm nay"
    BudgetPeriod.WEEK -> "tuần này"
    BudgetPeriod.MONTH -> "tháng này"
    BudgetPeriod.QUARTER -> "quý này"
    BudgetPeriod.YEAR -> "năm nay"
}

fun AccountType.iconRes(): Int = when (this) {
    AccountType.CASH -> R.drawable.ic_account_cash
    AccountType.BANK -> R.drawable.ic_account_bank
    AccountType.CARD -> R.drawable.ic_account_card
    AccountType.EWALLET -> R.drawable.ic_account_ewallet
}

fun AccountType.label(): String = when (this) {
    AccountType.CASH -> "Tiền mặt"
    AccountType.BANK -> "Ngân hàng"
    AccountType.CARD -> "Thẻ"
    AccountType.EWALLET -> "Ví điện tử"
}

fun DayOfWeek.vietnamese(): String = when (this) {
    DayOfWeek.MONDAY -> "Thứ Hai"
    DayOfWeek.TUESDAY -> "Thứ Ba"
    DayOfWeek.WEDNESDAY -> "Thứ Tư"
    DayOfWeek.THURSDAY -> "Thứ Năm"
    DayOfWeek.FRIDAY -> "Thứ Sáu"
    DayOfWeek.SATURDAY -> "Thứ Bảy"
    DayOfWeek.SUNDAY -> "Chủ nhật"
}

/** `08/11`. */
fun LocalDate.dayMonth(): String = "%02d/%02d".format(dayOfMonth, monthValue)

/** `08/11/2026`. */
fun LocalDate.dayMonthYear(): String = "%02d/%02d/%d".format(dayOfMonth, monthValue, year)

/** `Thứ Hai, 08/11/2026`. */
fun LocalDate.display(): String =
    "%s, %02d/%02d/%d".format(dayOfWeek.vietnamese(), dayOfMonth, monthValue, year)
