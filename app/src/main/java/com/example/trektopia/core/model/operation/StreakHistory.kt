package com.example.trektopia.core.model.operation

import com.google.firebase.Timestamp

data class StreakHistory(
    val date: Timestamp = Timestamp.now(),
    val active: Boolean = false,
)