package io.github.chayanforyou.animatedwidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.RemoteViews
import kotlin.concurrent.Volatile


class AppWidgetReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = AppWidgetReceiver::class.java.simpleName
        const val WIDGET_CLICK = "io.github.chayanforyou.animatedwidget.APPWIDGET_CLICK"

        @Volatile
        var isClicked: Boolean = false
    }

    private lateinit var context: Context
    private lateinit var appWidgetManager: AppWidgetManager
    private var gifDecoder = GifDecoder()

    @Volatile
    private var widgetIds: IntArray? = IntArray(0)

    private val widgetLock = Any()

    override fun onReceive(context: Context, intent: Intent) {
        this.context = context

        // AppWidgetManager instant
        appWidgetManager = AppWidgetManager.getInstance(context)

        // Get widget ids
        widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)

        // GifDecoder instant
        gifDecoder.read(context.resources.openRawResource(R.raw.ic_widget))
        gifDecoder.complete()

        if (intent.action == WIDGET_CLICK) {
            if (!isClicked) {
                Thread(animationThread).start()
                isClicked = true
            }
        }
    }

    private val animationThread = Runnable {
        var frame = 0
        val frameCount = gifDecoder.frameCount
        val bitmaps = arrayOfNulls<Bitmap>(frameCount)
        val delays = IntArray(frameCount)

        synchronized(widgetLock) {
            for (i in 0 until frameCount) {
                bitmaps[i] = gifDecoder.getFrame(i)
                delays[i] = gifDecoder.getDelay(i)
                // Log.d(TAG, "===>Frame " + i + ": [" + delays[i] + "]")
            }
            val remoteViews = arrayOfNulls<RemoteViews>(bitmaps.size)
            for (i in remoteViews.indices) {
                remoteViews[i] = RemoteViews(context.packageName, R.layout.widget_layout)
                remoteViews[i]!!.setImageViewBitmap(R.id.icon, bitmaps[i])
            }
            while (frame < frameCount) {
                appWidgetManager.updateAppWidget(widgetIds, remoteViews[frame])
                try {
                    Thread.sleep(delays[frame].toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                frame++
            }
        }
        isClicked = false
    }
}

