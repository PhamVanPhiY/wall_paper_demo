package com.example.demowallpaper

import android.Manifest
import android.content.SharedPreferences
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class TransparentCameraWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "TransparentCameraWallpaper"
        // SharedPreferences constants
        const val PREF_NAME = "transparent_wallpaper_settings"
        const val KEY_TRANSPARENCY = "transparency"
        const val KEY_USE_FRONT_CAMERA = "use_front_camera"
    }

    override fun onCreateEngine(): Engine {
        return TransparentCameraEngine()
    }

    inner class TransparentCameraEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private var cameraManager: CameraManager? = null
        private var cameraDevice: CameraDevice? = null
        private var captureSession: CameraCaptureSession? = null
        private var backgroundThread: HandlerThread? = null
        private var backgroundHandler: Handler? = null
        private var cameraId: String = "0"
        private var isUsingFrontCamera = false
        private var imageReader: ImageReader? = null
        private var lastFrameTime = 0L
        private val frameDelay = 33L // ~30 FPS để giảm lag

        // SharedPreferences
        private lateinit var sharedPreferences: SharedPreferences

        // Thêm biến để lưu orientation
        private var sensorOrientation = 0
        private var displayRotation = 0

        private val paint = Paint().apply {
            isAntiAlias = true
            alpha = 180 // Độ trong suốt (0-255, 180 ≈ 70%)
            isFilterBitmap = true // Smooth scaling
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

            // Khởi tạo SharedPreferences
            sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)

            // Đọc cài đặt từ SharedPreferences
            loadSettings()

            // Lấy display rotation
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            displayRotation = windowManager.defaultDisplay.rotation

            startBackgroundThread()
        }

        private fun loadSettings() {
            isUsingFrontCamera = sharedPreferences.getBoolean(KEY_USE_FRONT_CAMERA, false)
            val transparency = sharedPreferences.getInt(KEY_TRANSPARENCY, 70)

            // Cập nhật alpha của paint dựa trên transparency
            paint.alpha = (transparency * 255 / 100).coerceIn(0, 255)

            Log.d(TAG, "Loaded settings - Front camera: $isUsingFrontCamera, Transparency: $transparency")
        }

        // Implement SharedPreferences.OnSharedPreferenceChangeListener
        @RequiresPermission(Manifest.permission.CAMERA)
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                KEY_USE_FRONT_CAMERA -> {
                    val newCameraState = sharedPreferences?.getBoolean(KEY_USE_FRONT_CAMERA, false) ?: false
                    if (isUsingFrontCamera != newCameraState) {
                        Log.d(TAG, "Camera setting changed - switching to ${if (newCameraState) "front" else "back"} camera")
                        isUsingFrontCamera = newCameraState
                        // Restart camera với cài đặt mới
                        if (isVisible) {
                            closeCamera()
                            openCamera()
                        }
                    }
                }
                KEY_TRANSPARENCY -> {
                    val transparency = sharedPreferences?.getInt(KEY_TRANSPARENCY, 70) ?: 70
                    paint.alpha = (transparency * 255 / 100).coerceIn(0, 255)
                    Log.d(TAG, "Transparency changed to: $transparency")
                }
            }
        }

        @RequiresPermission(Manifest.permission.CAMERA)
        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            openCamera()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            closeCamera()
        }

        @RequiresPermission(Manifest.permission.CAMERA)
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                // Reload settings khi wallpaper hiển thị lại
                loadSettings()
                openCamera()
            } else {
                closeCamera()
            }
        }

        private fun startBackgroundThread() {
            backgroundThread = HandlerThread("CameraBackground").also { it.start() }
            backgroundHandler = Handler(backgroundThread?.looper!!)
        }

        private fun stopBackgroundThread() {
            backgroundThread?.quitSafely()
            try {
                backgroundThread?.join()
                backgroundThread = null
                backgroundHandler = null
            } catch (e: InterruptedException) {
                Log.e(TAG, "Error stopping background thread", e)
            }
        }

        @RequiresPermission(Manifest.permission.CAMERA)
        private fun openCamera() {
            try {
                val manager = cameraManager ?: return

                Log.d(TAG, "Opening camera - Using front camera: $isUsingFrontCamera")

                // Chọn camera (back hoặc front) dựa trên cài đặt
                for (id in manager.cameraIdList) {
                    val characteristics = manager.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                    if (!isUsingFrontCamera && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = id
                        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                        Log.d(TAG, "Selected back camera: $id")
                        break
                    } else if (isUsingFrontCamera && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraId = id
                        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                        Log.d(TAG, "Selected front camera: $id")
                        break
                    }
                }

                Log.d(TAG, "Camera sensor orientation: $sensorOrientation, Display rotation: $displayRotation")

                // Thiết lập ImageReader để nhận frame từ camera
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val outputSizes = map?.getOutputSizes(ImageFormat.YUV_420_888)

                val previewSize = outputSizes?.find {
                    it.width <= 1280 && it.height <= 720
                } ?: outputSizes?.get(outputSizes.size - 1) ?: Size(1280, 720)

                imageReader = ImageReader.newInstance(
                    previewSize.width,
                    previewSize.height,
                    ImageFormat.YUV_420_888,
                    1
                )

                imageReader?.setOnImageAvailableListener({
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastFrameTime < frameDelay) {
                        it.acquireLatestImage()?.close()
                        return@setOnImageAvailableListener
                    }
                    lastFrameTime = currentTime

                    val image = it.acquireLatestImage()
                    image?.let { img ->
                        processImage(img)
                        img.close()
                    }
                }, backgroundHandler)

                manager.openCamera(cameraId, stateCallback, backgroundHandler)

            } catch (e: CameraAccessException) {
                Log.e(TAG, "Cannot access camera", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Camera permission not granted", e)
            }
        }

        private val stateCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createCameraPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
                Log.e(TAG, "Camera error: $error")
            }
        }

        private fun createCameraPreviewSession() {
            try {
                val reader = imageReader ?: return
                val device = cameraDevice ?: return

                val previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                previewRequestBuilder.addTarget(reader.surface)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    createModernCameraSession(device, reader, previewRequestBuilder)
                } else {
                    createLegacyCameraSession(device, reader, previewRequestBuilder)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error creating camera preview session", e)
                createLegacyCameraSession()
            }
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private fun createModernCameraSession(
            device: CameraDevice,
            reader: ImageReader,
            previewRequestBuilder: CaptureRequest.Builder
        ) {
            val outputConfig = OutputConfiguration(reader.surface)
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(outputConfig),
                { command -> backgroundHandler?.post(command) },
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        captureSession = session
                        try {
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )

                            val previewRequest = previewRequestBuilder.build()
                            session.setRepeatingRequest(previewRequest, null, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Error creating capture session", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Configuration failed")
                    }
                }
            )

            device.createCaptureSession(sessionConfig)
        }

        private fun createLegacyCameraSession() {
            try {
                val reader = imageReader ?: return
                val device = cameraDevice ?: return

                val previewRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                previewRequestBuilder.addTarget(reader.surface)

                device.createCaptureSession(
                    listOf(reader.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            if (cameraDevice == null) return

                            captureSession = session
                            try {
                                previewRequestBuilder.set(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )

                                val previewRequest = previewRequestBuilder.build()
                                session.setRepeatingRequest(previewRequest, null, backgroundHandler)
                            } catch (e: CameraAccessException) {
                                Log.e(TAG, "Error creating capture session", e)
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "Configuration failed")
                        }
                    },
                    backgroundHandler
                )
            } catch (e: CameraAccessException) {
                Log.e(TAG, "Error creating legacy camera session", e)
            }
        }

        private fun createLegacyCameraSession(
            device: CameraDevice,
            reader: ImageReader,
            previewRequestBuilder: CaptureRequest.Builder
        ) {
            device.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        captureSession = session
                        try {
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )

                            val previewRequest = previewRequestBuilder.build()
                            session.setRepeatingRequest(previewRequest, null, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Error creating capture session", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Configuration failed")
                    }
                },
                backgroundHandler
            )
        }

        private fun processImage(image: android.media.Image) {
            try {
                val bitmap = yuv420ToBitmap(image)
                bitmap?.let {
                    drawToWallpaper(it)
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
            }
        }

        private fun yuv420ToBitmap(image: android.media.Image): Bitmap? {
            try {
                val planes = image.planes
                val yBuffer = planes[0].buffer
                val uBuffer = planes[1].buffer
                val vBuffer = planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)

                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                val out = java.io.ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 60, out)
                val imageBytes = out.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                return processImageOrientation(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Error converting YUV to Bitmap", e)
                return null
            }
        }

        private fun processImageOrientation(bitmap: Bitmap?): Bitmap? {
            if (bitmap == null) return null

            try {
                val matrix = Matrix()
                val rotation = getImageRotation()
                Log.d(TAG, "Applying rotation: $rotation degrees, Front camera: $isUsingFrontCamera")

                if (isUsingFrontCamera) {
                    matrix.preScale(-1.0f, 1.0f) // Lật ngang để khắc phục hiệu ứng gương
                    if (rotation == 270 || rotation == 90) {
                        matrix.postRotate(90f) // Xoay thêm 180 độ nếu cần
                    }
                }

                if (rotation != 0 && !isUsingFrontCamera) {
                    matrix.postRotate(rotation.toFloat()) // Xoay cho camera sau
                }

                val processedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )

                if (bitmap != processedBitmap && !bitmap.isRecycled) {
                    bitmap.recycle()
                }

                return processedBitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image orientation", e)
                return bitmap
            }
        }

        private fun getImageRotation(): Int {
            val displayRotationDegrees = when (displayRotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            return if (isUsingFrontCamera) {
                // Công thức cho camera trước
                (sensorOrientation + displayRotationDegrees + 360) % 360
            } else {
                // Công thức cho camera sau
                (sensorOrientation - displayRotationDegrees + 360) % 360
            }
        }

        private fun drawToWallpaper(bitmap: Bitmap) {
            val holder = surfaceHolder
            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas()
                canvas?.let { c ->
                    c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                    val canvasWidth = c.width.toFloat()
                    val canvasHeight = c.height.toFloat()
                    val bitmapWidth = bitmap.width.toFloat()
                    val bitmapHeight = bitmap.height.toFloat()

                    val scaleX = canvasWidth / bitmapWidth
                    val scaleY = canvasHeight / bitmapHeight
                    val scale = maxOf(scaleX, scaleY)

                    val scaledWidth = (bitmapWidth * scale).toInt()
                    val scaledHeight = (bitmapHeight * scale).toInt()

                    val left = (canvasWidth - scaledWidth) / 2
                    val top = (canvasHeight - scaledHeight) / 2

                    val dstRect = Rect(
                        left.toInt(),
                        top.toInt(),
                        (left + scaledWidth).toInt(),
                        (top + scaledHeight).toInt()
                    )
                    val srcRect = Rect(0, 0, bitmap.width, bitmap.height)

                    c.drawBitmap(bitmap, srcRect, dstRect, paint)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error drawing to wallpaper", e)
            } finally {
                canvas?.let {
                    try {
                        holder.unlockCanvasAndPost(it)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unlocking canvas", e)
                    }
                }
            }
        }

        private fun closeCamera() {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null
        }

        override fun onDestroy() {
            super.onDestroy()
            // Unregister SharedPreferences listener
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            closeCamera()
            stopBackgroundThread()
        }
    }
}