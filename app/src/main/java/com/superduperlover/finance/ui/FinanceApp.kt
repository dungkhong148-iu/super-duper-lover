package com.superduperlover.finance.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.superduperlover.finance.ui.add.AddTransactionScreen
import com.superduperlover.finance.ui.categories.CategoriesScreen
import com.superduperlover.finance.ui.components.ChiLiTopBar
import com.superduperlover.finance.ui.components.OneLineText
import com.superduperlover.finance.ui.goals.GoalDetailScreen
import com.superduperlover.finance.ui.home.HomeScreen
import com.superduperlover.finance.ui.jars.JarsScreen
import com.superduperlover.finance.ui.settings.AppearanceScreen
import com.superduperlover.finance.ui.settings.SettingsScreen
import com.superduperlover.finance.ui.settings.SettingsTabScreen
import com.superduperlover.finance.ui.transactions.TransactionsScreen

// Bottom bar: 3 tabs (docs/ui-layout.md section 1).
private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Trang chủ", Icons.Filled.Home),
    TRANSACTIONS("transactions", "Giao dịch", Icons.AutoMirrored.Filled.List),
    SETTINGS("settings", "Cài đặt", Icons.Filled.Settings),
}

private const val ADD_ROUTE = "add/{txnId}"
private const val CATEGORIES_ROUTE = "categories"
private const val BACKUP_ROUTE = "backup"
private const val APPEARANCE_ROUTE = "appearance"
private const val JARS_ROUTE = "jars"

// Transactions tab; the optional argument opens it already filtered to one account (tap on an account tile).
private const val TRANSACTIONS_PATTERN = "transactions?accountId={accountId}"

@Composable
fun FinanceApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route?.substringBefore('?')
    val currentTab = Tab.entries.firstOrNull { it.route == currentRoute }
    val onTab = currentTab != null

    fun openTab(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (onTab) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { openTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
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
            // "+" on Trang chủ and Giao dịch only; not on Cài đặt.
            if (currentTab == Tab.HOME || currentTab == Tab.TRANSACTIONS) {
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
                    onOpenAccountTransactions = { id ->
                        navController.navigate("transactions?accountId=$id") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenGoal = { id -> navController.navigate("goal/$id") },
                    onOpenJars = { navController.navigate(JARS_ROUTE) },
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
                    ChiLiTopBar("Giao dịch")
                    TransactionsScreen(
                        onEdit = { id -> navController.navigate("add/$id") },
                        initialAccountId = accountId,
                    )
                }
            }
            composable(Tab.SETTINGS.route) {
                SettingsTabScreen(
                    onOpenCategories = { navController.navigate(CATEGORIES_ROUTE) },
                    onOpenBackup = { navController.navigate(BACKUP_ROUTE) },
                    onOpenAppearance = { navController.navigate(APPEARANCE_ROUTE) },
                )
            }
            composable(APPEARANCE_ROUTE) { AppearanceScreen(onBack = { navController.popBackStack() }) }
            composable(BACKUP_ROUTE) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(CATEGORIES_ROUTE) { CategoriesScreen(onBack = { navController.popBackStack() }) }
            composable(JARS_ROUTE) {
                JarsScreen(
                    onBack = { navController.popBackStack() },
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
