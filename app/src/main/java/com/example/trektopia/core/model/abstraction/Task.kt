package com.example.trektopia.core.model.abstraction

import android.net.Uri
import com.example.trektopia.core.model.enum.ProgressType

abstract class Task(
    open val id: String,
    open val name: String,
    open val reward: Int,
    open val status: Boolean,
    open val requirement: Double,
    open val type: ProgressType,
    open val pictureUri: Uri
)