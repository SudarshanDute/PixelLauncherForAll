package org.fossify.home.receivers

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import org.fossify.home.R
import org.fossify.home.extensions.addBlackTint


/**
 * Simple widget provider for the new Google widget.
 * It inflates the layout defined in `widget_google.xml` and updates the widget.
 */
class GoogleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Update each instance of the widget with the remote view.
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_google)
            
            val bgColor = ContextCompat.getColor(context, R.color.google_widget_bg)
            val tintedColor = bgColor.addBlackTint(15)
            views.setInt(R.id.google_widget_bg_view, "setColorFilter", tintedColor)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
