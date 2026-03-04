package com.pixellauncherforall.home.receivers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pixellauncherforall.home.R

/**
 * Simple widget provider for the new Google widget.
 * It inflates the layout defined in `widget_google.xml` and updates the widget.
 */
class GoogleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_google)

            // Intent to open Google Search
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                setPackage("com.google.android.googlequicksearchbox")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val searchPendingIntent = PendingIntent.getActivity(
                context, 0, searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.google_search_bar_container, searchPendingIntent)

            // Intent to open Voice Search / Assistant
            val micIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                setPackage("com.google.android.googlequicksearchbox")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val micPendingIntent = PendingIntent.getActivity(
                context, 1, micIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.mice_logo, micPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
