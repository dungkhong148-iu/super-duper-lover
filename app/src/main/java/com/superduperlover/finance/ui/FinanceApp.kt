package com.superduperlover.finance.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.superduperlover.finance.R
import com.superduperlover.finance.ui.add.AddTransactionScreen
import com.superduperlover.finance.ui.categories.CategoriesScreen
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.goals.GoalDetailScreen
import com.superduperlover.finance.ui.home.HomeScreen
import com.superduperlover.finance.ui.home.PlanPage
import com.superduperlover.finance.ui.jars.JarsScreen
import com.superduperlover.finance.ui.settings.ArchiveScreen
import com.superduperlover.finance.ui.settings.SettingsScreen
import com.superduperlover.finance.ui.settings.SettingsTabScreen
import com.superduperlover.finance.ui.theme.ChiLiV3
import com.superduperlover.finance.ui.transactions.TransactionsScreen

// Bottom bar: 4 tabs (docs/ui-layout.md section 1).
private enum class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
) {
    HOME("home", "Trang chủ", icon = Icons.Filled.Home),
    BUDGET("budget", "Ngân sách", iconRes = R.drawable.ic_wallet),
    JARS("jars", "Kho hũ", iconRes = R.drawable.ic_savings),
    MORE("more", "Khác", icon = Icons.Filled.Menu),
}

private const val ADD_ROUTE = "add/{txnId}"
private const val CATEGORIES_ROUTE = "categories"
private const val BACKUP_ROUTE = "backup"
private const val ARCHIVE_ROUTE = "archive"

// Full transaction list (no longer a tab): Home "Xem chi tiết", Khác > "Tra cứu sổ giao dịch"; the optional argument opens it filtered to one account.
private const val TRANSACTIONS_PATTERN = "transactions?accountId={accountId}"

@Composable
fun FinanceApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?')
    val currentTab = Tab.entries.firstOrNull { it.route == currentRoute }
    val onTab = currentTab != null
    LocalGreenStatusBar.current.value = currentTab == Tab.HOME

    fun openTab(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        // Home has a green header that runs under the status bar.
        containerColor = if (currentTab == Tab.HOME) ChiLiV3.colors.headerTop else MaterialTheme.colorScheme.background,
        bottomBar = {
            if (onTab) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { openTab(tab) },
                            icon = {
                                if (tab.icon != null) {
                                    Icon(tab.icon, contentDescription = null)
                                } else if (tab.iconRes != null) {
                                    Icon(painterResource(tab.iconRes), contentDescription = null)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            // One line, never wrapped: shrinks on large system font scales.
                            label = {
                                val labelStyle = MaterialTheme.typography.labelMedium.let {
                                    if (it.fontSize.isSpecified && it.fontSize > 12.sp) it.copy(fontSize = 12.sp) else it
                                }
                                OneLineText(
                                    tab.label,
                                    style = labelStyle,
                                    minFontSize = 10.sp,
                                )
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // "+" on Trang chủ only.
            if (currentTab == Tab.HOME) {
                FloatingActionButton(
                    onClick = { navController.navigate("add/0") },
                    containerColor = ChiLiV3.colors.fab,
                    contentColor = ChiLiV3.colors.onFab,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Thêm giao dịch")
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.HOME.route) {
                HomeScreen(
                    onOpenTransactions = { id ->
                        navController.navigate(if (id == null) "transactions" else "transactions?accountId=$id") { launchSingleTop = true }
                    },
                    onEditTxn = { id -> navController.navigate("add/$id") },
                    onOpenBudgets = { openTab(Tab.BUDGET) },
                    onOpenGoal = { id -> navController.navigate("goal/$id") },
                    onOpenJars = { openTab(Tab.JARS) },
                )
            }
            composable(
                TRANSACTIONS_PATTERN,
                arguments = listOf(
                    navArgument("accountId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                val accountId = entry.arguments?.getLong("accountId")?.takeIf { it >= 0 }
                Column {
                    ChiLiTopBar("Giao dịch", onBack = { navController.popBackStack() })
                    TransactionsScreen(
                        onEdit = { id -> navController.navigate("add/$id") },
                        initialAccountId = accountId,
                    )
                }
            }
            composable(Tab.BUDGET.route) {
                Column {
                    ChiLiTopBar("Ngân sách")
                    PlanPage()
                }
            }
            composable(Tab.MORE.route) {
                SettingsTabScreen(
                    onOpenTransactions = { navController.navigate("transactions") { launchSingleTop = true } },
                    onOpenCategories = { navController.navigate(CATEGORIES_ROUTE) },
                    onOpenBackup = { navController.navigate(BACKUP_ROUTE) },
                    onOpenArchive = { navController.navigate(ARCHIVE_ROUTE) },
                )
            }
            composable(BACKUP_ROUTE) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(CATEGORIES_ROUTE) { CategoriesScreen(onBack = { navController.popBackStack() }) }
            composable(ARCHIVE_ROUTE) { ArchiveScreen(onBack = { navController.popBackStack() }) }
            composable(Tab.JARS.route) {
                JarsScreen(
                    onBack = null,
                    onOpenJar = { id -> navController.navigate("goal/$id") },
                )
            }
            composable(
                "goal/{goalId}",
                arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
            ) { entry ->
                GoalDetailScreen(
                    goalId = entry.arguments?.getLong("goalId") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                ADD_ROUTE,
                arguments = listOf(navArgument("txnId") { type = NavType.LongType }),
            ) { entry ->
                AddTransactionScreen(
                    txnId = entry.arguments?.getLong("txnId") ?: 0L,
                    onClose = { navController.popBackStack() },
                    onGoAccounts = {
                        // No accounts yet: go back to Trang chủ, where the account tiles live.
                        navController.popBackStack()
                        openTab(Tab.HOME)
                    },
                )
            }
        }
    }
}
