package com.pixellauncherforall.home.helpers

import android.app.Activity
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat

object WallpaperBlurManager {

    fun prepareWindow(activity: Activity) {
        activity.window.apply {
            // Ensure the wallpaper is shown behind the activity
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            
            // On Android 12+, explicitly enable the blur behind flag early to prevent flickering
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                
                // Warm-up call: some OEMs (like OnePlus) need an initial radius update 
                // to correctly initialize the window blur compositor layer.
                setBlurRadius(activity, 0)
            }
            
            // Allow drawing behind system bars
            WindowCompat.setDecorFitsSystemWindows(this, false)
            
            // Set window background to transparent to allow wallpaper to be visible
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Use translucent format to ensure blur effects can be seen.
            // On some devices, this MUST be set for FLAG_BLUR_BEHIND to trigger.
            setFormat(PixelFormat.TRANSLUCENT)
        }
    }

    /**
     * Applies the blur. MUST be called on the main thread synchronously.
     */
    fun setBlurRadius(activity: Activity, radius: Int) {
        // Fallback for versions before Android 12 (API 31) is handled via background alpha in fragments
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        activity.window.apply {
            try {
                /**
                 * NOTE: We are bypassing the explicit isCrossWindowBlurEnabled check here because
                 * on some OEM devices (like OnePlus/OxygenOS), this can return false even when
                 * blurs are supported/working, or during state transitions.
                 * We wrap in try-catch to avoid crashes on unsupported hardware.
                 */
                
                val params = attributes
                params.blurBehindRadius = radius
                
                // Some OEMs require setBackgroundBlurRadius specifically
                setBackgroundBlurRadius(radius)
                
                // Directly manipulate flags and attributes for better consistency across versions
                if (radius > 0) {
                    addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    if (activity.windowManager.isCrossWindowBlurEnabled) {
                        // Voodoo Fix: Add a tiny amount of dimming. On some OEMs (OnePlus/Oppo), 
                        // this forces the window manager to create the necessary blur layers.
                        params.dimAmount = 0.01f
                    } else {
                        // Fallback: If blur is disabled (e.g. OnePlus OxygenOS 15 bug or battery saver),
                        // use dimming based on the requested radius to keep the background legible.
                        // We scale the dim amount so it transitions smoothly with the radius animation.
                        params.dimAmount = ((radius / 80f) * 0.5f).coerceIn(0f, 0.8f)
                    }
                    
                    // Voodoo Fix: Setting alpha slightly below 1.0 can trigger transparency 
                    // compositions that enable blur on problematic hardware.
                    params.alpha = 0.99f
                    
                    // Ensure the window is translucent
                    params.format = android.graphics.PixelFormat.TRANSLUCENT
                } else {
                    clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    params.dimAmount = 0f
                    params.alpha = 1.0f
                }
                
                attributes = params
                
                // Critical: force the compositor to pick up changes immediately
                decorView.invalidate()
            } catch (e: Exception) {
                // Ignore failures
            }
        }
    }
}
