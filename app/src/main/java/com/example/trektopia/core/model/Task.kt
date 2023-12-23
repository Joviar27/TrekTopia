package com.example.trektopia.core.model

import android.net.Uri
import com.example.trektopia.core.model.enum.ProgressType

data class Task(
    val id: String = "",
    val name: String = "",
    val reward: Int = 0,
    val requirement: Double = 0.0,
    val type: String = "",
    val pictureUri: String = ""
)