package com.example.trektopia.core.model

import android.net.Uri
import com.example.trektopia.core.model.abstraction.Task
import com.example.trektopia.core.model.enum.ProgressType

data class DailyMission(
    override val id: String,
    override val name: String,
    override val reward: Int,
    override val status: Boolean,
    override val requirement: Double,
    override val type: ProgressType,
    override val pictureUri: Uri
): Task(id,name,reward,status,requirement,type,pictureUri)