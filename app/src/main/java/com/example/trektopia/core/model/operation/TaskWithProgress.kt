package com.example.trektopia.core.model.operation

import com.example.trektopia.core.model.Progress
import com.example.trektopia.core.model.abstraction.Task
import com.google.firebase.Timestamp

data class TaskWithProgress(
    val task: Task,
    val progress: Progress,
    val activeDate: Timestamp?,
    val relationId: String
)