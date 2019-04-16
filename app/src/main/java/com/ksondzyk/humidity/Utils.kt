package com.ksondzyk.humidity

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

fun Float.toPx(): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
)

fun Context.getColorCompat(@ColorRes colorRes: Int) =
        ContextCompat.getColor(this, colorRes)

fun Context.getDrawableCompat(@DrawableRes drawableRes: Int) =
        ContextCompat.getDrawable(this, drawableRes)

@ColorInt
fun Int.adjustAlpha(factor: Float): Int {
    val alpha = Math.round(Color.alpha(this) * factor)
    val red = Color.red(this)
    val green = Color.green(this)
    val blue = Color.blue(this)
    return Color.argb(alpha, red, green, blue)
}

fun intermediateColor(a: Int, b: Int, t: Float): Int {
    return Color.rgb(
            Color.red(a) + ((Color.red(b) - Color.red(a)) * t).toInt(),
            Color.green(a) + ((Color.green(b) - Color.green(a)) * t).toInt(),
            Color.blue(a) + ((Color.blue(b) - Color.blue(a)) * t).toInt()
    )
}
