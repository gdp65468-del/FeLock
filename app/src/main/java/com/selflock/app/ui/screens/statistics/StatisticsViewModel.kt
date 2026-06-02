package com.selflock.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selflock.app.data.local.entity.AggregatedUsage
import com.selflock.app.data.local.entity.BlockEvent
import com.selflock.app.data.repository.AppRuleRepository
import com.selflock.app.data.repository.UsageRepository
import com.selflock.app.data.repository.WebsiteRuleRepository
import com.selflock.app.domain.model.TargetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class UsageItem(
    val targetId: Long,
    val targetType: TargetType,
    val displayName: String,
    val packageName: String?,
    val totalSeconds: Long,
    val percentage: Float
)

data class StatisticsUiState(
    val dateRangeIndex: Int = 0,
    val totalScreenTimeSeconds: Long = 0,
    val totalBlockedSeconds: Long = 0,
    val blockCount: Int = 0,
    val appUsage: List<UsageItem> = emptyList(),
    val websiteUsage: List<UsageItem> = emptyList(),
    val blockEvents: List<BlockEvent> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val appRuleRepository: AppRuleRepository,
    private val websiteRuleRepository: WebsiteRuleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsUiState())
    val state: StateFlow<StatisticsUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun setDateRange(index: Int) {
        _state.value = _state.value.copy(dateRangeIndex = index)
        loadData()
    }

    private fun getDateRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (_state.value.dateRangeIndex) {
            0 -> today to today
            1 -> today.minusDays(6) to today
            2 -> today.withDayOfMonth(1) to today
            else -> today to today
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val (startDate, endDate) = getDateRange()
            val startDateStr = startDate.toString()
            val endDateStr = endDate.toString()
            val startMillis = startDate.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            val endMillis = endDate.plusDays(1).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000

            val totalUsage = usageRepository.getTotalUsage(startDateStr, endDateStr)
            val totalBlocked = usageRepository.getTotalBlockedTime(startMillis, endMillis)
            val blockCount = usageRepository.getBlockCount(startMillis, endMillis)
            val aggregatedUsage = usageRepository.getAggregatedUsage(startDateStr, endDateStr)
            val blockEvents = usageRepository.getBlockEvents(startMillis, endMillis)

            val appRules = appRuleRepository.getAllRules().first()
            val websiteRules = websiteRuleRepository.getAllRules().first()

            val maxSeconds = aggregatedUsage.maxOfOrNull { it.totalSeconds } ?: 1L

            val appUsage = aggregatedUsage
                .filter { it.targetType == TargetType.APP }
                .map { usage ->
                    val rule = appRules.find { it.id == usage.targetId }
                    UsageItem(
                        targetId = usage.targetId,
                        targetType = TargetType.APP,
                        displayName = rule?.appName ?: "Unknown App",
                        packageName = rule?.packageName,
                        totalSeconds = usage.totalSeconds,
                        percentage = if (maxSeconds > 0) usage.totalSeconds.toFloat() / maxSeconds else 0f
                    )
                }

            val websiteUsage = aggregatedUsage
                .filter { it.targetType == TargetType.WEBSITE }
                .map { usage ->
                    val rule = websiteRules.find { it.id == usage.targetId }
                    UsageItem(
                        targetId = usage.targetId,
                        targetType = TargetType.WEBSITE,
                        displayName = rule?.domain ?: "Unknown Website",
                        packageName = null,
                        totalSeconds = usage.totalSeconds,
                        percentage = if (maxSeconds > 0) usage.totalSeconds.toFloat() / maxSeconds else 0f
                    )
                }

            _state.value = StatisticsUiState(
                dateRangeIndex = _state.value.dateRangeIndex,
                totalScreenTimeSeconds = totalUsage,
                totalBlockedSeconds = totalBlocked,
                blockCount = blockCount,
                appUsage = appUsage,
                websiteUsage = websiteUsage,
                blockEvents = blockEvents,
                isLoading = false
            )
        }
    }
}
