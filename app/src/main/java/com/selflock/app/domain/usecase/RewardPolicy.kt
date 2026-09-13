package com.selflock.app.domain.usecase

import java.time.LocalTime

object RewardPolicy {
    fun isAvailable(now: LocalTime, start: LocalTime?, end: LocalTime?): Boolean {
        if (start == null || end == null) return true
        return if (start.isBefore(end)) !now.isBefore(start) && now.isBefore(end) else !now.isBefore(start) || now.isBefore(end)
    }

    fun creditedSeconds(progressSeconds: Long, elapsedSeconds: Long, requiredSeconds: Long): Long =
        minOf(elapsedSeconds, (requiredSeconds - progressSeconds).coerceAtLeast(0))

    fun isReleased(activeUntil: Long, nowMillis: Long, releasedPackages: Set<String>, packageName: String): Boolean =
        activeUntil > nowMillis && packageName in releasedPackages
}
