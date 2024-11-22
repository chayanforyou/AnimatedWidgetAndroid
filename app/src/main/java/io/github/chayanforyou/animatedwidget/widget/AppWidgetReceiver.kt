package io.github.chayanforyou.animatedwidget.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import io.github.chayanforyou.animatedwidget.R
import io.github.chayanforyou.animatedwidget.gif.GifDecoder

class AppWidgetReceiver : BroadcastReceiver(), Runnable {

    companion object {
        private val TAG = AppWidgetReceiver::class.java.simpleName
        const val NEW_WIDGET = "io.github.chayanforyou.animatedwidget.NEW_WIDGET"
        const val WIDGET_CLICK = "io.github.chayanforyou.animatedwidget.WIDGET_CLICK"

        private val animationStates = mutableMapOf<Int, Boolean>()
    }

    private var context: Context? = null
    private lateinit var appWidgetManager: AppWidgetManager
    private val gifDecoder: GifDecoder by lazy {
        GifDecoder()
    }

    private var animationThread: Thread? = null
    private var tmpBitmap: Bitmap? = null
    private var progressBitmap: Bitmap? = null
    private var appWidgetId: Int = 0
    private val myHandler = Handler(Looper.getMainLooper())

    private val canvasSize = 400
    private val canvasPadding = 20f
    private var progress = 0
    private val maxProgress = 56   // 56%

    private val updateWidget = Runnable {
        if (tmpBitmap != null && !tmpBitmap!!.isRecycled) {
            val remoteViews = RemoteViews(context?.packageName, R.layout.widget_layout).apply {
                setImageViewBitmap(R.id.widget_image, tmpBitmap)
                setImageViewBitmap(R.id.progress, null)
                setTextViewText(R.id.progress_pct, "0%")
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private val updateProgress: Runnable = Runnable {
        if (progressBitmap != null && !progressBitmap!!.isRecycled) {
            val views = RemoteViews(context?.packageName, R.layout.widget_layout).apply {
                setImageViewBitmap(R.id.progress, progressBitmap)
                setTextViewText(R.id.progress_pct, "$progress%")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        this.context = context

        appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0) ?: return

        when (intent.action) {
            NEW_WIDGET -> {
                animationStates[appWidgetId] = false
                progress = maxProgress
                progressBitmap = getProgressBitmap(progress)
                myHandler.post(updateProgress)
            }

            WIDGET_CLICK -> {
                val isAnimating = animationStates[appWidgetId] ?: false
                if (!isAnimating) {
                    animationStates[appWidgetId] = true
                    startAnimationThread()
                }
            }
        }
    }

    private fun startAnimationThread() {
        if (animationThread == null) {
            animationThread = Thread(this)
            animationThread!!.start()
        }
    }

    override fun run() {
        // Read GIF image from stream
        gifDecoder.read(context?.resources?.openRawResource(R.raw.ic_widget))

        for (pos in 0 until gifDecoder.frameCount) {
            gifDecoder.advance()

            var frameDecodeTime: Long = 0
            try {
                val before = System.nanoTime()
                tmpBitmap = gifDecoder.nextFrame
                frameDecodeTime = (System.nanoTime() - before) / 1000000
                myHandler.post(updateWidget)
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.w(TAG, e)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, e)
            }

            try {
                var delay = gifDecoder.nextDelay.toLong()
                delay -= frameDecodeTime
                if (delay > 0) Thread.sleep(delay)
            } catch (e: InterruptedException) {
                // suppress exception
            }
        }

        try {
            Thread.sleep(800L)
        } catch (e: InterruptedException) {
            // suppress exception
        }

        // Increment the progress and update he RemoteViews
        while (progress < maxProgress) {
            progress += 1
            progressBitmap = getProgressBitmap(progress)
            myHandler.post(updateProgress)
        }

        animationThread = null
        animationStates[appWidgetId] = false
    }

    private fun getProgressBitmap(progress: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = canvasPadding
        }

        paint.color = Color.argb(45, 255, 255, 255)
        canvas.drawCircle(
            canvasSize / 2f,
            canvasSize / 2f,
            canvasSize / 2f - canvasPadding / 2f,
            paint
        )

        paint.color = Color.argb(255, 255, 194, 10)
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(
            RectF(
                canvasPadding / 2f,
                canvasPadding / 2f,
                (canvasSize - canvasPadding / 2f),
                (canvasSize - canvasPadding / 2f)
            ),
            -90f,
            360 * (progress / 100f),
            false,
            paint
        )
        return bitmap
    }
}

