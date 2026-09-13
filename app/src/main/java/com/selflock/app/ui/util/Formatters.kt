package com.selflock.app.ui.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Formatters {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatTime(hour: Int, minute: Int): String {
        return LocalTime.of(hour, minute).format(timeFormatter)
    }

    fun formatCountdown(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> "Restam $hours horas e $mins minutos"
            else -> "Restam $mins minutos"
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
}
