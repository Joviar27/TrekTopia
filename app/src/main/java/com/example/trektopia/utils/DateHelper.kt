package com.example.trektopia.utils

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
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
        val formatter = DateTimeFormatter.ofPattern("dd/MM", Locale.ENGLISH)
        return localDate.format(formatter)
    }

    fun formatDayOfWeek(localDate: LocalDate): String {
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
        return localDate.format(dayOfWeekFormatter)
    }

    fun formatDateMonthYear(localDate: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
        return localDate.format(formatter)
    }

    fun formatElapsedTime(elapsedTimeMillis: Long): String {
        val totalSeconds = (elapsedTimeMillis / 1000)

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val formattedString = StringBuilder()

        when {
            hours > 0 -> formattedString.append("$hours ${if (hours == 1L) "Hour" else "Hours"} ")
            minutes > 0 -> formattedString.append("$minutes ${if (minutes == 1L) "Minute" else "Minutes"}")
            else -> formattedString.append("$seconds ${if (seconds == 1L) "Second" else "Seconds"}")
        }

        return formattedString.toString().trim()
    }

    fun formatTime(localDateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
        return formatter.format(localDateTime)
    }

    fun isTimestampInCurrentDay(timeStamp: Timestamp): Boolean {
        val currentTimestamp = Timestamp.now()

        val currentCalendar = Calendar.getInstance()
        currentCalendar.time = currentTimestamp.toDate()

        val yourTimestampCalendar = Calendar.getInstance()
        yourTimestampCalendar.time = timeStamp.toDate()

        return currentCalendar.get(Calendar.DAY_OF_YEAR) == yourTimestampCalendar.get(Calendar.DAY_OF_YEAR)
    }

    fun isTimeStampPreviousDay(timeStamp: Timestamp): Boolean {
        val currentTimestamp = Timestamp.now()

        val currentCalendar = Calendar.getInstance()
        currentCalendar.time = currentTimestamp.toDate()

        val yourTimestampCalendar = Calendar.getInstance()
        yourTimestampCalendar.time = timeStamp.toDate()

        return currentCalendar.get(Calendar.DAY_OF_YEAR) - yourTimestampCalendar.get(Calendar.DAY_OF_YEAR) == 1
    }

    fun millisToMinutes(elapsedTimeMillis: Long): Double {
        val millisecondsInMinute = 60000.0
        return elapsedTimeMillis / millisecondsInMinute
    }

}
