# TingPay Proguard Rules
-keep class com.tinhocgenz.tingpay.data.database.entity.** { *; }
-keep class com.tinhocgenz.tingpay.domain.model.** { *; }
-keep class com.tinhocgenz.tingpay.payment.model.** { *; }

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
