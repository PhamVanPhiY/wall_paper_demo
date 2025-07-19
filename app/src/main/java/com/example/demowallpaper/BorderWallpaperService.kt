package com.example.demowallpaper

import android.content.Context
import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

class BorderWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return BorderWallpaperEngine()
    }

    inner class BorderWallpaperEngine : Engine() {
        private var drawThread: Thread? = null
        private var isRunning = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var animationProgress = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var borderThickness = 10f
        private var animationSpeed = 50
        private var borderLength = 100f // Phần trăm chiều dài của đường viền (10-100%)
        private var cornerRadius = 0f // Bán kính góc bo tròn (0-50)

        // Mảng màu có thể thay đổi
        private var colors = intArrayOf(
            Color.parseColor("#FF0000"), // Đỏ
            Color.parseColor("#00FF00"), // Xanh lá
            Color.parseColor("#FFFF00"), // Vàng
            Color.parseColor("#8A2BE2")  // Tím
        )

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            loadSettings()
            Log.d("BorderWallpaper", "Engine created")
        }

        private fun loadSettings() {
            val sharedPref = getSharedPreferences("border_wallpaper_settings", Context.MODE_PRIVATE)
            borderThickness = sharedPref.getInt("thickness", 10).toFloat()
            animationSpeed = sharedPref.getInt("speed", 50)
            borderLength = sharedPref.getInt("length", 100).toFloat()
            cornerRadius = sharedPref.getInt("corner", 0).toFloat()

            // Load saved colors
            val savedColorsJson = sharedPref.getString("colors", null)
            if (savedColorsJson != null) {
                try {
                    val gson = com.google.gson.Gson()
                    val type = object : com.google.gson.reflect.TypeToken<List<Int>>() {}.type
                    val savedColors: List<Int> = gson.fromJson(savedColorsJson, type)
                    colors = savedColors.toIntArray()
                } catch (e: Exception) {
                    // If loading fails, use default colors
                    e.printStackTrace()
                }
            }

            Log.d("BorderWallpaper", "Loaded settings - Thickness: $borderThickness, Speed: $animationSpeed, Length: $borderLength, Corner: $cornerRadius, Colors: ${colors.size}")
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            Log.d("BorderWallpaper", "Surface changed: ${width}x${height}")
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isRunning = visible
            if (visible) {
                loadSettings() // Reload settings when wallpaper becomes visible
                startDrawing()
            } else {
                stopDrawing()
            }
            Log.d("BorderWallpaper", "Visibility changed: $visible")
        }

        private fun startDrawing() {
            stopDrawing()

            drawThread = Thread {
                while (isRunning && !Thread.currentThread().isInterrupted) {
                    val canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        try {
                            // Clear canvas with black background
                            canvas.drawColor(Color.BLACK)

                            // Draw animated border with rainbow effect
                            drawRainbowBorder(canvas)

                            // Update animation progress
                            animationProgress += (animationSpeed / 100f) * 1f
                            if (animationProgress >= 360f) {
                                animationProgress = 0f
                            }

                        } catch (e: Exception) {
                            Log.e("BorderWallpaper", "Error drawing", e)
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
            Log.d("BorderWallpaper", "Drawing thread started")
        }

        private fun drawRainbowBorder(canvas: Canvas) {
            val rect = RectF(
                borderThickness / 2,
                borderThickness / 2,
                surfaceWidth - borderThickness / 2,
                surfaceHeight - borderThickness / 2
            )

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderThickness
            paint.strokeCap = Paint.Cap.ROUND

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

        private fun stopDrawing() {
            drawThread?.interrupt()
            try {
                drawThread?.join(1000)
            } catch (e: InterruptedException) {
                // Ignore
            }
            drawThread = null
            Log.d("BorderWallpaper", "Drawing thread stopped")
        }

        override fun onDestroy() {
            Log.d("BorderWallpaper", "Engine destroying")
            isRunning = false
            stopDrawing()
            super.onDestroy()
        }
    }
}