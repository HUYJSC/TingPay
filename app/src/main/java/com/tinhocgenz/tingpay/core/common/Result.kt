package com.tinhocgenz.tingpay.core.common

sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>
}

inline fun <T> AppResult<T>.onSuccess(action: (value: T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (message: String, cause: Throwable?) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(message, cause)
    return this
}
