package io.github.chayanforyou.animatedwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import io.github.chayanforyou.animatedwidget.R
import io.github.chayanforyou.animatedwidget.utils.ProgressBitmap

class AppWidgetProviderInfo : AppWidgetProvider() {

    companion object {
        const val MAX_PROGRESS = 56 // 56%
    }

    /**
     * Called when the first widget is added or reboot the system.
     * Sets up a one-time alarm to immediately trigger a widget update.
     */
    override fun onEnabled(context: Context?) {
        super.onEnabled(context)

        context?.let {
            // Get the widget manager and fetch all widget IDs
            val appWidgetManager = AppWidgetManager.getInstance(it)
            val thisWidget = ComponentName(it, AppWidgetProviderInfo::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            // Prepare an intent to update all widgets
            val intent = Intent(it, AppWidgetProviderInfo::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }

            // Create a PendingIntent to broadcast the update action
            val pendingIntent = PendingIntent.getBroadcast(
                it,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use AlarmManager to schedule the widget update immediately
            val alarmManager = it.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                pendingIntent
            )
        }
    }

    /**
     * Called when widgets are added, updated, or when the user manually refreshes them.
     * Updates all instances of the widget.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Get all widget IDs for the current provider
        val thisWidget = ComponentName(context, AppWidgetProviderInfo::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        // Update each widget instance
        allWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Updates the widget UI for a specific widget ID.
     */
    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Prepare the intent for handling widget click events
        val intent = Intent(context, AppWidgetReceiver::class.java).apply {
            action = AppWidgetReceiver.ACTION_WIDGET_CLICK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        // Create a PendingIntent for the click action
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Generate the progress bitmap for the widget
        val bitmap = ProgressBitmap.create(MAX_PROGRESS)

        // Configure the RemoteViews for the widget
        val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
            setImageViewBitmap(R.id.progress, bitmap)
            setTextViewText(R.id.progress_pct, "$MAX_PROGRESS%")
            setOnClickPendingIntent(R.id.widget, pendingIntent)
        }

        // Update the widget instance with the new layout
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

