package com.janreins.piso.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility functions for date formatting, month keys, and time calculations.
 */
object DateUtil {

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val shortDateFormat = SimpleDateFormat("MMM d", Locale.US)
    private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Formats timestamp into readable date (e.g. "Sep 1, 2026")
     */
    fun formatDate(millis: Long): String {
        return fullDateFormat.format(Date(millis))
    }

    /**
     * Formats timestamp into short date (e.g. "Sep 1")
     */
    fun formatDateShort(millis: Long): String {
        return shortDateFormat.format(Date(millis))
    }

    /**
     * Formats timestamp into "yyyy-MM-dd" for date pickers/inputs
     */
    fun formatInputDate(millis: Long): String {
        return inputDateFormat.format(Date(millis))
    }

    /**
     * Returns the current month key, e.g. "2026-09"
     */
    fun getCurrentMonthKey(): String {
        return monthKeyFormat.format(Date())
    }

    /**
     * Returns the month key for a given timestamp, e.g. "2026-09"
     */
    fun getMonthKey(millis: Long): String {
        return monthKeyFormat.format(Date(millis))
    }

    /**
     * Converts a month key like "2026-09" into "September 2026"
     */
    fun getMonthDisplayName(monthKey: String): String {
        return try {
            val date = monthKeyFormat.parse(monthKey)
            if (date != null) monthYearFormat.format(date) else monthKey
        } catch (_: Exception) {
            monthKey
        }
    }

    /**
     * Shifts monthKey by delta (-1 for previous month, +1 for next month)
     */
    fun shiftMonthKey(monthKey: String, delta: Int): String {
        return try {
            val date = monthKeyFormat.parse(monthKey) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.MONTH, delta)
            }
            monthKeyFormat.format(cal.time)
        } catch (_: Exception) {
            getCurrentMonthKey()
        }
    }

    /**
     * Returns greeting text based on current hour of day
     */
    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
