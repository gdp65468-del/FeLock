package com.selflock.app.data.local.entity

import com.selflock.app.domain.model.TargetType

data class AggregatedUsage(
    val targetId: Long,
    val targetType: TargetType,
    val totalSeconds: Long
)
