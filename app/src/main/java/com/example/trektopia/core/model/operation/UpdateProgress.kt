package com.example.trektopia.core.model.operation

import com.google.firebase.firestore.DocumentReference

data class UpdateProgress(
    val taskAndRelationRef: List<Pair<DocumentReference,DocumentReference>>,
    val addedProgress: Double
)