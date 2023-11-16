package com.example.trektopia.core.utils

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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
}
