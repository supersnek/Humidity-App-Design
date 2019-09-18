package com.ksondzyk.humidity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

data class BezierCurve(
    var c0: Float = 0.0F,
    var c1: Float = 0.0F,
    var c2: Float = 0.0F,
    var c3: Float = 0.0F
)

data class ProgressData(
    val maxY: Float,
    val minY: Float,
    val maxProgress: Float,
    val minProgress: Float
)

data class Range(val start: Float, val end: Float)

class CurveSeekView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var backgroundShadowColor = context.getColorCompat(R.color.background)
        set(value) {
            field = value
            invalidate()
        }

    //Curve
    var firstGradientColor = context.getColorCompat(R.color.colorAccent)
        set(value) {
            field = value
            invalidate()
        }
    var secondGradientColor = context.getColorCompat(R.color.secondaryAccent)
        set(value) {
            field = value
            invalidate()
        }
    private val optimalPercentageStart = 0.3F
    private val optimalPercentageEnd = 0.7F
    private val curveStrokeWidth = 4F.toPx()

    //Scale
    var scaleColor = context.getColorCompat(R.color.scaleColor)
        set(value) {
            field = value
            scalePaint.color = value
            invalidate()
        }
    private val scalePaddingVertical = 72.0F.toPx()
    private val scalePaddingHorizontal = 8F.toPx()
    private val bigLineWidth = 20F.toPx()
    private val smallLineWidth = 14F.toPx()
    private val bigLineCount = 9
    private val bigLineStep = 6
    private val linesCount = bigLineCount + 5 * bigLineCount

    //Slider
    var sliderColor = context.getColorCompat(R.color.white)
        set(value) {
            field = value
            circlePaint.color = value
            invalidate()
        }
    private var sliderDrawable = context.getDrawableCompat(R.drawable.ic_slider)
    var sliderIconColor = context.getColorCompat(R.color.white)
        set(value) {
            field = value

            sliderDrawable?.colorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
            invalidate()
        }
    private val sliderCirclePadding = 12F.toPx()
    private val sliderTouchArea = 32F.toPx()
    private val sliderCircleRadius = 16F.toPx()

    //Label
    var selectedLabelColor = context.getColorCompat(R.color.colorAccent)
        set(value) {
            field = value
            invalidate()
        }
    var labelColor = context.getColorCompat(R.color.white)
        set(value) {
            field = value
            invalidate()
        }
    private val selectedLabelTextSize = 28F.toPx()
    private val labelTextSize = 18F.toPx()
    private val labelPaddingVertical = 4F.toPx()
    private val labelsCount = 10
    private val labelPadding = 16F.toPx()
    private val labelSuffix = "%"

    //Mark
    private val markColor = context.getColorCompat(R.color.warningColor)
    private val markPadding = labelPadding * 0.5F
    private val markRadius = 2F.toPx()
    private val markRanges = arrayOf(Range(10.0F, 30.5F), Range(80.0F, 100.0F))

    //Progress
    private val maxProgress = 10F
    private val minProgress = 100F

    //Fling
    private val flingMultiplier = 2
    private val flingAnimationDuration = 200L
    private val flingInterpolator = DecelerateInterpolator(2F)


    private var progressX = 0.0F
    private var progressY = 0.5F

    private var progress = 0.0F

    private lateinit var labels: Array<Label>

    var onProgressChangeListener: ((progress: Float) -> Unit)? = null

    private val gradientLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = firstGradientColor
        strokeWidth = this@CurveSeekView.curveStrokeWidth
    }

    private val shadowGradientLinePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        strokeWidth = this@CurveSeekView.curveStrokeWidth
    }

    private val circlePaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        isAntiAlias = true
        color = sliderColor
    }

    private val scalePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = scaleColor
        strokeWidth = 1F.toPx()
    }

    private val markPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        isAntiAlias = true
        color = markColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        progressX = width - sliderCircleRadius
        val scalePaddingDiv = scalePaddingVertical / h

        shadowGradientLinePaint.shader = LinearGradient(
            0F,
            0F,
            progressX,
            0F,
            backgroundShadowColor.adjustAlpha(1F),
            backgroundShadowColor.adjustAlpha(0.0F),
            Shader.TileMode.CLAMP
        )

        gradientLinePaint.shader = LinearGradient(
            progressX,
            0.0F,
            progressX,
            h.toFloat(),
            intArrayOf(
                secondGradientColor.adjustAlpha(0.0F),
                secondGradientColor,
                secondGradientColor,
                secondGradientColor,
                firstGradientColor,
                firstGradientColor,
                secondGradientColor,
                secondGradientColor,
                secondGradientColor,
                secondGradientColor.adjustAlpha(0.0F)
            ),
            floatArrayOf(
                0.0F,
                scalePaddingDiv,
                scalePaddingDiv,
                optimalPercentageStart - 0.05F,
                optimalPercentageStart + 0.05F,
                optimalPercentageEnd - 0.05F,
                optimalPercentageEnd + 0.05F,
                1.0F - scalePaddingDiv,
                1.0F - scalePaddingDiv,
                1.0F
            ),
            Shader.TileMode.CLAMP
        )

        progressY = ((1F - (progress - maxProgress) / (minProgress - maxProgress)) *
                (height - scalePaddingVertical * 2)) + scalePaddingVertical

        calcPaths()
        initLabels()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            gradientLinePaint.style = Paint.Style.FILL
            gradientLinePaint.alpha = 70
            drawPath(gradientPath, gradientLinePaint)
            drawPath(gradientPath, shadowGradientLinePaint)
            gradientLinePaint.style = Paint.Style.STROKE
            gradientLinePaint.alpha = 255
            drawPath(curvePath, gradientLinePaint)
            drawCircle(progressX, progressY, sliderCircleRadius, circlePaint)
            sliderDrawable?.setBounds(
                (progressX - sliderCircleRadius).toInt(),
                (progressY - sliderCircleRadius).toInt(),
                (progressX + sliderCircleRadius).toInt(),
                (progressY + sliderCircleRadius).toInt()
            )
            sliderDrawable?.draw(canvas)
            drawScale()
            drawLabels()
        }
    }

    private var labelFocusIndex = -1
    private val textBounds = Rect()

    private fun initLabels() {
        labels = Array(labelsCount) {
            val y = (((height - scalePaddingVertical * 2) / (labelsCount - 1)) * it) +
                    scalePaddingVertical

            Label(
                y,
                selectedLabelColor,
                labelColor,
                selectedLabelTextSize,
                labelTextSize,
                ProgressData(
                    height - scalePaddingVertical,
                    scalePaddingVertical, maxProgress, minProgress
                ),
                labelSuffix,
                this
            )
        }
        calcLabels()
    }

    private fun calcLabels() {
        if (labelFocusIndex >= 0) {
            var focus = labels[labelFocusIndex]
            focus.getTextBounds(textBounds)
            var topFocusY = focus.currentY - textBounds.height() * 0.5F - labelPaddingVertical
            var bottomFocusY = focus.currentY + textBounds.height() * 0.5F + labelPaddingVertical

            var focusRequestIndex = -1
            val scaleHeight = height - scalePaddingVertical * 2

            labels.forEachIndexed { index, label ->
                label.getTextBounds(textBounds)
                val y = ((scaleHeight / bigLineCount) * index) + scalePaddingVertical
                val topY = y - textBounds.height() * 0.5F - labelPaddingVertical
                val bottomY = y + textBounds.height() * 0.5F + labelPaddingVertical

                if (((topFocusY < bottomY && topFocusY > topY) ||
                            (bottomFocusY > topY && bottomFocusY < bottomY))
                    && index != labelFocusIndex
                ) {

                    focusRequestIndex = index
                }
            }

            if (focusRequestIndex >= 0) {
                labels[labelFocusIndex].outFocus(this)
                labelFocusIndex = focusRequestIndex
                labels[labelFocusIndex].inFocus(this)
            }

            focus = labels[labelFocusIndex]
            focus.getTextBounds(textBounds)
            topFocusY = focus.currentY - textBounds.height() * 0.5F - labelPaddingVertical
            bottomFocusY = focus.currentY + textBounds.height() * 0.5F + labelPaddingVertical

            if (progressY > bottomFocusY) {
                labels[labelFocusIndex].currentY += progressY - bottomFocusY
                return
            }

            if (progressY < topFocusY) {
                labels[labelFocusIndex].currentY -= topFocusY - progressY
                return
            }

            labels[labelFocusIndex].currentY = labels[labelFocusIndex].currentY

        } else {
            var minDiffLabelIndex = 0
            for (i in 1..(labels.lastIndex)) {
                val dy1 = abs(labels[i].currentY - progressY)
                val dy2 = abs(labels[minDiffLabelIndex].currentY - progressY)
                if (dy1 < dy2) {
                    minDiffLabelIndex = i
                }
            }

            labels[minDiffLabelIndex].currentY = progressY
            labels[minDiffLabelIndex].inFocus(this)

            labelFocusIndex = minDiffLabelIndex
        }
    }

    private fun Canvas.drawLabels() {
        labels.forEach {
            it.getTextBounds(textBounds)

            val labelProgress = it.progress * 100.0F

            markRanges.find { range -> labelProgress >= range.start && labelProgress <= range.end }
                ?.apply {
                    drawCircle(markPadding, it.currentY, markRadius, markPaint)
                }

            drawText(
                it.text,
                0,
                it.lastCharIndex, labelPadding,
                it.currentY - textBounds.centerY(),
                it.paint
            )
        }
    }

    private val bezierCurveXStart = BezierCurve()
    private val bezierCurveYStart = BezierCurve()
    private val bezierCurveYEnd = BezierCurve()
    private val bezierCurveXEnd = BezierCurve()

    private val curvePath = Path()
    private val gradientPath = Path()

    private fun calcPaths() {
        calcPath(curvePath)

        calcPath(gradientPath, -curveStrokeWidth * 0.5F + 1F)
        gradientPath.apply {
            lineTo(0F, height.toFloat())
            lineTo(0F, 0F)
            lineTo(progressX, 0.0F)
            close()
        }
    }

    private fun calcPath(path: Path, shift: Float = 0F) {
        path.apply {
            reset()
            val x = progressX + shift
            val y = progressY

            moveTo(x, 0.0F)

            bezierCurveXStart.c0 = x
            bezierCurveYStart.c0 = getEdgeY(true)

            bezierCurveXStart.c1 = x
            bezierCurveYStart.c1 = curveY1(true)

            bezierCurveXStart.c2 = curveX1() + shift
            bezierCurveYStart.c2 = curveY2(true)

            bezierCurveXStart.c3 = curveX2() + shift
            bezierCurveYStart.c3 = y

            lineTo(bezierCurveXStart.c0, bezierCurveYStart.c0)

            path.cubicTo(
                bezierCurveXStart.c1,
                bezierCurveYStart.c1,
                bezierCurveXStart.c2,
                bezierCurveYStart.c2,
                bezierCurveXStart.c3,
                bezierCurveYStart.c3
            )

            bezierCurveXEnd.c0 = curveX2() + shift
            bezierCurveYEnd.c0 = y

            bezierCurveXEnd.c1 = curveX1() + shift
            bezierCurveYEnd.c1 = curveY2(false)

            bezierCurveXEnd.c2 = x
            bezierCurveYEnd.c2 = curveY1(false)

            bezierCurveXEnd.c3 = x
            bezierCurveYEnd.c3 = getEdgeY(false)

            path.cubicTo(
                bezierCurveXEnd.c1,
                bezierCurveYEnd.c1,
                bezierCurveXEnd.c2,
                bezierCurveYEnd.c2,
                bezierCurveXEnd.c3,
                bezierCurveYEnd.c3
            )

            lineTo(x, height.toFloat())
        }
    }

    private fun getEdgeY(first: Boolean) = progressY +
            (sliderCircleRadius * 2F + sliderCirclePadding * 2F) * if (first) -1 else +1

    private fun curveX1() =
        progressX - (sliderCircleRadius * 0.9F + sliderCirclePadding + curveStrokeWidth)

    private fun curveY1(first: Boolean) =
        progressY + (sliderCircleRadius * 1.3F) * if (first) -1 else +1

    private fun curveY2(first: Boolean) = progressY +
            (sliderCircleRadius * 1.3F + sliderCirclePadding + curveStrokeWidth) * if (first) -1 else +1

    private fun curveX2() =
        progressX - (sliderCircleRadius + sliderCirclePadding + curveStrokeWidth)

    private fun Canvas.drawScale() {
        val scaleSpacing = -scalePaddingHorizontal - curveStrokeWidth * 0.5F
        val lineStartX = progressX + scaleSpacing
        val dScale = (height - scalePaddingVertical * 2.0F) / linesCount
        for (i in 0..linesCount) {
            val lineWidth = if (i % bigLineStep == 0) bigLineWidth else smallLineWidth
            val lineY = dScale * i + scalePaddingVertical
            if (lineY > getEdgeY(true) && lineY < getEdgeY(false)) {
                if (lineY < progressY) {
                    val t = 1 - (progressY - lineY) / (progressY - getEdgeY(true))
                    val curveX = calcBezierCoordinate(t, bezierCurveXStart) + scaleSpacing
                    val curveY = calcBezierCoordinate(t, bezierCurveYStart)
                    drawLine(curveX, curveY, curveX - lineWidth, curveY, scalePaint)
                } else {
                    val t = (-progressY + lineY) / (getEdgeY(false) - progressY)
                    val curveX = calcBezierCoordinate(t, bezierCurveXEnd) + scaleSpacing
                    val curveY = calcBezierCoordinate(t, bezierCurveYEnd)
                    drawLine(curveX, curveY, curveX - lineWidth, curveY, scalePaint)
                }
            } else {
                drawLine(lineStartX, lineY, lineStartX - lineWidth, lineY, scalePaint)
            }
        }
    }

    private fun calcBezierCoordinate(t: Float, bezierCurve: BezierCurve): Float {
        return (1 - t) * (1 - t) * (1 - t) * bezierCurve.c0 +
                3 * (1 - t) * (1 - t) * t * bezierCurve.c1 +
                3 * (1 - t) * t * t * bezierCurve.c2 +
                t * t * t * bezierCurve.c3
    }

    fun getProgress(): Float = getProgress(progressY)

    private fun getProgress(y: Float): Float {
        return ((y - scalePaddingVertical) /
                (height - scalePaddingVertical * 2)) * (maxProgress - minProgress) + minProgress
    }

    fun setProgress(newProgress: Float) {
        progress = newProgress
        updateSeekY(
            (progress / maxProgress.coerceAtLeast(minProgress) *
                    (height - scalePaddingVertical * 2)) + scalePaddingVertical
        )
    }

    private var isSeeking = false
    private var dSeekingY = 0.0F

    private var flingAnimator: ValueAnimator? = null

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && inTouchArea(event)) {
            dSeekingY = event.y - progressY
            isSeeking = true
            flingAnimator?.cancel()
            performClick()
            return true
        }

        if (event.action == MotionEvent.ACTION_MOVE) {
            handleMove(event)
            return true
        }

        if (event.action == MotionEvent.ACTION_UP) {
            dSeekingY = 0.0F
            isSeeking = false
            fling()
            performClick()
            return true
        }

        return false
    }

    private fun inTouchArea(event: MotionEvent): Boolean {
        val centerSeekX = progressX
        val centerSeekY = progressY

        return event.x < centerSeekX + sliderTouchArea &&
                event.x > centerSeekX - sliderTouchArea &&
                event.y < centerSeekY + sliderTouchArea &&
                event.y > centerSeekY - sliderTouchArea
    }

    private var previousSeekY = progressY
    private var previousTouchEventTime = System.currentTimeMillis()
    private var progressSpeed = 0.0F

    private fun handleMove(event: MotionEvent) {
        updateSeekY(event.y - dSeekingY)
    }

    private fun fling() {
        val step = progressSpeed * flingMultiplier
        flingAnimator?.cancel()
        flingAnimator = ValueAnimator.ofFloat(2F, 0.5F)
        flingAnimator?.duration = flingAnimationDuration
        flingAnimator?.interpolator = flingInterpolator
        flingAnimator?.addUpdateListener { animation ->
            updateSeekY(progressY + step * animation.animatedValue as Float)
        }
        flingAnimator?.start()
        progressSpeed = 0F
    }

    private fun updateSeekY(newSeekY: Float) {
        if (height == 0) return
        previousSeekY = progressY

        val dTime = System.currentTimeMillis() - previousTouchEventTime
        if (newSeekY > scalePaddingVertical && newSeekY < height - scalePaddingVertical) {
            progressSpeed = (newSeekY - previousSeekY) / dTime
            progressY = newSeekY
        } else if (newSeekY <= scalePaddingVertical) {
            progressSpeed = (scalePaddingVertical - previousSeekY) / dTime
            progressY = scalePaddingVertical
        } else {
            progressSpeed = ((height - scalePaddingVertical) - previousSeekY) / dTime
            progressY = (height - scalePaddingVertical)
        }

        previousTouchEventTime = System.currentTimeMillis()
        calcPaths()
        calcLabels()
        invalidate()
        progress = getProgress()
        onProgressChangeListener?.invoke(progress)
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.progress = progress
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        setProgress(savedState.progress)
    }

    internal class SavedState : BaseSavedState {
        var progress: Float = 0.0F

        constructor(superState: Parcelable?) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            progress = `in`.readFloat()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(progress)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

}
