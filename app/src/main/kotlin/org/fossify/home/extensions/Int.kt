package org.fossify.home.extensions

import android.graphics.Color
import androidx.core.graphics.ColorUtils

/**
 * Adds a black tint to the current color.
 * @param percentage The percentage of black tint (0-100). Default is 10%.
 */
fun Int.addBlackTint(percentage: Int = 10): Int {
    val alpha = (percentage * 255) / 100
    val blackTint = Color.argb(alpha, 0, 0, 0)
    return ColorUtils.compositeColors(blackTint, this)
}
