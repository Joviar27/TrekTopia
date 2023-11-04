package com.example.trektopia.core.model

import java.time.LocalDate

data class DailyStreak(
    val count: Int,
    val latestActive: LocalDate?,
    val weeklyHistory: List<LocalDate>?
)