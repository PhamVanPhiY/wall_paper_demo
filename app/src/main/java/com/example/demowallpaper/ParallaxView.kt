package com.example.demeparallax

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.demowallpaper.R
import kotlin.math.max
import kotlin.math.min

class ParallaxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null

    // Các layer parallax
    private var backgroundBitmap: Bitmap? = null
    private var mountainBitmap: Bitmap? = null
    private var treesBitmap: Bitmap? = null
    private var foregroundBitmap: Bitmap? = null

    // Paint objects
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Vị trí hiện tại
    private var currentX = 0f
    private var currentY = 0f

    // Độ nhạy và tốc độ di chuyển cho từng layer
    private val sensitivity = 8f
    private val backgroundSpeed = 3f
    private val mountainSpeed = 2f
    private val treesSpeed = 1f
    private val foregroundSpeed = 0.8f

    // Scale factor cho background
    private val backgroundScale = 1.3f

    // Giới hạn di chuyển
    private var maxMoveX = 100f
    private var maxMoveY = 60f

    // Matrix cho background
    private val backgroundMatrix = Matrix()

    // Rect objects để tránh allocation trong onDraw
    private val srcRect = Rect()
    private val dstRect = RectF()

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        loadBitmaps()
    }

    private fun loadBitmaps() {
        try {
            // Load các bitmap từ drawable resources
            backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
            // mountainBitmap = BitmapFactory.decodeResource(resources, R.drawable.mountain) // Nếu có
            treesBitmap = BitmapFactory.decodeResource(resources, R.drawable.people)
            foregroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.foreground_layer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startSensorListener()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopSensorListener()
    }

    fun startSensorListener() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopSensorListener() {
        sensorManager.unregisterListener(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Cập nhật giới hạn di chuyển dựa trên kích thước view
        maxMoveX = w * 0.08f   // 8% chiều rộng
        maxMoveY = h * 0.05f   // 5% chiều cao

        // Reset vị trí
        currentX = 0f
        currentY = 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (viewWidth <= 0 || viewHeight <= 0) return

        // Vẽ background với black color
        canvas.drawColor(Color.BLACK)

        // Vẽ background layer với Matrix
        backgroundBitmap?.let { bitmap ->
            drawBackgroundLayer(canvas, bitmap, viewWidth, viewHeight)
        }

        // Vẽ mountain layer
        mountainBitmap?.let { bitmap ->
            val moveX = max(-maxMoveX * 0.8f, min(currentX * mountainSpeed, maxMoveX * 0.8f))
            val moveY = max(-maxMoveY * 0.8f, min(currentY * mountainSpeed, maxMoveY * 0.8f))
            drawTranslationLayer(canvas, bitmap, viewWidth, viewHeight, moveX, moveY)
        }

        // Vẽ trees layer
        treesBitmap?.let { bitmap ->
            val moveX = max(-maxMoveX * 0.6f, min(currentX * treesSpeed, maxMoveX * 0.6f))
            val moveY = max(-maxMoveY * 0.6f, min(currentY * treesSpeed, maxMoveY * 0.6f))
            drawTranslationLayer(canvas, bitmap, viewWidth, viewHeight, moveX, moveY)
        }

        // Vẽ foreground layer
        foregroundBitmap?.let { bitmap ->
            val moveX = max(-maxMoveX * 0.4f, min(currentX * foregroundSpeed, maxMoveX * 0.4f))
            val moveY = max(-maxMoveY * 0.4f, min(currentY * foregroundSpeed, maxMoveY * 0.4f))
            drawTranslationLayer(canvas, bitmap, viewWidth, viewHeight, moveX, moveY)
        }

        // Vẽ UI overlay
        drawUIOverlay(canvas, viewWidth, viewHeight)
    }

    private fun drawBackgroundLayer(canvas: Canvas, bitmap: Bitmap, viewWidth: Float, viewHeight: Float) {
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        // Tính scale để ảnh phủ kín view và có thể di chuyển
        val scaleX = (viewWidth * backgroundScale) / imageWidth
        val scaleY = (viewHeight * backgroundScale) / imageHeight
        val scale = max(scaleX, scaleY)

        // Tính vị trí center ban đầu
        val scaledImageWidth = imageWidth * scale
        val scaledImageHeight = imageHeight * scale
        val baseCenterX = (viewWidth - scaledImageWidth) / 2f
        val baseCenterY = (viewHeight - scaledImageHeight) / 2f

        // Tính di chuyển
        val moveX = currentX * backgroundSpeed
        val moveY = currentY * backgroundSpeed

        // Tính giới hạn di chuyển
        val maxBackgroundMoveX = (scaledImageWidth - viewWidth) / 2f
        val maxBackgroundMoveY = (scaledImageHeight - viewHeight) / 2f

        // Giới hạn di chuyển
        val limitedMoveX = max(-maxBackgroundMoveX, min(moveX, maxBackgroundMoveX))
        val limitedMoveY = max(-maxBackgroundMoveY, min(moveY, maxBackgroundMoveY))

        // Đảo ngược hướng di chuyển
        val finalX = baseCenterX - limitedMoveX
        val finalY = baseCenterY - limitedMoveY

        // Thiết lập matrix
        backgroundMatrix.reset()
        backgroundMatrix.postScale(scale, scale)
        backgroundMatrix.postTranslate(finalX, finalY)

        // Vẽ bitmap với matrix
        canvas.drawBitmap(bitmap, backgroundMatrix, paint)
    }

    private fun drawTranslationLayer(canvas: Canvas, bitmap: Bitmap, viewWidth: Float, viewHeight: Float, moveX: Float, moveY: Float) {
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        // Tính scale để fit view
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = max(scaleX, scaleY)

        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        // Tính vị trí center
        val left = (viewWidth - scaledWidth) / 2f + moveX
        val top = (viewHeight - scaledHeight) / 2f + moveY

        // Vẽ bitmap
        dstRect.set(left, top, left + scaledWidth, top + scaledHeight)
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    }

    private fun drawUIOverlay(canvas: Canvas, viewWidth: Float, viewHeight: Float) {
        // Tạo gradient overlay
        val gradient = LinearGradient(
            0f, 0f, 0f, viewHeight,
            intArrayOf(
                Color.argb(0, 0, 0, 0),
                Color.argb(20, 0, 0, 0),
                Color.argb(40, 0, 0, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint)
        paint.shader = null

        // Vẽ text
        paint.color = Color.WHITE
        paint.textSize = viewWidth * 0.08f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.setShadowLayer(4f, 2f, 2f, Color.BLACK)

        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        canvas.drawText("3D PARALLAX", centerX, centerY - 20, paint)

        paint.textSize = viewWidth * 0.035f
        paint.color = Color.argb(224, 224, 224, 224) // #E0E0E0
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.setShadowLayer(2f, 1f, 1f, Color.BLACK)

        canvas.drawText("DEPTH LAYERS", centerX, centerY + 30, paint)

        // Reset paint
        paint.clearShadowLayer()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]

                val targetX = currentX - (x * sensitivity)
                val targetY = currentY + (y * sensitivity)

                val smoothing = 0.08f
                currentX += (targetX - currentX) * smoothing
                currentY += (targetY - currentY) * smoothing

                // Yêu cầu vẽ lại
                invalidate()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Không cần xử lý
    }

    // Method để cleanup resources
    fun cleanup() {
        stopSensorListener()
        backgroundBitmap?.recycle()
        mountainBitmap?.recycle()
        treesBitmap?.recycle()
        foregroundBitmap?.recycle()
    }
}