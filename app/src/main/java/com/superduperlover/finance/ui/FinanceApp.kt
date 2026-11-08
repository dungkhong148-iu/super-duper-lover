package com.superduperlover.finance.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.superduperlover.finance.ui.accounts.AccountsScreen
import com.superduperlover.finance.ui.add.AddTransactionScreen
import com.superduperlover.finance.ui.budgets.BudgetsScreen
import com.superduperlover.finance.ui.categories.CategoriesScreen
import com.superduperlover.finance.ui.goals.GoalDetailScreen
import com.superduperlover.finance.ui.goals.GoalsScreen
import com.superduperlover.finance.ui.home.HomeScreen
import com.superduperlover.finance.ui.settings.SettingsScreen
import com.superduperlover.finance.ui.transactions.TransactionsScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Tổng quan", Icons.Filled.Home),
    ACCOUNTS("accounts", "Tài khoản", Icons.Filled.AccountBox),
    TRANSACTIONS("transactions", "Giao dịch", Icons.AutoMirrored.Filled.List),
    BUDGETS("budgets", "Ngân sách", Icons.Filled.DateRange),
    GOALS("goals", "Mục tiêu", Icons.Filled.Star),
}

private const val ADD_ROUTE = "add/{txnId}"
private const val CATEGORIES_ROUTE = "categories"
private const val SETTINGS_ROUTE = "settings"

@Composable
fun FinanceApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTab = Tab.entries.any { it.route == currentRoute }

    fun openTab(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { openTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = {
                                // One line, never wrapped: shrink the label instead when a large
                                // system font scale or a wide system font would not fit the item.
                                Text(
                                    tab.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    autoSize = TextAutoSize.StepBased(
                                        minFontSize = 8.sp,
                                        maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
                                        stepSize = 0.5.sp,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (onTab) {
                FloatingActionButton(onClick = { navController.navigate("add/0") }) {
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
                    onOpenBudgets = { openTab(Tab.BUDGETS) },
                    onOpenAccounts = { openTab(Tab.ACCOUNTS) },
                    onOpenGoal = { id -> navController.navigate("goal/$id") },
                )
            }
            composable(Tab.ACCOUNTS.route) {
                AccountsScreen(
                    onOpenCategories = { navController.navigate(CATEGORIES_ROUTE) },
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                )
            }
            composable(Tab.TRANSACTIONS.route) {
                TransactionsScreen(onEdit = { id -> navController.navigate("add/$id") })
            }
            composable(Tab.BUDGETS.route) { BudgetsScreen() }
            composable(Tab.GOALS.route) {
                GoalsScreen(onOpenGoal = { id -> navController.navigate("goal/$id") })
            }
            composable(SETTINGS_ROUTE) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(CATEGORIES_ROUTE) { CategoriesScreen(onBack = { navController.popBackStack() }) }
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
                        navController.popBackStack()
                        openTab(Tab.ACCOUNTS)
                    },
                )
            }
        }
    }
}
