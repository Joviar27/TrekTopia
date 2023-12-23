package com.example.trektopia.core.model

import com.example.trektopia.core.model.Progress
import com.google.firebase.Timestamp

data class Relation(
    val userRef: String = "",
    val taskRef: String = "",
    val progress: Progress = Progress(),
    val activeDate: Timestamp? = Timestamp.now(),
)