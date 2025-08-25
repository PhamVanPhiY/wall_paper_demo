package com.example.demowallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.max
import kotlin.math.min

class ParallaxWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return ParallaxWallpaperEngine()
    }

    inner class ParallaxWallpaperEngine : Engine(), SensorEventListener {
        private var drawThread: Thread? = null
        private var isRunning = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        private lateinit var sensorManager: SensorManager
        private var accelerometer: Sensor? = null

        // Các layer parallax
        private lateinit var backgroundBitmap: Bitmap
        private lateinit var mountainBitmap: Bitmap
        private lateinit var treesBitmap: Bitmap
        private lateinit var foregroundBitmap: Bitmap

        // Vị trí hiện tại
        private var currentX = 0f
        private var currentY = 0f

        // Thêm các biến để tính giới hạn
        private var maxCurrentX = 0f
        private var maxCurrentY = 0f

        // Matrix cho từng layer
        private val backgroundMatrix = Matrix()
        private val mountainMatrix = Matrix()
        private val treesMatrix = Matrix()
        private val foregroundMatrix = Matrix()

        // Kích thước ảnh đã scale
        private var backgroundImageWidth = 0f
        private var backgroundImageHeight = 0f
        private var mountainImageWidth = 0f
        private var mountainImageHeight = 0f
        private var treesImageWidth = 0f
        private var treesImageHeight = 0f
        private var foregroundImageWidth = 0f
        private var foregroundImageHeight = 0f

        // Độ nhạy và tốc độ di chuyển
        private val sensitivity = 8f
        private val backgroundSpeed = 6f
        private val mountainSpeed = 5.5f
        private val treesSpeed = 5.5f
        private val foregroundSpeed = 3f

        // Scale factor - TĂNG ĐỂ TẠO THÊM KHÔNG GIAN DI CHUYỂN
        private val backgroundScale = 1.5f  // Tăng từ 1.3f
        private val mountainScale = 1.4f    // Tăng từ 1.2f
        private val treesScale = 1.3f       // Tăng từ 1.15f
        private val foregroundScale = 1.25f // Tăng từ 1.1f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // Khởi tạo SensorManager và Accelerometer
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            // Load các bitmap từ drawable
            backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.b1)
            mountainBitmap = BitmapFactory.decodeResource(resources, R.drawable.b2)
            treesBitmap = BitmapFactory.decodeResource(resources, R.drawable.b3)
            foregroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.b4)

            // Thiết lập matrix ban đầu
            setupAllMatrices()
        }

        private fun setupAllMatrices() {
            setupLayerMatrix(backgroundBitmap, backgroundMatrix, backgroundScale) { width, height ->
                backgroundImageWidth = width
                backgroundImageHeight = height
                maxCurrentX = max(0f, (width - surfaceWidth) / 2f / backgroundSpeed)
                maxCurrentY = max(0f, (height - surfaceHeight) / 2f / backgroundSpeed)

            }
            setupLayerMatrix(mountainBitmap, mountainMatrix, mountainScale) { width, height ->
                mountainImageWidth = width
                mountainImageHeight = height
            }
            setupLayerMatrix(treesBitmap, treesMatrix, treesScale) { width, height ->
                treesImageWidth = width
                treesImageHeight = height
            }
            setupLayerMatrix(foregroundBitmap, foregroundMatrix, foregroundScale) { width, height ->
                foregroundImageWidth = width
                foregroundImageHeight = height
            }
        }

        private fun setupLayerMatrix(
            bitmap: Bitmap,
            matrix: Matrix,
            scale: Float,
            onImageSizeCalculated: (Float, Float) -> Unit
        ) {
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            // THAY ĐỔI: Đảm bảo ảnh luôn lớn hơn surface ở cả 2 chiều
            val scaleX = (surfaceWidth * scale) / imageWidth
            val scaleY = (surfaceHeight * scale) / imageHeight

            // Sử dụng scale lớn nhất để đảm bảo có không gian di chuyển
            val finalScale = max(scaleX, scaleY)

            // Tính vị trí để center ảnh
            val scaledImageWidth = imageWidth * finalScale
            val scaledImageHeight = imageHeight * finalScale
            val translateX = (surfaceWidth - scaledImageWidth) / 2f
            val translateY = (surfaceHeight - scaledImageHeight) / 2f

            // Thiết lập matrix
            matrix.reset()
            matrix.postScale(finalScale, finalScale)
            matrix.postTranslate(translateX, translateY)

            // Lưu kích thước đã scale
            onImageSizeCalculated(scaledImageWidth, scaledImageHeight)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            setupAllMatrices() // Cập nhật matrix khi kích thước surface thay đổi
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isRunning = visible
            if (visible) {
                accelerometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                }
                startDrawing()
            } else {
                sensorManager.unregisterListener(this)
                stopDrawing()
            }
        }

        private fun startDrawing() {
            stopDrawing()
            drawThread = Thread {
                while (isRunning && !Thread.currentThread().isInterrupted) {
                    val canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        try {
                            // Clear canvas
                            canvas.drawColor(Color.BLACK)
                            // Vẽ các layer
                            updateAllLayers(canvas)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            surfaceHolder.unlockCanvasAndPost(canvas)
                        }
                    }
                    try {
                        Thread.sleep(16) // ~60 FPS
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
            drawThread?.start()
        }

        private fun updateAllLayers(canvas: Canvas) {
            updateLayerMatrix(
                canvas,
                backgroundBitmap,
                backgroundMatrix,
                backgroundScale,
                backgroundImageWidth,
                backgroundImageHeight,
                currentX * backgroundSpeed,
                currentY * backgroundSpeed
            )
            updateLayerMatrix(
                canvas,
                mountainBitmap,
                mountainMatrix,
                mountainScale,
                mountainImageWidth,
                mountainImageHeight,
                currentX * mountainSpeed,
                currentY * mountainSpeed
            )
            updateLayerMatrix(
                canvas,
                treesBitmap,
                treesMatrix,
                treesScale,
                treesImageWidth,
                treesImageHeight,
                currentX * treesSpeed,
                currentY * treesSpeed
            )
            updateLayerMatrix(
                canvas,
                foregroundBitmap,
                foregroundMatrix,
                foregroundScale,
                foregroundImageWidth,
                foregroundImageHeight,
                currentX * foregroundSpeed,
                currentY * foregroundSpeed
            )
        }

        private fun updateLayerMatrix(
            canvas: Canvas,
            bitmap: Bitmap,
            matrix: Matrix,
            scale: Float,
            scaledImageWidth: Float,
            scaledImageHeight: Float,
            moveX: Float,
            moveY: Float
        ) {
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            // Tính scale
            val scaleX = (surfaceWidth * scale) / imageWidth
            val scaleY = (surfaceHeight * scale) / imageHeight
            val finalScale = max(scaleX, scaleY)

            // Tính vị trí center ban đầu
            val baseCenterX = (surfaceWidth - scaledImageWidth) / 2f
            val baseCenterY = (surfaceHeight - scaledImageHeight) / 2f

            // Tính giới hạn di chuyển
            val maxMoveX = max(0f, (scaledImageWidth - surfaceWidth) / 2f)
            val maxMoveY = max(0f, (scaledImageHeight - surfaceHeight) / 2f)

            // Giới hạn di chuyển
            val limitedMoveX = max(-maxMoveX, min(moveX, maxMoveX))
            val limitedMoveY = max(-maxMoveY, min(moveY, maxMoveY))

            // Đảo ngược hướng di chuyển để tạo hiệu ứng parallax tự nhiên
            val finalX = baseCenterX - limitedMoveX
            val finalY = baseCenterY - limitedMoveY

            // Thiết lập matrix
            matrix.reset()
            matrix.postScale(finalScale, finalScale)
            matrix.postTranslate(finalX, finalY)

            // Vẽ bitmap lên canvas
            canvas.drawBitmap(bitmap, matrix, paint)
        }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Kiểm tra độ chính xác sensor
                    if (it.accuracy < SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                        return
                    }

                    val x = it.values[0]
                    val y = it.values[1]

                    // Giảm threshold để cho phép di chuyển nhỏ hơn
                    if (Math.abs(x) < 0.05f && Math.abs(y) < 0.05f) {
                        return
                    }

                    val targetX = currentX - (x * sensitivity)
                    val targetY = currentY + (y * sensitivity)

                    val smoothing = 0.15f
                    currentX += (targetX - currentX) * smoothing
                    currentY += (targetY - currentY) * smoothing

                    if (maxCurrentX > 0) {
                        currentX = max(-maxCurrentX, min(currentX, maxCurrentX))
                    }
                    if (maxCurrentY > 0) {
                        currentY = max(-maxCurrentY, min(currentY, maxCurrentY))
                    }

                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        private fun stopDrawing() {
            drawThread?.interrupt()
            try {
                drawThread?.join(1000)
            } catch (e: InterruptedException) {
                // Ignore
            }
            drawThread = null
        }
        override fun onDestroy() {
            isRunning = false
            stopDrawing()
            sensorManager.unregisterListener(this)
            super.onDestroy()
        }
    }
}