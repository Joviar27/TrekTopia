package com.example.trektopia.core.model

data class User (
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val pictureUri: String? = "",
    val point: Int = 0,
    val dailyStreak: DailyStreak = DailyStreak()
)