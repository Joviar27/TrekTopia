package com.example.trektopia.core.model

data class DailyMission(
    override val id: String,
    override val name: String,
    override val reward: Int,
    override val status: Boolean,
    override val requirement: Double,
    override val type: ProgressType
): Task(id,name,reward,status,requirement,type)