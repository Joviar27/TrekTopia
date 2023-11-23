package com.example.trektopia.core.model

import com.google.firebase.Timestamp

data class DailyStreak(
    val count: Int = 0,
    val longest: Int = 0,
    val latestActive: Timestamp? = null,
    val history: List<Pair<Boolean,Timestamp>>? = null
)