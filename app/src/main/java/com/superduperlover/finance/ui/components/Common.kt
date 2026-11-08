package com.superduperlover.finance.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.TxnKind
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

private val ExpenseLight = Color(0xFFC62828)
private val ExpenseDark = Color(0xFFEF9A9A)
private val IncomeLight = Color(0xFF2E7D32)
private val IncomeDark = Color(0xFFA5D6A7)

@Composable
fun expenseColor(): Color = if (isSystemInDarkTheme()) ExpenseDark else ExpenseLight

@Composable
fun incomeColor(): Color = if (isSystemInDarkTheme()) IncomeDark else IncomeLight

/** Red for expense, green for income, neutral for transfer. Never the only cue: icon and label go with it. */
@Composable
fun TxnKind.color(): Color = when (this) {
    TxnKind.EXPENSE -> expenseColor()
    TxnKind.INCOME -> incomeColor()
    TxnKind.TRANSFER -> MaterialTheme.colorScheme.onSurfaceVariant
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

/** `Thứ Hai, 08/11/2026`. */
fun LocalDate.display(): String =
    "%s, %02d/%02d/%d".format(dayOfWeek.vietnamese(), dayOfMonth, monthValue, year)

@Composable
fun ComingSoon(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$title - sắp có", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
