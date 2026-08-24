package com.tinhocgenz.tingpay.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tinhocgenz.tingpay.data.database.dao.AppSettingDao
import com.tinhocgenz.tingpay.data.database.dao.BankAccountDao
import com.tinhocgenz.tingpay.data.database.dao.OrderDao
import com.tinhocgenz.tingpay.data.database.dao.TransactionDao
import com.tinhocgenz.tingpay.data.database.entity.AppSettingEntity
import com.tinhocgenz.tingpay.data.database.entity.BankAccountEntity
import com.tinhocgenz.tingpay.data.database.entity.OrderEntity
import com.tinhocgenz.tingpay.data.database.entity.TransactionEntity

@Database(
    entities = [
        BankAccountEntity::class,
        OrderEntity::class,
        TransactionEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TingPayDatabase : RoomDatabase() {

    abstract fun bankAccountDao(): BankAccountDao
    abstract fun orderDao(): OrderDao
    abstract fun transactionDao(): TransactionDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: TingPayDatabase? = null

        fun getInstance(context: Context): TingPayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TingPayDatabase::class.java,
                    "tingpay.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
