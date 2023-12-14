package com.example.trektopia.core.model

import com.example.trektopia.core.model.operation.StreakHistory
import com.google.firebase.Timestamp

data class DailyStreak(
    val count: Int = 0,
    val longest: Int = 0,
    val latestActive: Timestamp? = null,
    val history: List<StreakHistory>? = null
)