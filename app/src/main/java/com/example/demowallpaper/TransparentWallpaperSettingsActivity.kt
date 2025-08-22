package com.example.demowallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TransparentWallpaperSettingsActivity : AppCompatActivity() {

    private lateinit var transparencySeekBar: SeekBar
    private lateinit var transparencyLabel: TextView
    private lateinit var switchCameraButton: Button
    private lateinit var saveSettingsButton: Button
    private lateinit var applyWallpaperButton: Button

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREF_NAME = "transparent_wallpaper_settings"
        const val KEY_TRANSPARENCY = "transparency"
        const val KEY_USE_FRONT_CAMERA = "use_front_camera"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transparent_wallpaper_settings)

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        transparencySeekBar = findViewById(R.id.transparency_seekbar)
        transparencyLabel = findViewById(R.id.transparency_label)
        switchCameraButton = findViewById(R.id.switch_camera_button)
        saveSettingsButton = findViewById(R.id.save_settings_button)
        applyWallpaperButton = findViewById(R.id.apply_wallpaper_button) // Thêm button này vào layout

        setupTransparencyControl()
        setupButtons()
        loadSettings()
    }

    private fun setupTransparencyControl() {
        transparencySeekBar.max = 100
        transparencySeekBar.progress = 70 // Mặc định 70%

        transparencySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                transparencyLabel.text = "Độ trong suốt: $progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        switchCameraButton.setOnClickListener {
            val isUsingFront = sharedPreferences.getBoolean(KEY_USE_FRONT_CAMERA, false)
            val newValue = !isUsingFront

            sharedPreferences.edit()
                .putBoolean(KEY_USE_FRONT_CAMERA, newValue)
                .apply()

            updateCameraButtonText(newValue)
            Toast.makeText(this, "Đã chuyển sang camera ${if (newValue) "trước" else "sau"}", Toast.LENGTH_SHORT).show()
        }

        saveSettingsButton.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show()
        }

        applyWallpaperButton.setOnClickListener {
            saveSettings()
            setTransparentWallpaper()
        }
    }

    private fun loadSettings() {
        val transparency = sharedPreferences.getInt(KEY_TRANSPARENCY, 70)
        val isUsingFront = sharedPreferences.getBoolean(KEY_USE_FRONT_CAMERA, false)

        transparencySeekBar.progress = transparency
        transparencyLabel.text = "Độ trong suốt: $transparency%"
        updateCameraButtonText(isUsingFront)
    }

    private fun saveSettings() {
        val transparency = transparencySeekBar.progress

        sharedPreferences.edit()
            .putInt(KEY_TRANSPARENCY, transparency)
            .apply()
    }

    private fun updateCameraButtonText(isUsingFront: Boolean) {
        switchCameraButton.text = if (isUsingFront) {
            "Đang dùng: Camera trước\nBấm để chuyển sang camera sau"
        } else {
            "Đang dùng: Camera sau\nBấm để chuyển sang camera trước"
        }
    }

    private fun setTransparentWallpaper() {
        try {
            val intent = android.content.Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, TransparentCameraWallpaperService::class.java)
            )
            startActivity(intent)
            Toast.makeText(this, "Chọn 'Transparent Camera Wallpaper' để áp dụng", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở cài đặt wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}