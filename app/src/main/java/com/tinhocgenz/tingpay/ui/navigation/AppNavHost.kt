package com.tinhocgenz.tingpay.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tinhocgenz.tingpay.ui.screens.account.AddBankAccountScreen
import com.tinhocgenz.tingpay.ui.screens.account.BankAccountListScreen
import com.tinhocgenz.tingpay.ui.screens.cashier.CashierModeScreen
import com.tinhocgenz.tingpay.ui.screens.history.TransactionHistoryScreen
import com.tinhocgenz.tingpay.ui.screens.home.HomeScreen
import com.tinhocgenz.tingpay.ui.screens.onboarding.OnboardingPermissionScreen
import com.tinhocgenz.tingpay.ui.screens.payment.CreatePaymentScreen
import com.tinhocgenz.tingpay.ui.screens.payment.PaymentSuccessScreen
import com.tinhocgenz.tingpay.ui.screens.payment.QrPaymentScreen
import com.tinhocgenz.tingpay.ui.screens.settings.SettingsScreen
import com.tinhocgenz.tingpay.ui.screens.splash.SplashScreen
import com.tinhocgenz.tingpay.ui.screens.statistics.StatisticsScreen
import com.tinhocgenz.tingpay.ui.theme.PrimaryEmerald

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Trang chủ", Icons.Default.Home),
    BottomNavItem(Screen.CreatePayment.route, "Thanh toán", Icons.Default.QrCode),
    BottomNavItem(Screen.History.route, "Lịch sử", Icons.Default.History),
    BottomNavItem(Screen.Statistics.route, "Thống kê", Icons.Default.BarChart),
    BottomNavItem(Screen.Settings.route, "Cài đặt", Icons.Default.Settings)
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.CreatePayment.route,
        Screen.History.route,
        Screen.Statistics.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryEmerald,
                                selectedTextColor = PrimaryEmerald,
                                indicatorColor = PrimaryEmerald.copy(alpha = 0.15f)
                            ),
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingPermissionScreen(
                    onPermissionGranted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCreatePayment = { navController.navigate(Screen.CreatePayment.route) },
                    onNavigateToCashier = { navController.navigate(Screen.CashierMode.route) },
                    onNavigateToAddAccount = { navController.navigate(Screen.AddBankAccount.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                )
            }

            composable(Screen.CreatePayment.route) {
                CreatePaymentScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQr = { orderId ->
                        navController.navigate(Screen.QrPayment.createRoute(orderId))
                    }
                )
            }

            composable(
                route = Screen.QrPayment.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                QrPaymentScreen(
                    orderId = orderId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSuccess = { amount, bank, code ->
                        navController.navigate(Screen.PaymentSuccess.createRoute(amount, bank, code)) {
                            popUpTo(Screen.CreatePayment.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.PaymentSuccess.route,
                arguments = listOf(
                    navArgument("amount") { type = NavType.LongType },
                    navArgument("bankName") { type = NavType.StringType },
                    navArgument("orderCode") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val amount = backStackEntry.arguments?.getLong("amount") ?: 0L
                val bankName = backStackEntry.arguments?.getString("bankName") ?: ""
                val orderCode = backStackEntry.arguments?.getString("orderCode") ?: ""
                PaymentSuccessScreen(
                    amount = amount,
                    bankName = bankName,
                    orderCode = orderCode,
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.BankAccountList.route) {
                BankAccountListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddAccount = { navController.navigate(Screen.AddBankAccount.route) }
                )
            }

            composable(Screen.AddBankAccount.route) {
                AddBankAccountScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CashierMode.route) {
                CashierModeScreen(
                    onExitCashierMode = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                TransactionHistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Statistics.route) {
                StatisticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAccounts = { navController.navigate(Screen.BankAccountList.route) }
                )
            }
        }
    }
}
