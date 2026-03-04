package com.pixellauncherforall.home.extensions

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import org.fossify.commons.extensions.applyColorFilter
import com.pixellauncherforall.home.R

fun View.animateScale(
    from: Float,
    to: Float,
    duration: Long,
) = animate()
    .scaleX(to)
    .scaleY(to)
    .setDuration(duration)
    .setInterpolator(AccelerateDecelerateInterpolator())
    .withStartAction {
        scaleX = from
        scaleY = from
    }

fun View.setupDrawerBackground() {
    // Force the view itself to be fully opaque
    this.alpha = 1f
    
    /**
     * Use a semi-transparent background for all versions. 
     * On Android 12+ (API 31+), this allows the wallpaper blur behind the window to be visible.
     * On older versions, it provides a nice translucent fallback effect.
     */
    val backgroundColor = Color.argb(153, 0, 0, 0) // ~60% opaque black

    val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.drawer_background, context.theme)
    drawable?.applyColorFilter(backgroundColor)
    background = drawable

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        v.updatePadding(top = context.resources.getDimensionPixelSize(org.fossify.commons.R.dimen.medium_margin))
        insets
    }
}
