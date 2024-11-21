package io.github.chayanforyou.animatedwidget.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import io.github.chayanforyou.animatedwidget.R
import io.github.chayanforyou.animatedwidget.gif.GifDecoder

class AppWidgetReceiver : BroadcastReceiver(), Runnable {

    companion object {
        private val TAG = AppWidgetReceiver::class.java.simpleName
        const val WIDGET_CLICK = "io.github.chayanforyou.animatedwidget.WIDGET_CLICK"

        @Volatile
        private var isAnimating: Boolean = false
        private var gifDecoder: GifDecoder? = null
    }

    private var context: Context? = null
    private lateinit var appWidgetManager: AppWidgetManager

    private var tmpBitmap: Bitmap? = null
    private var widgetIds: IntArray? = IntArray(0)
    private var animationThread: Thread? = null
    private val myHandler = Handler(Looper.getMainLooper())

    private val updateResults = Runnable {
        if (tmpBitmap != null && !tmpBitmap!!.isRecycled) {
            val remoteViews = RemoteViews(context?.packageName, R.layout.widget_layout)
            remoteViews.setImageViewBitmap(R.id.icon, tmpBitmap)
            appWidgetManager.updateAppWidget(widgetIds, remoteViews)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        this.context = context

        appWidgetManager = AppWidgetManager.getInstance(context)
        widgetIds = intent?.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)

        gifDecoder = gifDecoder ?: GifDecoder().apply {
            read(context?.resources?.openRawResource(R.raw.ic_widget), 4096)
        }

        if (intent?.action == WIDGET_CLICK) {
            if (!isAnimating) {
                isAnimating = true
                startAnimationThread()
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
        synchronized(AppWidgetReceiver::class.java) {
            for (pos in 0 until gifDecoder!!.frameCount) {
                gifDecoder!!.advance()

                var frameDecodeTime: Long = 0
                try {
                    val before = System.nanoTime()
                    tmpBitmap = gifDecoder!!.nextFrame
                    frameDecodeTime = (System.nanoTime() - before) / 1000000
                    myHandler.post(updateResults)
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
        }
        isAnimating = false
        animationThread = null
    }
}

