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

        for (appWidgetId in allWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val thisWidget = ComponentName(context, AppWidgetProviderInfo::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        val intent = Intent(context, AppWidgetReceiver::class.java)
        intent.setAction(AppWidgetReceiver.WIDGET_CLICK)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        views.setOnClickPendingIntent(R.id.widget, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

