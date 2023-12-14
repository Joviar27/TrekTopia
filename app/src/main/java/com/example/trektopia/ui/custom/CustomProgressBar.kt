package com.example.trektopia.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.trektopia.R

class CustomProgressBar : RelativeLayout {

    private lateinit var progressBackground: View
    private lateinit var progressBar: View
    private lateinit var progressText: TextView

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        inflate(context, R.layout.custom_task_progress, this)
        progressBackground = findViewById(R.id.custom_task_progress)
        progressBar = findViewById(R.id.task_percentage)
        progressText = findViewById(R.id.tv_task_progress)
    }

    fun setProgress(percentage: Double, current: Double, required: Double) {
        val layoutParams = progressBar.layoutParams as LayoutParams
        val fullWidth = progressBackground.layoutParams.width
        layoutParams.width = (fullWidth * percentage / 100.0).toInt()
        progressBar.layoutParams = layoutParams

        progressText.text = context.resources.getString(R.string.progress,current,required)
    }
}