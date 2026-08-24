package com.tinhocgenz.tingpay.core.security

class PinSecurityManager {

    fun hashPin(pin: String): String {
        return HashUtils.sha256("TingPay_Salt_$pin")
    }

    fun verifyPin(enteredPin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return true
        return hashPin(enteredPin) == storedHash
    }
}
