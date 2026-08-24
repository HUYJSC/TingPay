package com.tinhocgenz.tingpay.ui.screens.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tinhocgenz.tingpay.TingPayApp
import com.tinhocgenz.tingpay.domain.model.DashboardStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TingPayApp
    private val repo = app.transactionRepository

    private val _stats = MutableStateFlow(DashboardStatistics())
    val stats: StateFlow<DashboardStatistics> = _stats.asStateFlow()

    private val _timeRange = MutableStateFlow("TODAY") // "TODAY", "WEEK", "MONTH"
    val timeRange: StateFlow<String> = _timeRange.asStateFlow()

    init {
        loadStatistics("TODAY")
    }

    fun setTimeRange(range: String) {
        _timeRange.value = range
        loadStatistics(range)
    }

    private fun loadStatistics(range: String) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)

            when (range) {
                "WEEK" -> calendar.add(Calendar.DAY_OF_YEAR, -7)
                "MONTH" -> calendar.add(Calendar.DAY_OF_YEAR, -30)
            }

            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()
            val result = repo.getStatistics(startTime, endTime)
            _stats.value = result
        }
    }
}
