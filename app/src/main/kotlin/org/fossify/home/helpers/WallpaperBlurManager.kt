package org.fossify.home.helpers

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
            
            // Allow content to draw behind system bars to ensure the blur covers the whole screen
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            
            // Ensure hardware acceleration is enabled
            addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            
            // Allow drawing behind system bars
            WindowCompat.setDecorFitsSystemWindows(this, false)
            
            // Set window background to transparent to allow wallpaper to be visible
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Use translucent format to ensure blur effects can be seen
            setFormat(PixelFormat.TRANSLUCENT)
        }
    }

    /**
     * Applies the blur. MUST be called on the main thread synchronously.
     */
    fun setBlurRadius(activity: Activity, radius: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        activity.window.apply {
            try {
                val params = attributes
                params.blurBehindRadius = radius
                
                // Set the blur behind flag on the params object
                if (radius > 0) {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                } else {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                }
                
                // Update the window attributes to apply both radius and flags synchronously
                attributes = params
                
                // Also blurs the area behind the window background drawable
                setBackgroundBlurRadius(radius)
                
                // Force the compositor to pick up the change immediately
                decorView.invalidate()
            } catch (e: Exception) {
                // Ignore failures
            }
        }
    }
}
