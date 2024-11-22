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

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val thisWidget = ComponentName(context, AppWidgetProviderInfo::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        allWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // Set the click action for the widget
       val onClickPendingIntent = createPendingIntent(context, appWidgetId,  AppWidgetReceiver.WIDGET_CLICK)

        // Send the new widget action
        val onCreatePendingIntent = createPendingIntent(context, appWidgetId,  AppWidgetReceiver.NEW_WIDGET)
        onCreatePendingIntent.send()

        val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
            setOnClickPendingIntent(R.id.widget, onClickPendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
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

