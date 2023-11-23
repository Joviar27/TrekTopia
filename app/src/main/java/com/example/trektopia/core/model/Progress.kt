package com.example.trektopia.core.model

data class Progress(
    val percentage: Double = 0.0,
    val current: Double = 0.0,
    val enabled: Boolean = false
)