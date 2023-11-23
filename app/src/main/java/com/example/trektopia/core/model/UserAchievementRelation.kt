package com.example.trektopia.core.model

import com.example.trektopia.core.model.abstraction.Relation
import com.google.firebase.Timestamp

data class UserAchievementRelation(
    override val userRef: String,
    override val taskRef: String,
    override val progress: Progress,
    override val activeDate: Timestamp? = null,
): Relation(userRef,taskRef,progress,activeDate)