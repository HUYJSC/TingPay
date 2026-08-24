package com.tinhocgenz.tingpay

import android.app.Application
import com.tinhocgenz.tingpay.core.audio.AudioEngine
import com.tinhocgenz.tingpay.data.database.TingPayDatabase
import com.tinhocgenz.tingpay.data.repository.BankAccountRepositoryImpl
import com.tinhocgenz.tingpay.data.repository.OrderRepositoryImpl
import com.tinhocgenz.tingpay.data.repository.SettingRepositoryImpl
import com.tinhocgenz.tingpay.data.repository.TransactionRepositoryImpl
import com.tinhocgenz.tingpay.domain.repository.BankAccountRepository
import com.tinhocgenz.tingpay.domain.repository.OrderRepository
import com.tinhocgenz.tingpay.domain.repository.SettingRepository
import com.tinhocgenz.tingpay.domain.repository.TransactionRepository
import com.tinhocgenz.tingpay.domain.usecase.CreateOrderUseCase
import com.tinhocgenz.tingpay.domain.usecase.ProcessNotificationUseCase
import com.tinhocgenz.tingpay.payment.duplicate.DuplicateDetector
import com.tinhocgenz.tingpay.payment.matcher.OrderMatchingEngine
import com.tinhocgenz.tingpay.payment.normalizer.TransactionNormalizer
import com.tinhocgenz.tingpay.payment.parser.BankParserRegistry

class TingPayApp : Application() {

    // Database
    val database: TingPayDatabase by lazy { TingPayDatabase.getInstance(this) }

    // Repositories
    val bankAccountRepository: BankAccountRepository by lazy { BankAccountRepositoryImpl(database) }
    val orderRepository: OrderRepository by lazy { OrderRepositoryImpl(database) }
    val transactionRepository: TransactionRepository by lazy { TransactionRepositoryImpl(database) }
    val settingRepository: SettingRepository by lazy { SettingRepositoryImpl(database) }

    // Audio Engine
    val audioEngine: AudioEngine by lazy { AudioEngine(this) }

    // Payment Engine
    val parserRegistry: BankParserRegistry by lazy { BankParserRegistry() }
    val duplicateDetector: DuplicateDetector by lazy { DuplicateDetector(transactionRepository) }
    val normalizer: TransactionNormalizer by lazy { TransactionNormalizer() }
    val matchingEngine: OrderMatchingEngine by lazy { OrderMatchingEngine(orderRepository) }

    // Use Cases
    val createOrderUseCase: CreateOrderUseCase by lazy {
        CreateOrderUseCase(orderRepository, bankAccountRepository)
    }

    val processNotificationUseCase: ProcessNotificationUseCase by lazy {
        ProcessNotificationUseCase(
            parserRegistry = parserRegistry,
            duplicateDetector = duplicateDetector,
            normalizer = normalizer,
            matchingEngine = matchingEngine,
            transactionRepository = transactionRepository,
            orderRepository = orderRepository,
            settingRepository = settingRepository,
            audioEngine = audioEngine
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        audioEngine.shutdown()
    }

    companion object {
        lateinit var instance: TingPayApp
            private set
    }
}
