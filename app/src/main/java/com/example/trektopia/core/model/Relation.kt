package com.example.trektopia.core.model

import com.google.firebase.Timestamp

abstract class Relation(
    open val userRef: String,
    open val taskRef: String,
    open val progress: Progress,
    open val activeDate: Timestamp?,
)