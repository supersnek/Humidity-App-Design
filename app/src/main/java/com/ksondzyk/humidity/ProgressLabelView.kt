package com.ksondzyk.humidity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.log10

private const val EMPTY_CHAR = '#'

class ProgressLabelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var animationDuration = 800L

    var textColor = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = textColor
        strokeWidth = 1F.toPx()
        textSize = 26F.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val text: CharArray = CharArray(12) { EMPTY_CHAR }
    private val newText: CharArray = CharArray(12) { EMPTY_CHAR }
    private var lastCharIndex: Int = 0
    private val suffix: String = "%"
    private val maxValueString = "100%"
    private val charChanged = BooleanArray(maxValueString.length) { false }
    private var animationProgress = 0.0F

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setMaxTextSize(paint, maxValueString)
    }

    private var nextProgress = 0

    fun setProgress(progress: Int) {
        nextProgress = progress
        if (animationProgress != 0.0F) return

        var n = progress

        for (i in newText.indices) newText[i] = EMPTY_CHAR
        for (i in charChanged.indices) charChanged[i] = false

        lastCharIndex = (log10(n.toDouble()) + 1).toInt()

        for (i in (lastCharIndex - 1) downTo 0) {
            newText[i] = '0' + n % 10
            n /= 10
        }

        suffix.forEachIndexed { index, c -> newText[lastCharIndex + index] = c }

        var change = false

        for (i in charChanged.indices) {
            charChanged[i] = text[i] != newText[i]
            if (!change) change = charChanged[i]
        }

        if (change) startChangeAnim()

        invalidate()
    }

    private var animator: ValueAnimator? = null

    private fun startChangeAnim() {
        animationProgress = 1.0F
        animator = ValueAnimator.ofFloat(1.0F, 0.0F)
        animator?.apply {
            duration = animationDuration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                if (animationProgress == 0.0F) {
                    newText.forEachIndexed { index, c -> text[index] = c }
                    setProgress(nextProgress)
                }
                invalidate()
            }
            start()
        }
    }

    private fun setMaxTextSize(paint: Paint, text: String) {
        val testTextSize = 48f

        paint.textSize = testTextSize
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val desiredTextSize = testTextSize * width / bounds.width()

        paint.textSize = desiredTextSize
    }

    private val bounds = Rect()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            var x = 0F
            paint.getTextBounds(text, 0, text.lastIndex, bounds)
            val textHeight = bounds.height()
            val y = height + (textHeight * 0.5F - height * 0.5F)
            for (i in charChanged.indices) {
                if (!charChanged[i]) {
                    if (text[i] == EMPTY_CHAR) return
                    paint.color = textColor
                    drawText(text, i, 1, x, y, paint)
                } else {
                    if (text[i] != EMPTY_CHAR) {
                        paint.color = textColor.adjustAlpha(
                            if (animationProgress > 0.5) animationProgress * 2 else 0F
                        )
                        drawText(text, i, 1, x, y - textHeight * (1.0F - animationProgress), paint)
                    }

                    if (newText[i] != EMPTY_CHAR) {
                        paint.color = textColor.adjustAlpha(1 - animationProgress)
                        drawText(newText, i, 1, x, y + textHeight * animationProgress, paint)
                    }
                }
                x += paint.measureText(text, i, 1)
            }
        }
    }

}