package com.selflock.app.vpn

import com.selflock.app.data.local.entity.WebsiteRule
import com.selflock.app.data.repository.WebsiteRuleRepository
import com.selflock.app.domain.model.BlockType
import com.selflock.app.domain.usecase.CheckBlockStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistManager @Inject constructor(
    private val websiteRuleRepository: WebsiteRuleRepository,
    private val checkBlockStatusUseCase: CheckBlockStatusUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _blockedDomains = MutableStateFlow<Set<String>>(emptySet())
    val blockedDomains: StateFlow<Set<String>> = _blockedDomains

    private var cachedRules: List<WebsiteRule> = emptyList()

    init {
        scope.launch {
            websiteRuleRepository.getActiveRules().collect { rules ->
                cachedRules = rules
                refreshBlocklist()
            }
        }
    }

    fun refreshBlocklist() {
        scope.launch {
            val activeDomains = mutableSetOf<String>()
            for (rule in cachedRules) {
                activeDomains.add(rule.domain.lowercase())
            }
            _blockedDomains.value = activeDomains
        }
    }

    fun isDomainBlocked(domain: String): Boolean {
        val normalized = domain.lowercase().removeSuffix(".")
        val blocked = _blockedDomains.value
        return blocked.any { normalized == it || normalized.endsWith(".$it") }
    }

    fun shouldBlockDoh(): Boolean = _blockedDomains.value.isNotEmpty()
}
