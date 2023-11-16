package com.example.trektopia.core.model

data class TaskProgress(
    val task: Task,
    val progress: Progress,
    val relationId: String
)