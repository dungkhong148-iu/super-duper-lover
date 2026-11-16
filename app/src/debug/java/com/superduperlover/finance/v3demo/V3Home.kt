package com.superduperlover.finance.v3demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.superduperlover.finance.core.Account
import com.superduperlover.finance.core.AccountType
import com.superduperlover.finance.core.Budget
import com.superduperlover.finance.core.MAIN_CURRENCY
import com.superduperlover.finance.core.Txn
import com.superduperlover.finance.core.TxnKind
import com.superduperlover.finance.core.accountBalance
import com.superduperlover.finance.core.budgetUsage
import com.superduperlover.finance.core.formatMoney
import com.superduperlover.finance.core.reservedByCurrency
import com.superduperlover.finance.ui.LocalRepo
import com.superduperlover.finance.ui.components.dayMonth
import com.superduperlover.finance.ui.components.iconRes
import com.superduperlover.finance.ui.theme.ChiLiV3
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

// N2 demo (b)+(c): Home "Tài khoản" of the v3 mock on real ChiLi data (read-only), and the collapsing white sheet
// over the account carousel inside the 2-page pager + yellow accordion. Nothing here is wired into the real app.

private val SheetShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)

private class HomeData(
    val accounts: List<Account>,
    val txns: List<Txn>,
    val categoryNames: Map<Long, String>,
    val budgets: List<Budget>,
    val reserved: Long,
    val sample: Boolean,
)

/** Real data of the debug app; when it has no account yet a small sample set so the gestures can still be tried. */
@Composable
private fun rememberHomeData(): HomeData {
    val repo = LocalRepo.current
    val accounts by remember(repo) { repo.accounts() }.collectAsState(initial = emptyList())
    val txns by remember(repo) { repo.txns() }.collectAsState(initial = emptyList())
    val categories by remember(repo) { repo.categories(includeArchived = true) }.collectAsState(initial = emptyList())
    val budgets by remember(repo) { repo.budgets() }.collectAsState(initial = emptyList())
    val jars by remember(repo) { repo.jarSavings() }.collectAsState(initial = emptyList())
    val reserved = reservedByCurrency(jars)[MAIN_CURRENCY] ?: 0L
    if (accounts.isNotEmpty()) {
        return HomeData(accounts, txns, categories.associate { it.id to it.name }, budgets, reserved, sample = false)
    }
    val today = LocalDate.now()
    val sampleAccounts = listOf(
        Account(1, "Tiền mặt", AccountType.CASH, 30_000),
        Account(2, "Ngân hàng", AccountType.BANK, 100_000),
        Account(3, "Ví điện tử", AccountType.EWALLET, 5_000),
    )
    val names = mapOf(1L to "Ăn uống", 2L to "Lương", 3L to "Đi lại", 4L to "Cà phê")
    val sampleTxns = (0..11).map { i ->
        val income = i % 5 == 0
        Txn(
            id = i + 1L, kind = if (income) TxnKind.INCOME else TxnKind.EXPENSE, amount = if (income) 150_000L else 800L + i * 120L,
            accountId = 1L + i % 3, toAccountId = null, categoryId = if (income) 2L else listOf(1L, 3L, 4L)[i % 3],
            date = today.minusDays(i * 2L), note = "",
        )
    }
    return HomeData(sampleAccounts, sampleTxns, names, emptyList(), 0L, sample = true)
}

@Composable
fun V3HomeFrame(collapsing: Boolean) {
    val c = ChiLiV3.colors
    val data = rememberHomeData()
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<Long?>(null) }
    val label = selected?.let { id -> data.accounts.firstOrNull { it.id == id }?.name }
        ?: if (data.sample) "Tất cả · mẫu" else "Tất cả"

    Column(Modifier.fillMaxSize().background(c.headerTop)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFBEF264)))
            Text(label, Modifier.weight(1f), color = c.onHeader, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.clip(CircleShape).background(Color(0x40000000)).padding(4.dp)) {
                listOf("Tài khoản", "Lịch").forEachIndexed { i, title ->
                    val on = pager.currentPage == i
                    Box(
                        Modifier.clip(CircleShape).background(if (on) c.cardActive else Color.Transparent)
                            .clickable { scope.launch { pager.animateScrollToPage(i) } }.padding(horizontal = 14.dp, vertical = 6.dp),
                    ) { Text(title, color = if (on) c.onCard else c.onHeader, style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
        HorizontalPager(pager, Modifier.weight(1f), verticalAlignment = Alignment.Top) { page ->
            if (page == 0) {
                V3AccountsPage(data, selected, onSelect = { selected = if (selected == it) null else it }, collapsing = collapsing)
            } else {
                Surface(Modifier.fillMaxSize(), shape = SheetShape, color = c.sheet) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Trang Lịch (N4 làm sau)", color = c.onSheetMuted, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun V3AccountsPage(data: HomeData, selected: Long?, onSelect: (Long) -> Unit, collapsing: Boolean) {
    val c = ChiLiV3.colors
    // Height of the carousel layer, measured (it grows with the font scale); it is also the collapse distance.
    var carouselPx by remember { mutableIntStateOf(0) }
    var offset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    suspend fun snap() {
        val target = if (offset < -carouselPx / 2f) -carouselPx.toFloat() else 0f
        animate(offset, target) { v, _ -> offset = v }
    }

    // Collapsing-toolbar pattern: swipe up slides the sheet over the carousel before the sheet content scrolls;
    // swipe down brings it back once the content is at its top. The sticky bar above the pager never moves.
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f || offset <= -carouselPx.toFloat()) return Offset.Zero
                val new = (offset + available.y).coerceAtLeast(-carouselPx.toFloat())
                return Offset(0f, new - offset).also { offset = new }
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f || offset >= 0f) return Offset.Zero
                val new = (offset + available.y).coerceAtMost(0f)
                return Offset(0f, new - offset).also { offset = new }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                snap()
                return Velocity.Zero
            }
        }
    }

    val accountsShown = if (selected == null) data.accounts else data.accounts.filter { it.id == selected }
    // Page = one clipped box with the header gradient behind everything: the carousel slides up (clipped), the sheet is
    // laid out from the carousel's bottom edge (+offset, never above 0) down to the page bottom, so nothing can
    // overlap the bar or leave a gap.
    Box(
        Modifier.fillMaxSize().clipToBounds()
            .background(Brush.verticalGradient(listOf(c.headerTop, c.headerBottom)))
            .then(if (collapsing) Modifier.nestedScroll(connection) else Modifier),
    ) {
        LazyRow(
            Modifier.fillMaxWidth().onSizeChanged { carouselPx = it.height }
                .graphicsLayer { translationY = if (collapsing) offset else 0f },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(data.accounts, key = { it.id }) { a ->
                AccountCard(a, accountBalance(a, data.txns), active = a.id == selected, onClick = { onSelect(a.id) })
            }
            item(key = "add") { AddTile() }
        }

        Surface(
            Modifier.layout { m, cons ->
                val top = (carouselPx + (if (collapsing) offset else 0f)).roundToInt().coerceIn(0, cons.maxHeight)
                val p = m.measure(Constraints.fixed(cons.maxWidth, cons.maxHeight - top))
                layout(cons.maxWidth, cons.maxHeight) { p.place(0, top) }
            },
            shape = SheetShape, color = c.sheet, shadowElevation = 12.dp,
        ) {
            Column {
                val handleMod = if (collapsing) {
                    Modifier.draggable(
                        rememberDraggableState { d -> offset = (offset + d).coerceIn(-carouselPx.toFloat(), 0f) },
                        Orientation.Vertical,
                        onDragStopped = { snap() },
                    ).clickable { scope.launch { animate(offset, if (offset < 0f) 0f else -carouselPx.toFloat()) { v, _ -> offset = v } } }
                } else Modifier
                Box(Modifier.fillMaxWidth().then(handleMod).padding(top = 10.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(40.dp).height(6.dp).clip(CircleShape).background(c.onSheetMuted.copy(alpha = 0.35f)))
                }
                SheetContent(data, selected, accountsShown)
            }
        }
    }
}

@Composable
private fun AccountCard(a: Account, balance: Long, active: Boolean, onClick: () -> Unit) {
    val c = ChiLiV3.colors
    // No fixed height: the card grows with the font (1.25 em line at "Chữ 1,25").
    Box(
        Modifier.width(122.dp).clip(RoundedCornerShape(22.dp))
            .background(if (active) c.cardActive else c.card)
            .border(2.dp, if (active) c.accent else Color.Transparent, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick).padding(12.dp),
    ) {
        if (active) Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(c.accent))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(44.dp).height(36.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(a.type.iconRes()), null, Modifier.size(24.dp), tint = c.onCard)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(a.name, color = c.onCard, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatMoney(balance, a.currency), color = c.accent, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AddTile() {
    val c = ChiLiV3.colors
    Column(
        Modifier.width(76.dp).height(76.dp).clip(RoundedCornerShape(22.dp)).background(c.card),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, null, tint = c.onCard)
        Text("Thêm", color = c.onCard, style = MaterialTheme.typography.labelSmall)
    }
}

private enum class Range(val label: String) { DAY("Ngày"), WEEK("Tuần"), MONTH("Tháng"), ALL("Tất cả") }

private fun Range.contains(d: LocalDate, today: LocalDate): Boolean = when (this) {
    Range.DAY -> d == today
    Range.WEEK -> !d.isBefore(today.minusDays(today.dayOfWeek.value - 1L)) && !d.isAfter(today.plusDays(7L - today.dayOfWeek.value))
    Range.MONTH -> d.year == today.year && d.month == today.month
    Range.ALL -> true
}

@Composable
private fun SheetContent(data: HomeData, selected: Long?, accountsShown: List<Account>) {
    val c = ChiLiV3.colors
    val today = remember { LocalDate.now() }
    var hidden by remember { mutableStateOf(false) }
    var typeFilter by remember { mutableStateOf<TxnKind?>(null) }
    var range by remember { mutableStateOf(Range.ALL) }
    var expanded by remember { mutableStateOf(true) }

    val ids = accountsShown.map { it.id }.toSet()
    val mine = data.txns.filter { it.accountId in ids || it.toAccountId in ids }
    val balance = accountsShown.filter { it.currency == MAIN_CURRENCY }.sumOf { accountBalance(it, data.txns) }
    val monthTxns = mine.filter { Range.MONTH.contains(it.date, today) }
    val income = monthTxns.filter { it.kind == TxnKind.INCOME && it.accountId in ids }.sumOf { it.amount }
    val expense = monthTxns.filter { it.kind == TxnKind.EXPENSE && it.accountId in ids }.sumOf { it.amount }
    val totalBudgets = data.budgets.filter { it.categoryId == null && it.currency == MAIN_CURRENCY }
    val left = totalBudgets.takeIf { it.isNotEmpty() }?.sumOf { budgetUsage(it, data.txns, data.accounts, today).remaining }
    val shown = mine.filter { (typeFilter == null || it.kind == typeFilter) && range.contains(it.date, today) }
    fun money(v: Long) = if (hidden) "•••••" else formatMoney(v, MAIN_CURRENCY)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero
        Box(Modifier.clip(RoundedCornerShape(28.dp)).background(c.heroOuter).padding(5.dp)) {
            Column(
                Modifier.clip(RoundedCornerShape(23.dp))
                    .background(Brush.linearGradient(listOf(c.heroStart, c.heroMid, c.heroEnd)))
                    .padding(16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(accountsShown.singleOrNull()?.name ?: "Tổng số dư", color = c.onHero, style = MaterialTheme.typography.labelLarge)
                    Box(
                        Modifier.clip(CircleShape).background(Color(0x1A000000)).clickable { hidden = !hidden }.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) { Text(if (hidden) "Hiện" else "Ẩn", color = c.onHero, style = MaterialTheme.typography.labelMedium) }
                }
                Text(money(balance), color = c.heroBalance, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.heroWell).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Còn tiêu được kỳ này", color = c.onHero, style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.clip(CircleShape).background(Color(0xBFFFFFFF)).padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (left == null) "—" else money(left), color = Color(0xFF0F172A), style = MaterialTheme.typography.labelMedium)
                            Icon(Icons.Filled.KeyboardArrowRight, null, Modifier.size(16.dp), tint = Color(0xFF0F172A))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tự do: ${money(balance - data.reserved)}", color = c.onHero, style = MaterialTheme.typography.labelMedium)
                        Text("Trong hũ: ${money(data.reserved)}", color = c.onHero, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TypePill("THU NHẬP", "+" + money(income), c.incomePill, typeFilter == TxnKind.INCOME, Modifier.weight(1f)) {
                        typeFilter = if (typeFilter == TxnKind.INCOME) null else TxnKind.INCOME
                    }
                    TypePill("CHI TIÊU", "-" + money(expense), c.expensePill, typeFilter == TxnKind.EXPENSE, Modifier.weight(1f)) {
                        typeFilter = if (typeFilter == TxnKind.EXPENSE) null else TxnKind.EXPENSE
                    }
                }
            }
        }

        // Yellow block (accordion)
        Column(Modifier.clip(RoundedCornerShape(24.dp)).background(c.yellow).border(1.dp, c.yellowBorder, RoundedCornerShape(24.dp))) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = c.onYellow)
                Text("Giao dịch", color = c.onYellow, style = MaterialTheme.typography.titleSmall)
                Box(Modifier.clip(CircleShape).background(c.yellowChip).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("${shown.size}", color = c.onYellow, style = MaterialTheme.typography.labelSmall)
                }
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.yellowChip).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Range.entries.forEach { r ->
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (r == range) c.yellowChipSelected else Color.Transparent)
                                    .clickable { range = r }.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(r.label, color = c.onYellow, style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                    if (typeFilter != null) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.yellowChipSelected.copy(alpha = 0.8f)).padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(if (typeFilter == TxnKind.INCOME) "Đang lọc: Thu nhập" else "Đang lọc: Chi tiêu", color = c.onYellow, style = MaterialTheme.typography.labelMedium)
                            Text("Xóa lọc", color = c.accent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { typeFilter = null })
                        }
                    }
                    if (shown.isEmpty()) {
                        Text("Không có giao dịch", color = c.onYellow, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
                    }
                    shown.take(20).forEach { t -> TxnRow(t, data, ::money) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${shown.size} giao dịch", color = c.onYellow, style = MaterialTheme.typography.labelMedium)
                        Box(Modifier.clip(CircleShape).background(Color(0xE60F172A)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Xem chi tiết", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypePill(label: String, value: String, fill: Color, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = ChiLiV3.colors
    Row(
        modifier.clip(CircleShape).background(fill)
            .border(if (active) BorderStroke(2.dp, Color.White) else BorderStroke(0.dp, Color.Transparent), CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, color = c.onPill, style = MaterialTheme.typography.labelSmall)
            Text(value, color = c.onPill, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TxnRow(t: Txn, data: HomeData, money: (Long) -> String) {
    val c = ChiLiV3.colors
    val title = when {
        t.kind == TxnKind.TRANSFER -> "Chuyển khoản"
        t.note.isNotBlank() -> t.note
        else -> data.categoryNames[t.categoryId] ?: "Giao dịch"
    }
    val color = when (t.kind) {
        TxnKind.INCOME -> c.income
        TxnKind.EXPENSE -> c.expense
        TxnKind.TRANSFER -> c.onTxRow
    }
    val sign = when (t.kind) { TxnKind.INCOME -> "+"; TxnKind.EXPENSE -> "-"; TxnKind.TRANSFER -> "" }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.txRow).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.onTxRow, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.date.dayMonth(), color = c.onSheetMuted, style = MaterialTheme.typography.labelSmall)
        }
        Text(sign + money(t.amount), color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
