package com.tinhocgenz.tingpay.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object CreatePayment : Screen("create_payment")
    data object QrPayment : Screen("qr_payment/{orderId}") {
        fun createRoute(orderId: String) = "qr_payment/$orderId"
    }
    data object PaymentSuccess : Screen("payment_success/{amount}/{bankName}/{orderCode}") {
        fun createRoute(amount: Long, bankName: String, orderCode: String) = "payment_success/$amount/$bankName/$orderCode"
    }
    data object BankAccountList : Screen("bank_account_list")
    data object AddBankAccount : Screen("add_bank_account")
    data object CashierMode : Screen("cashier_mode")
    data object History : Screen("history")
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")
}
