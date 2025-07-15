package com.example.demowallpaper

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import pl.droidsonroids.gif.GifImageView

class WallpaperSettingsActivity : AppCompatActivity() {
    private lateinit var gifPreview: GifImageView
    private lateinit var sizeSeekBar: SeekBar
    private lateinit var positionSeekBar: SeekBar
    private lateinit var applyButton: Button
    private lateinit var cancelButton: Button

    private var currentSize = 100 // Giá trị từ 50 đến 200 (50% - 200%)
    private var currentPosition = 0 // Giá trị từ 0 đến 100 (top to bottom)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_settings)

        initViews()
        setupSeekBars()
        setupButtons()

        // Load GIF preview
        gifPreview.setImageResource(R.drawable.wallpaper1)

        // Load saved settings
        loadSettings()
        updatePreview()
    }

    private fun initViews() {
        gifPreview = findViewById(R.id.gif_preview)
        sizeSeekBar = findViewById(R.id.size_seekbar)
        positionSeekBar = findViewById(R.id.position_seekbar)
        applyButton = findViewById(R.id.apply_button)
        cancelButton = findViewById(R.id.cancel_button)
    }

    private fun loadSettings() {
        val sharedPref = getSharedPreferences("wallpaper_settings", MODE_PRIVATE)
        currentSize = sharedPref.getInt("size", 100)
        currentPosition = sharedPref.getInt("position", 0)

        // Update seekbars
        sizeSeekBar.progress = currentSize - 50 // Convert back to 0-150 range
        positionSeekBar.progress = currentPosition
    }

    private fun setupSeekBars() {
        // Size SeekBar (50% - 200%)
        sizeSeekBar.max = 150 // 0-150 maps to 50%-200%
        sizeSeekBar.progress = 50 // Default 100%

        sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSize = progress + 50 // 50-200
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Position SeekBar (0-100)
        positionSeekBar.max = 100
        positionSeekBar.progress = 0 // Default top

        positionSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentPosition = progress
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        applyButton.setOnClickListener {
            saveSettings()
            applyWallpaper()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun updatePreview() {
        // Sử dụng logic tính toán tương tự như trong wallpaper service
        val scale = currentSize / 100f

        gifPreview.post {
            val previewContainer = findViewById<android.widget.RelativeLayout>(R.id.preview_container)
            val containerWidth = previewContainer.width
            val containerHeight = previewContainer.height

            if (containerWidth > 0 && containerHeight > 0) {
                // Tính toán kích thước base dựa trên container
                val baseSize = minOf(containerWidth, containerHeight) * 0.4f // 40% of container
                val scaledSize = baseSize * scale

                // Update GIF size
                val layoutParams = gifPreview.layoutParams
                layoutParams.width = scaledSize.toInt()
                layoutParams.height = scaledSize.toInt()
                gifPreview.layoutParams = layoutParams

                // Calculate position
                val maxTranslationY = if (containerHeight > scaledSize) {
                    containerHeight - scaledSize
                } else {
                    0f
                }
                val translationY = (currentPosition / 100f) * maxTranslationY

                gifPreview.translationY = translationY

                // Reset scale to 1.0 since we're setting size directly
                gifPreview.scaleX = 1.0f
                gifPreview.scaleY = 1.0f
            }
        }
    }

    private fun saveSettings() {
        val sharedPref = getSharedPreferences("wallpaper_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("size", currentSize)
            putInt("position", currentPosition)
            apply()
        }
    }

    private fun applyWallpaper() {
        val intent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, GifWallpaperService::class.java)
        )
        startActivity(intent)
        finish()
    }
}