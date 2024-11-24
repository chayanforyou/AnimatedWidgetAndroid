package io.github.chayanforyou.animatedwidget.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object ProgressBitmap {

    private const val CANVAS_SIZE = 400
    private const val STROKE_WIDTH = 20f

    fun create(progress: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = STROKE_WIDTH
        }

        paint.color = Color.argb(45, 255, 255, 255)
        canvas.drawCircle(
            CANVAS_SIZE / 2f,
            CANVAS_SIZE / 2f,
            CANVAS_SIZE / 2f - STROKE_WIDTH / 2f,
            paint
        )

        paint.color = Color.argb(255, 255, 194, 10)
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(
            RectF(
                STROKE_WIDTH / 2f,
                STROKE_WIDTH / 2f,
                (CANVAS_SIZE - STROKE_WIDTH / 2f),
                (CANVAS_SIZE - STROKE_WIDTH / 2f)
            ),
            -90f,
            360 * (progress / 100f),
            false,
            paint
        )
        return bitmap
    }
}