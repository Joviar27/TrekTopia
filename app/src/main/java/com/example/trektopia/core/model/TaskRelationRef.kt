package com.example.trektopia.core.model

import com.google.firebase.firestore.DocumentReference

data class TaskRelationRef(
    val taskAndRelationRef: List<Pair<DocumentReference,DocumentReference>>,
    val addedProgress: Double
)