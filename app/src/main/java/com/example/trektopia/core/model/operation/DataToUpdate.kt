package com.example.trektopia.core.model.operation

import com.example.trektopia.core.model.Progress
import com.google.firebase.firestore.DocumentReference

data class DataToUpdate (
    val relationRef: DocumentReference,
    val requirement: Double,
    val progress: Progress,
    val addedProgress: Double
    )