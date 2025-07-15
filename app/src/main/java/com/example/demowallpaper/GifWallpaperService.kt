package com.example.demowallpaper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import pl.droidsonroids.gif.GifDrawable
import java.io.IOException
import kotlin.math.min

class GifWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return GifWallpaperEngine()
    }

    inner class GifWallpaperEngine : Engine() {
        private var gifDrawable: GifDrawable? = null
        private var drawThread: Thread? = null
        private var isRunning = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        // Settings values
        private var gifSize = 100 // 50-200 (50% - 200%)
        private var gifPosition = 0 // 0-100 (top to bottom)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            // Load settings
            loadSettings()

            try {
                // Kiểm tra file tồn tại
                val inputStream = assets.open("wallpaper1.gif")
                Log.d("GifWallpaper", "File size: ${inputStream.available()}")
                inputStream.close()

                // Đảm bảo file "wallpaper1.gif" nằm trong thư mục: src/main/assets/
                gifDrawable = GifDrawable(assets, "wallpaper1.gif").apply {
                    setLoopCount(0) // lặp vô hạn
                    start() // bắt đầu animation
                    Log.d(
                        "GifWallpaper",
                        "GIF intrinsic size: ${intrinsicWidth}x${intrinsicHeight}"
                    )
                }
                Log.d("GifWallpaper", "GIF loaded: ${gifDrawable?.numberOfFrames} frames")
            } catch (e: IOException) {
                Log.e("GifWallpaper", "Failed to load GIF", e)
            }
        }

        private fun loadSettings() {
            val sharedPref = getSharedPreferences("wallpaper_settings", Context.MODE_PRIVATE)
            gifSize = sharedPref.getInt("size", 100)
            gifPosition = sharedPref.getInt("position", 0)
            Log.d("GifWallpaper", "Loaded settings - Size: $gifSize, Position: $gifPosition")
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height

            updateGifBounds()
            Log.d("GifWallpaper", "Surface changed: ${width}x${height}")
        }

        private fun updateGifBounds() {
            gifDrawable?.let { gif ->
                val scale = gifSize / 100f

                // Sử dụng logic tương tự như trong preview
                // Tính toán base size dựa trên kích thước màn hình
                val baseSize = min(surfaceWidth, surfaceHeight) * 0.4f // 40% của màn hình
                val scaledSize = (baseSize * scale).toInt()

                // Center horizontally
                val x = (surfaceWidth - scaledSize) / 2

                // Calculate Y position based on position value (0-100)
                val maxY = if (surfaceHeight > scaledSize) {
                    surfaceHeight - scaledSize
                } else {
                    0
                }
                val y = ((gifPosition / 100f) * maxY).toInt()

                // Đảm bảo GIF không bị cắt
                val finalY = when {
                    y < 0 -> 0
                    y + scaledSize > surfaceHeight -> surfaceHeight - scaledSize
                    else -> y
                }

                gif.setBounds(x, finalY, x + scaledSize, finalY + scaledSize)
                Log.d("GifWallpaper", "GIF bounds updated: scale=$scale, x=$x, y=$finalY, size=$scaledSize")
                Log.d("GifWallpaper", "Surface: ${surfaceWidth}x${surfaceHeight}, Position: $gifPosition")
                Log.d("GifWallpaper", "Base size: $baseSize, Scaled size: $scaledSize")
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isRunning = visible
            if (visible) {
                // Reload settings when wallpaper becomes visible
                loadSettings()
                updateGifBounds()
                startDrawing()
            } else {
                stopDrawing()
            }
            Log.d("GifWallpaper", "Visibility changed: $visible")
        }

        private fun startDrawing() {
            stopDrawing() // Dừng thread cũ nếu có

            drawThread = Thread {
                while (isRunning && !Thread.currentThread().isInterrupted) {
                    val canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        try {
                            // Xóa canvas với màu đen
                            canvas.drawColor(Color.BLACK)

                            // Vẽ GIF
                            gifDrawable?.let { gif ->
                                if (!gif.isRecycled) {
                                    gif.draw(canvas)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("GifWallpaper", "Error drawing", e)
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
            Log.d("GifWallpaper", "Drawing thread started")
        }

        private fun stopDrawing() {
            drawThread?.interrupt()
            try {
                drawThread?.join(1000) // Đợi tối đa 1 giây
            } catch (e: InterruptedException) {
                // Ignore
            }
            drawThread = null
            Log.d("GifWallpaper", "Drawing thread stopped")
        }

        override fun onDestroy() {
            Log.d("GifWallpaper", "Engine destroying")
            isRunning = false
            stopDrawing()

            gifDrawable?.let { gif ->
                gif.stop()
                gif.recycle()
            }
            gifDrawable = null
            super.onDestroy()
        }
    }
}