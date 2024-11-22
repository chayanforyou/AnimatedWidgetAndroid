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

        @Volatile
        var isAnimating: Boolean = false
        private var gifDecoder: GifDecoder? = null
    }

    private var context: Context? = null
    private lateinit var appWidgetManager: AppWidgetManager

    private var tmpBitmap: Bitmap? = null
    private var progressBitmap: Bitmap? = null
    private var appWidgetId: Int = 0
    private var animationThread: Thread? = null
    private val myHandler = Handler(Looper.getMainLooper())

    private val canvasSize = 400
    private val canvasPadding = 20
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

        if (intent.action == NEW_WIDGET) {
            progress = maxProgress
            progressBitmap = getProgressBitmap(progress)
            myHandler.post(updateProgress)
        }

        if (intent.action == WIDGET_CLICK) {
            gifDecoder = gifDecoder ?: GifDecoder().apply {
                read(context?.resources?.openRawResource(R.raw.ic_widget))
            }

            synchronized(AppWidgetReceiver::class.java) {
                if (!isAnimating) {
                    isAnimating = true
                    startAnimationThread()
                }
            }
        }
    }

    private fun canStart(): Boolean {
        return isAnimating && gifDecoder != null && animationThread == null
    }

    private fun startAnimationThread() {
        if (canStart()) {
            animationThread = Thread(this)
            animationThread!!.start()
        }
    }

    override fun run() {
        for (pos in 0 until gifDecoder!!.frameCount) {
            gifDecoder!!.advance()

            var frameDecodeTime: Long = 0
            try {
                val before = System.nanoTime()
                tmpBitmap = gifDecoder!!.nextFrame
                frameDecodeTime = (System.nanoTime() - before) / 1000000
                myHandler.post(updateWidget)
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.w(TAG, e)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, e)
            }

            try {
                var delay = gifDecoder!!.nextDelay.toLong()
                delay -= frameDecodeTime
                if (delay > 0) {
                    Thread.sleep(delay)
                }
            } catch (e: InterruptedException) {
                // suppress exception
            }
        }

        // Increment the progress and update bitmap in the RemoteViews
        while (progress < maxProgress) {
            progress += 1
            progressBitmap = getProgressBitmap(progress)
            myHandler.post(updateProgress)
        }

        isAnimating = false
        animationThread = null
    }

    private fun getProgressBitmap(progress: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true

        paint.color = Color.argb(45, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = canvasPadding.toFloat()
        canvas.drawCircle(
            canvasSize / 2f,
            canvasSize / 2f,
            canvasSize / 2f - canvasPadding / 2f,
            paint
        )

        paint.color = Color.argb(255, 255, 194, 10)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = canvasPadding.toFloat()
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

