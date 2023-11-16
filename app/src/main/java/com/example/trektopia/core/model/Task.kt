package com.example.trektopia.core.model

abstract class Task(
    open val id: String,
    open val name: String,
    open val reward: Int,
    open val status: Boolean,
    open val requirement: Double,
    open val type: ProgressType
)