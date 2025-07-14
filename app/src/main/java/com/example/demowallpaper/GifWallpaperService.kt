package com.example.demowallpaper

import android.graphics.Color
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import pl.droidsonroids.gif.GifDrawable
import java.io.IOException

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

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            try {
                // Kiểm tra file tồn tại
                val inputStream = assets.open("wallpaper.gif")
                Log.d("GifWallpaper", "File size: ${inputStream.available()}")
                inputStream.close()

                // Đảm bảo file "wallpaper.gif" nằm trong thư mục: src/main/assets/
                gifDrawable = GifDrawable(assets, "wallpaper.gif").apply {
                    setLoopCount(0) // lặp vô hạn
                    start() // bắt đầu animation
                    Log.d("GifWallpaper", "GIF intrinsic size: ${intrinsicWidth}x${intrinsicHeight}")
                }
                Log.d("GifWallpaper", "GIF loaded: ${gifDrawable?.numberOfFrames} frames")
            } catch (e: IOException) {
                Log.e("GifWallpaper", "Failed to load GIF", e)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height

            // Thiết lập bounds cho GIF theo kích thước màn hình
            gifDrawable?.setBounds(0, 0, width, height)
            Log.d("GifWallpaper", "Surface changed: ${width}x${height}")
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isRunning = visible
            if (visible) {
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