package com.pixellauncherforall.home.interfaces

import com.pixellauncherforall.home.models.AppLauncher

interface AllAppsListener {
    fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher)
}
