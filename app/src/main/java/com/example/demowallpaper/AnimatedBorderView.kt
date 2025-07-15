package com.example.demowallpaper

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class AnimatedBorderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var borderThickness = 10f
    private var animationSpeed = 50
    private var animationProgress = 0f
    private var borderLength = 100f // Phần trăm chiều dài của đường viền (10-100%)
    private var cornerRadius = 0f // Bán kính góc bo tròn (0-50)

    // Mảng màu có thể thay đổi
    private var colors = intArrayOf(
        Color.parseColor("#FF0000"), // Đỏ
        Color.parseColor("#00FF00"), // Xanh lá
        Color.parseColor("#FFFF00"), // Vàng
        Color.parseColor("#8A2BE2")  // Tím
    )

    private var animationRunnable: Runnable? = null
    private var isAnimating = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    fun updateBorder(thickness: Int, speed: Int, length: Int, corner: Int, newColors: List<Int>? = null) {
        borderThickness = thickness.toFloat()
        animationSpeed = speed
        borderLength = length.toFloat()
        cornerRadius = corner.toFloat()
        newColors?.let {
            colors = it.toIntArray()
        }
        invalidate()
    }

    private fun startAnimation() {
        if (isAnimating) return
        isAnimating = true

        animationRunnable = object : Runnable {
            override fun run() {
                if (isAnimating) {
                    animationProgress += (animationSpeed / 100f) * 1f
                    if (animationProgress >= 360f) {
                        animationProgress = 0f
                    }
                    invalidate()
                    postDelayed(this, 16) // ~60 FPS
                }
            }
        }
        post(animationRunnable)
    }

    private fun stopAnimation() {
        isAnimating = false
        animationRunnable?.let { removeCallbacks(it) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rect = RectF(
            borderThickness / 2,
            borderThickness / 2,
            width - borderThickness / 2,
            height - borderThickness / 2
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderThickness
        paint.strokeCap = Paint.Cap.ROUND

        // Vẽ border với rainbow effect
        drawRainbowBorder(canvas, rect)
    }

    private fun drawRainbowBorder(canvas: Canvas, rect: RectF) {
        // Tạo path cho viền với góc bo tròn
        val path = Path()
        if (cornerRadius > 0) {
            path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        } else {
            path.addRect(rect, Path.Direction.CW)
        }

        val pathMeasure = PathMeasure(path, false)
        val totalLength = pathMeasure.length

        // Tính toán chiều dài thực tế của border dựa trên phần trăm
        val actualBorderLength = (totalLength * borderLength / 100f)

        // Vị trí bắt đầu của border (dựa trên animation progress)
        val startPosition = (animationProgress / 360f) * totalLength

        // Chia border thành các segment nhỏ để tạo gradient mượt
        val segmentCount = kotlin.math.max(50, (actualBorderLength / 10f).toInt())
        val segmentLength = actualBorderLength / segmentCount

        for (i in 0 until segmentCount) {
            val segmentStart = (startPosition + i * segmentLength) % totalLength
            val segmentEnd = (startPosition + (i + 1) * segmentLength) % totalLength

            // Tính toán màu cho segment này
            val colorProgress = (i.toFloat() / segmentCount) * colors.size
            val colorIndex = colorProgress.toInt() % colors.size
            val nextColorIndex = (colorIndex + 1) % colors.size
            val colorFraction = colorProgress - colorIndex

            val color = interpolateColor(colors[colorIndex], colors[nextColorIndex], colorFraction)
            paint.color = color

            // Vẽ segment
            val segmentPath = Path()

            if (segmentStart < segmentEnd) {
                pathMeasure.getSegment(segmentStart, segmentEnd, segmentPath, true)
            } else {
                // Trường hợp segment vượt qua điểm cuối của path
                pathMeasure.getSegment(segmentStart, totalLength, segmentPath, true)
                val tempPath = Path()
                pathMeasure.getSegment(0f, segmentEnd, tempPath, true)
                segmentPath.addPath(tempPath)
            }

            canvas.drawPath(segmentPath, paint)
        }
    }

    // Phương thức để interpolate giữa hai màu
    private fun interpolateColor(color1: Int, color2: Int, fraction: Float): Int {
        val clampedFraction = fraction.coerceIn(0f, 1f)

        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        val r = (r1 + (r2 - r1) * clampedFraction).toInt()
        val g = (g1 + (g2 - g1) * clampedFraction).toInt()
        val b = (b1 + (b2 - b1) * clampedFraction).toInt()

        return Color.rgb(r, g, b)
    }
}