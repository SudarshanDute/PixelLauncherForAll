package com.pixellauncherforall.home.helpers

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.pixellauncherforall.home.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class IconPackHelper(private val context: Context, private val iconPackPackageName: String) {
    private val iconPackResources: Resources? = try {
        context.packageManager.getResourcesForApplication(iconPackPackageName)
    } catch (e: Exception) {
        null
    }

    private val iconMap = mutableMapOf<String, String>()
    private val resIdCache = ConcurrentHashMap<String, Int>()
    
    // Cache for bitmaps to avoid repeated rendering and high memory usage.
    // 200 items is usually enough for most users' app drawers.
    private val bitmapCache = object : LruCache<String, Bitmap>(200) {}

    private val maxIconSize: Int by lazy {
        (context.resources.getDimensionPixelSize(R.dimen.launcher_icon_size) * 1.5f).toInt()
    }

    init {
        loadAppFilter()
    }

    private fun loadAppFilter() {
        if (iconPackResources == null) return

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            val assets = context.packageManager.getResourcesForApplication(iconPackPackageName).assets
            
            // Common filenames for appfilter
            val filenames = listOf("appfilter.xml", "app_filter.xml")
            var inputStream = filenames.firstNotNullOfOrNull { filename ->
                try { assets.open(filename) } catch (e: Exception) { null }
            }

            if (inputStream == null) {
                // Try to find it in xml resources if not in assets
                val resId = iconPackResources.getIdentifier("appfilter", "xml", iconPackPackageName)
                if (resId != 0) {
                    val xmlParser = iconPackResources.getXml(resId)
                    parseXml(xmlParser)
                    return
                }
            }

            inputStream?.use {
                parser.setInput(it, "UTF-8")
                parseXml(parser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseXml(parser: XmlPullParser) {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null) {
                    iconMap[component] = drawable
                }
            }
            eventType = parser.next()
        }
    }

    fun getIcon(packageName: String, activityName: String): Drawable? {
        if (iconPackResources == null) return null

        val cacheKey = "$packageName/$activityName"
        bitmapCache.get(cacheKey)?.let { return BitmapDrawable(context.resources, it) }

        val componentName = "ComponentInfo{$packageName/$activityName}"
        val drawableName = iconMap[componentName] ?: iconMap["ComponentInfo{$packageName}"]
        
        var icon: Drawable? = null
        if (drawableName != null) {
            val resId = resIdCache.getOrPut(drawableName) {
                iconPackResources.getIdentifier(drawableName, "drawable", iconPackPackageName)
            }
            if (resId != 0) {
                icon = try {
                    iconPackResources.getDrawable(resId, null)
                } catch (e: Exception) {
                    null
                }
            }
        }

        if (icon == null) {
            // Fallback to basic mapping if not found in appfilter
            val fallbackName = packageName.replace(".", "_")
            val fallbackId = resIdCache.getOrPut(fallbackName) {
                iconPackResources.getIdentifier(fallbackName, "drawable", iconPackPackageName)
            }
            if (fallbackId != 0) {
                icon = try {
                    iconPackResources.getDrawable(fallbackId, null)
                } catch (e: Exception) {
                    null
                }
            }
        }

        if (icon != null) {
            val width = if (icon.intrinsicWidth > 0) min(icon.intrinsicWidth, maxIconSize) else maxIconSize
            val height = if (icon.intrinsicHeight > 0) min(icon.intrinsicHeight, maxIconSize) else maxIconSize
            
            try {
                val bitmap = icon.toBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmapCache.put(cacheKey, bitmap)
                return BitmapDrawable(context.resources, bitmap)
            } catch (e: Exception) {
                // Fallback for huge icons or memory issues
                return icon
            }
        }

        return null
    }
}
