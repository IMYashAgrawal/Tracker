package com.yash.tracker.analytics.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateHelpers {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    fun parseDate(dateStr: String): LocalDate = LocalDate.parse(dateStr, dateFormatter)
    fun parseTime(timeStr: String): LocalTime = LocalTime.parse(timeStr, timeFormatter)
    
    fun toLocalDateTime(dateStr: String, timeStr: String): LocalDateTime {
        return LocalDateTime.of(parseDate(dateStr), parseTime(timeStr))
    }

    fun getGapMinutes(before: LocalDateTime, after: LocalDateTime): Long {
        return ChronoUnit.MINUTES.between(before, after)
    }

    fun getTimeBucket(time: LocalTime): String {
        val hour = time.hour
        return when {
            hour in 5..7 -> "Early morning: 5–8 AM"
            hour in 8..11 -> "Morning: 8 AM–12 PM"
            hour in 12..16 -> "Afternoon: 12–5 PM"
            hour in 17..20 -> "Evening: 5–9 PM"
            hour in 21..23 -> "Night: 9 PM–12 AM"
            else -> "Late night: 12–5 AM"
        }
    }
}
