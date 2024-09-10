package com.example.livebroadcast.utility

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.livebroadcast.R


// 未能实现的功能，。。
object PartialMirroringPauseImageTool {

    fun generatePartialMirroringPauseImage(
        context: Context,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            //
        }
        return bitmap
    }
}