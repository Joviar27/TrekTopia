package com.example.trektopia.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.trektopia.R

fun Char.createCustomDrawable(context: Context): Drawable {
    val mBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val mCanvas = Canvas(mBitmap)
    val titlePaint = Paint(Paint.FAKE_BOLD_TEXT_FLAG)

    mCanvas.drawColor(ContextCompat.getColor(context, R.color.secondary))
    titlePaint.apply {
        textSize = 50f
        color = ContextCompat.getColor(context, R.color.white)
        textAlign = Paint.Align.CENTER
    }

    val x = mCanvas.width / 2f
    val y = (mCanvas.height / 2f) - ((titlePaint.descent() + titlePaint.ascent()) / 2f)

    mCanvas.drawText(this.toString(), x, y, titlePaint)

    return BitmapDrawable(context.resources, mBitmap)
}