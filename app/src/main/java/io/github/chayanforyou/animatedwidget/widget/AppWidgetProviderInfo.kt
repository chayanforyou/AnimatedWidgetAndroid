package io.github.chayanforyou.animatedwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.chayanforyou.animatedwidget.R

class AppWidgetProviderInfo : AppWidgetProvider() {

    companion object {
        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_WIDGET_IDS = "existing_widget_ids"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingWidgetIds = sharedPrefs.getStringSet(KEY_WIDGET_IDS, emptySet())
            ?.map { it.toInt() }?.toMutableSet() ?: mutableSetOf()

        val thisWidget = ComponentName(context, AppWidgetProviderInfo::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        allWidgetIds.forEach { appWidgetId ->
            if (!existingWidgetIds.contains(appWidgetId)) {
                onNewWidgetCreated(context, appWidgetId)
                existingWidgetIds.add(appWidgetId)
            }
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        sharedPrefs.edit().putStringSet(KEY_WIDGET_IDS, existingWidgetIds.map { it.toString() }.toSet()).apply()
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
       val onClickPendingIntent = createPendingIntent(context, appWidgetId,  AppWidgetReceiver.WIDGET_CLICK)

        val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
            setOnClickPendingIntent(R.id.widget, onClickPendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun onNewWidgetCreated(context: Context, appWidgetId: Int) {
        val onCreatePendingIntent = createPendingIntent(context, appWidgetId,  AppWidgetReceiver.NEW_WIDGET)
        onCreatePendingIntent.send()
    }

    private fun createPendingIntent(context: Context, appWidgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, AppWidgetReceiver::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

