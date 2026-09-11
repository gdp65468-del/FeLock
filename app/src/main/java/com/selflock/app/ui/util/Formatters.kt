package com.selflock.app.ui.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val locale = Locale.forLanguageTag("pt-BR")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val dateFormatter = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", locale)

    fun formatTime(hour: Int, minute: Int): String {
        return LocalTime.of(hour, minute).format(timeFormatter)
    }

    fun formatCountdown(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> if (mins > 0) "$hours h $mins min restantes" else "$hours h restantes"
            else -> "$mins min restantes"
        }
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    fun formatDateRange(start: LocalDate, end: LocalDate): String {
        return "${start.format(dateFormatter)} - ${end.format(dateFormatter)}"
    }

    fun formatDays(days: List<String>): String {
        val labels = mapOf(
            "MON" to "Seg",
            "TUE" to "Ter",
            "WED" to "Qua",
            "THU" to "Qui",
            "FRI" to "Sex",
            "SAT" to "Sáb",
            "SUN" to "Dom"
        )
        return days.joinToString(", ") { labels[it] ?: it }
    }
}
