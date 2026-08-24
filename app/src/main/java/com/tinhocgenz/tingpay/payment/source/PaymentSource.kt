package com.tinhocgenz.tingpay.payment.source

import com.tinhocgenz.tingpay.payment.model.BankNotification
import com.tinhocgenz.tingpay.payment.model.ParsedTransaction
import kotlinx.coroutines.flow.Flow

interface PaymentSource {
    val sourceName: String
    val transactions: Flow<ParsedTransaction>
}

interface NotificationPaymentSource : PaymentSource {
    suspend fun onNotificationReceived(notification: BankNotification)
}
