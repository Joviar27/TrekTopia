package com.example.trektopia.utils

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateHelper{
    fun timeStampToLocalDate(firebaseTimestamp: Timestamp): LocalDate {
        val instant: Instant =
            Instant.ofEpochSecond(firebaseTimestamp.seconds, firebaseTimestamp.nanoseconds.toLong())

        return instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }

    fun timeStampToLocalDateTime(firebaseTimestamp: Timestamp): LocalDateTime {
        val instant: Instant =
            Instant.ofEpochSecond(firebaseTimestamp.seconds, firebaseTimestamp.nanoseconds.toLong())

        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    fun formatDateMonth(localDate: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMMM", Locale.ENGLISH)
        return localDate.format(formatter)
    }

    fun formatDayOfWeek(localDate: LocalDate): String {
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
        return localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }

    fun formatDateMonthYear(localDate: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
        return localDate.format(formatter)
    }
}
