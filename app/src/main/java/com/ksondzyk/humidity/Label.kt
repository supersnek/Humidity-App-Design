package com.ksondzyk.humidity

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.log10
import kotlin.math.roundToInt

private const val KEY_TEXT_SIZE = "KEY_TEXT_SIZE"
private const val KEY_TEXT_Y = "KEY_TEXT_Y"
private const val KEY_TEXT_COLOR = "KEY_TEXT_COLOR"

class Label(
    private val realY: Float,
    private val selectedColor: Int,
    private val color: Int,
    private val selectedTextSize: Float,
    private val textSize: Float,
    private var progressData: ProgressData,
    private val suffix: String = "",
    private val view: CurveSeekView
) {

    private val textBounds: Rect = Rect()
    var currentY: Float = realY
        set(value) {
            field = value
            onYChange()
        }

    val text: CharArray = CharArray(12) { '0' }
    var lastCharIndex: Int = 0
        private set

    val paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        isAntiAlias = true
        color = this@Label.color
        textSize = this@Label.textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var animator: ValueAnimator? = null
    private var duration: Long = 400
    private val interpolator = AccelerateDecelerateInterpolator()
    private var inFocus = false
    var progress: Float = 0.0F
        private set

    init {
        currentY = realY
    }

    private fun onYChange() {
        if (!inFocus) {
            progressData.apply {
                getTextBounds(textBounds)
                progress = ((currentY - minY) / (maxY - minY)) *
                        (maxProgress - minProgress) + minProgress
                progress = progress.roundToInt().toFloat()
            }
        } else {
            progress = view.getProgress().roundToInt().toFloat()
        }

        lastCharIndex = (log10(progress.toDouble()) + 1).toInt()

        for (i in (lastCharIndex - 1) downTo 0) {
            text[i] = '0' + progress.toInt() % 10
            progress /= 10
        }

        suffix.forEachIndexed { index, c ->
            text[lastCharIndex + index] = c
        }

        lastCharIndex += suffix.length
    }

    fun inFocus(view: View) {
        inFocus = true
        animator?.cancel()
        animator = ValueAnimator.ofPropertyValuesHolder(
            PropertyValuesHolder.ofFloat(KEY_TEXT_SIZE, textSize, selectedTextSize),
            PropertyValuesHolder.ofFloat(KEY_TEXT_COLOR, 0.0F, 1.0F)
        )
        animator?.apply {
            duration = this@Label.duration
            interpolator = this@Label.interpolator
            addUpdateListener { animation ->
                paint.textSize = animation.getAnimatedValue(KEY_TEXT_SIZE) as Float
                paint.color = intermediateColor(
                    color,
                    selectedColor, animation.getAnimatedValue(KEY_TEXT_COLOR) as Float
                )
                view.invalidate()
            }
            start()
        }
    }

    fun outFocus(view: View) {
        inFocus = false
        animator?.cancel()
        animator = ValueAnimator.ofPropertyValuesHolder(
            PropertyValuesHolder.ofFloat(KEY_TEXT_SIZE, selectedTextSize, textSize),
            PropertyValuesHolder.ofFloat(KEY_TEXT_Y, currentY, realY),
            PropertyValuesHolder.ofFloat(KEY_TEXT_COLOR, 0.0F, 1.0F)
        )
        animator?.apply {
            duration = this@Label.duration
            interpolator = this@Label.interpolator
            addUpdateListener { animation ->
                paint.color = intermediateColor(
                    selectedColor,
                    color, animation.getAnimatedValue(KEY_TEXT_COLOR) as Float
                )
                currentY = animation.getAnimatedValue(KEY_TEXT_Y) as Float
                paint.textSize = animation.getAnimatedValue(KEY_TEXT_SIZE) as Float
                view.invalidate()
            }
            start()
        }
    }

    fun getTextBounds(bounds: Rect) {
        paint.getTextBounds(text, 0, text.size, bounds)
    }

}