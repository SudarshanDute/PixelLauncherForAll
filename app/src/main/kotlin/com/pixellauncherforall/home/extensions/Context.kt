package com.pixellauncherforall.home.extensions

import android.app.role.RoleManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.Size
import androidx.annotation.RequiresApi
import org.fossify.commons.helpers.isQPlus
import org.fossify.commons.helpers.isSPlus
import com.pixellauncherforall.home.databases.AppsDatabase
import com.pixellauncherforall.home.helpers.Config
import com.pixellauncherforall.home.helpers.IconPackHelper
import com.pixellauncherforall.home.interfaces.AppLaunchersDao
import com.pixellauncherforall.home.interfaces.HiddenIconsDao
import com.pixellauncherforall.home.interfaces.HomeScreenGridItemsDao
import kotlin.math.ceil
import kotlin.math.max

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.launchersDB: AppLaunchersDao
    get() = AppsDatabase.getInstance(applicationContext).AppLaunchersDao()

val Context.homeScreenGridItemsDB: HomeScreenGridItemsDao
    get() = AppsDatabase.getInstance(
        applicationContext
    ).HomeScreenGridItemsDao()

val Context.hiddenIconsDB: HiddenIconsDao
    get() = AppsDatabase.getInstance(applicationContext).HiddenIconsDao()

@get:RequiresApi(Build.VERSION_CODES.Q)
val Context.roleManager: RoleManager
    get() = getSystemService(RoleManager::class.java)

private var iconPackHelper: IconPackHelper? = null
private var cachedIconPack = ""

fun Context.getDrawableForPackageName(packageName: String, activityName: String = ""): Drawable? {
    var resolvedActivityName = activityName
    if (resolvedActivityName.isEmpty()) {
        try {
            val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val activityList = launcher.getActivityList(packageName, Process.myUserHandle())
            if (activityList.isNotEmpty()) {
                resolvedActivityName = activityList[0].componentName.className
            }
        } catch (ignored: Exception) {
        }
    }

    val iconPack = config.iconPack
    if (iconPack.isNotEmpty()) {
        if (iconPackHelper == null || cachedIconPack != iconPack) {
            iconPackHelper = IconPackHelper(this, iconPack)
            cachedIconPack = iconPack
        }
        
        val icon = iconPackHelper?.getIcon(packageName, resolvedActivityName)
        if (icon != null) {
            return icon
        }
    } else {
        iconPackHelper = null
        cachedIconPack = ""
    }

    var drawable: Drawable? = null
    try {
        // try getting the properly colored launcher icons
        val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityList = launcher.getActivityList(packageName, Process.myUserHandle())
        if (activityList.isNotEmpty()) {
            val activity = if (resolvedActivityName.isNotEmpty()) {
                activityList.firstOrNull { it.componentName.className == resolvedActivityName } ?: activityList[0]
            } else {
                activityList[0]
            }
            drawable = activity.getBadgedIcon(0)
        }
    } catch (e: Exception) {
    } catch (e: Error) {
    }

    if (drawable == null) {
        drawable = try {
            packageManager.getApplicationIcon(packageName)
        } catch (ignored: Exception) {
            null
        }
    }

    return drawable
}

fun Context.getInitialCellSize(
    info: AppWidgetProviderInfo,
    fallbackWidth: Int,
    fallbackHeight: Int
): Size {
    return if (isSPlus() && info.targetCellWidth != 0 && info.targetCellHeight != 0) {
        Size(info.targetCellWidth, info.targetCellHeight)
    } else {
        val widthCells = getCellCount(fallbackWidth)
        val heightCells = getCellCount(fallbackHeight)
        Size(widthCells, heightCells)
    }
}

fun Context.getCellCount(size: Int): Int {
    val tiles = ceil(((size / resources.displayMetrics.density) - 30) / 70.0).toInt()
    return max(tiles, 1)
}

fun Context.isDefaultLauncher(): Boolean {
    return if (isQPlus()) {
        with(roleManager) {
            isRoleAvailable(RoleManager.ROLE_HOME) && isRoleHeld(RoleManager.ROLE_HOME)
        }
    } else {
        val filters = ArrayList<IntentFilter>()
        val activities = ArrayList<ComponentName>()
        @Suppress("DEPRECATION")
        packageManager.getPreferredActivities(filters, activities, null)
        return activities.indices.any { i ->
            activities[i].packageName == packageName &&
                    filters[i].hasAction(Intent.ACTION_MAIN) &&
                    filters[i].hasCategory(Intent.CATEGORY_HOME)
        }
    }
}
