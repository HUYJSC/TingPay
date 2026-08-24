package com.tinhocgenz.tingpay.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinhocgenz.tingpay.TingPayApp
import com.tinhocgenz.tingpay.core.audio.AudioNotificationMode
import com.tinhocgenz.tingpay.core.notification.NotificationHelper
import com.tinhocgenz.tingpay.core.security.PinSecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val audioMode: AudioNotificationMode = AudioNotificationMode.TING_AND_AMOUNT,
    val isNotificationServiceEnabled: Boolean = false,
    val hasPinSet: Boolean = false,
    val isPinSavedSuccess: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TingPayApp
    private val settingRepo = app.settingRepository
    private val audioEngine = app.audioEngine
    private val pinManager = PinSecurityManager()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            val audioModeStr = settingRepo.getSetting("audio_mode", AudioNotificationMode.TING_AND_AMOUNT.name)
            val audioMode = try {
                AudioNotificationMode.valueOf(audioModeStr)
            } catch (e: Exception) {
                AudioNotificationMode.TING_AND_AMOUNT
            }
            val hasPin = settingRepo.getSetting("cashier_pin").isNotBlank()
            val serviceEnabled = NotificationHelper.isNotificationServiceEnabled(getApplication())

            _uiState.value = SettingsUiState(
                audioMode = audioMode,
                isNotificationServiceEnabled = serviceEnabled,
                hasPinSet = hasPin
            )
        }
    }

    fun setAudioMode(mode: AudioNotificationMode) {
        viewModelScope.launch {
            settingRepo.saveSetting("audio_mode", mode.name)
            _uiState.value = _uiState.value.copy(audioMode = mode)
        }
    }

    fun testAudio() {
        audioEngine.notifyPaymentReceived(
            amount = 350000,
            bankName = "MBBank",
            mode = _uiState.value.audioMode
        )
    }

    fun setCashierPin(pin: String) {
        viewModelScope.launch {
            val hash = if (pin.isBlank()) "" else pinManager.hashPin(pin)
            settingRepo.saveSetting("cashier_pin", hash)
            _uiState.value = _uiState.value.copy(hasPinSet = hash.isNotBlank(), isPinSavedSuccess = true)
        }
    }
}
