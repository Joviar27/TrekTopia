package com.example.trektopia.core.model

import com.google.firebase.Timestamp

data class DailyStreak(
    val count: Int = 0,
    val latestActive: Timestamp? = null,
    val weeklyHistory: List<Timestamp>? = null
)