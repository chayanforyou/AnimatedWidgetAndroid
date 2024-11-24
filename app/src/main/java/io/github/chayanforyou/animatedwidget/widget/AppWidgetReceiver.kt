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
import io.github.chayanforyou.animatedwidget.utils.ProgressBitmap

class AppWidgetReceiver : BroadcastReceiver(), Runnable {

    companion object {
        private const val POST_ANIMATION_DELAY = 800L
        const val ACTION_WIDGET_CLICK = "io.github.chayanforyou.animatedwidget.WIDGET_CLICK"

        private val TAG = AppWidgetReceiver::class.java.simpleName

        private val animationStates = mutableMapOf<Int, Boolean>()
    }

    // Context and app widget manager
    private var context: Context? = null
    private lateinit var appWidgetManager: AppWidgetManager

    // GIF decoder to process animations
    private val gifDecoder: GifDecoder by lazy { GifDecoder() }

    // Variables for tracking animation progress and widget state
    private var progress: Int = 0
    private var appWidgetId: Int = 0
    private var animationThread: Thread? = null

    // Bitmap objects for managing widget visuals
    private var tmpBitmap: Bitmap? = null
    private var progressBitmap: Bitmap? = null

    // Handler to post UI updates to the main thread
    private val myHandler = Handler(Looper.getMainLooper())

    /**
     * Runnable to update the widget image during the animation.
     */
    private val updateWidget = Runnable {
        tmpBitmap?.takeIf { !it.isRecycled }?.let {
            val remoteViews = RemoteViews(context?.packageName, R.layout.widget_layout).apply {
                setImageViewBitmap(R.id.widget_image, it) // Update image with animation frame
                setImageViewBitmap(R.id.progress, null)  // Clear progress bar
                setTextViewText(R.id.progress_pct, "0%") // Reset progress text
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    /**
     * Runnable to update the progress bar and percentage text.
     */
    private val updateProgress = Runnable {
        progressBitmap?.takeIf { !it.isRecycled }?.let {
            val views = RemoteViews(context?.packageName, R.layout.widget_layout).apply {
                setImageViewBitmap(R.id.progress, it) // Update progress bar
                setTextViewText(R.id.progress_pct, "$progress%") // Update progress percentage
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    /**
     * Handles widget click events and starts animation if not already animating.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        this.context = context

        appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0) ?: return

        when (intent.action) {
            ACTION_WIDGET_CLICK -> {
                // Check if animation is already running
                val isAnimating = animationStates[appWidgetId] ?: false
                if (!isAnimating) {
                    animationStates[appWidgetId] = true
                    startAnimationThread() // Start the animation
                }
            }
        }
    }

    /**
     * Starts a new thread to handle the animation.
     */
    private fun startAnimationThread() {
        if (animationThread == null) {
            animationThread = Thread(this).apply { start() }
        }
    }

    /**
     * Runs the animation logic in a background thread.
     */
    override fun run() {
        try {
            // Decode the GIF image
            gifDecoder.read(context?.resources?.openRawResource(R.raw.ic_widget))

            // Loop through the GIF frames
            for (frameIndex in 0 until gifDecoder.frameCount) {
                gifDecoder.advance() // Move to the next frame
                val startTime = System.nanoTime()

                try {
                    tmpBitmap = gifDecoder.nextFrame
                    val decodeTime = (System.nanoTime() - startTime) / 1_000_000
                    myHandler.post(updateWidget)

                    // Adjust delay for smooth animation
                    val delay = gifDecoder.nextDelay.toLong() - decodeTime
                    if (delay > 0) Thread.sleep(delay)
                } catch (e: Exception) {
                    Log.w(TAG, "Error during frame processing", e)
                }
            }

            // Add post-animation delay
            Thread.sleep(POST_ANIMATION_DELAY)

            // Increment progress and update the progress bar
            while (progress < AppWidgetProviderInfo.MAX_PROGRESS) {
                progress++
                progressBitmap = ProgressBitmap.create(progress)
                myHandler.post(updateProgress)
            }
        } catch (e: InterruptedException) {
            Log.w(TAG, "Animation interrupted", e)
        } finally {
            // Reset animation state
            animationStates[appWidgetId] = false
            animationThread = null
        }
    }
}

