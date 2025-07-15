package com.example.demowallpaper

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BorderWallpaperActivity : AppCompatActivity() {
    private lateinit var borderPreview: AnimatedBorderView
    private lateinit var thicknessSeekBar: SeekBar
    private lateinit var speedSeekBar: SeekBar
    private lateinit var lengthSeekBar: SeekBar
    private lateinit var cornerSeekBar: SeekBar
    private lateinit var applyButton: Button
    private lateinit var cancelButton: Button
    private lateinit var showColorPickerButton: Button
    private lateinit var colorsRecyclerView: RecyclerView
    private lateinit var colorAdapter: ColorAdapter

    private var currentThickness = 10 // Giá trị từ 5 đến 50 (độ dày viền)
    private var currentSpeed = 50 // Giá trị từ 1 đến 100 (tốc độ animation)
    private var currentLength = 100 // Giá trị từ 10 đến 100 (phần trăm chiều dài viền)
    private var currentCorner = 0 // Giá trị từ 0 đến 50 (bán kính góc bo tròn)

    // Màu mặc định
    private val defaultColors = mutableListOf(
        Color.parseColor("#FF0000"), // Đỏ
        Color.parseColor("#00FF00"), // Xanh lá
        Color.parseColor("#FFFF00"), // Vàng
        Color.parseColor("#8A2BE2")  // Tím
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_border_wallpaper)

        initViews()
        setupSeekBars()
        setupColorRecyclerView()
        setupButtons()

        // Load saved settings
        loadSettings()
        updatePreview()
    }

    private fun initViews() {
        borderPreview = findViewById(R.id.border_preview)
        thicknessSeekBar = findViewById(R.id.thickness_seekbar)
        speedSeekBar = findViewById(R.id.speed_seekbar)
        lengthSeekBar = findViewById(R.id.length_seekbar)
        cornerSeekBar = findViewById(R.id.corner_seekbar)
        applyButton = findViewById(R.id.apply_button)
        cancelButton = findViewById(R.id.cancel_button)
        showColorPickerButton = findViewById(R.id.show_color_picker_button)
        colorsRecyclerView = findViewById(R.id.colors_recycler_view)
    }

    private fun setupColorRecyclerView() {
        colorAdapter = ColorAdapter(defaultColors) { position ->
            // Remove color when clicked
            if (colorAdapter.getColors().size > 1) { // Ensure at least 1 color remains
                colorAdapter.removeColor(position)
                updatePreview()
            }
        }

        colorsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        colorsRecyclerView.adapter = colorAdapter
    }

    private fun loadSettings() {
        val sharedPref = getSharedPreferences("border_wallpaper_settings", MODE_PRIVATE)
        currentThickness = sharedPref.getInt("thickness", 10)
        currentSpeed = sharedPref.getInt("speed", 50)
        currentLength = sharedPref.getInt("length", 100)
        currentCorner = sharedPref.getInt("corner", 0)

        // Load saved colors
        val savedColorsJson = sharedPref.getString("colors", null)
        if (savedColorsJson != null) {
            try {
                val gson = Gson()
                val type = object : TypeToken<List<Int>>() {}.type
                val savedColors: List<Int> = gson.fromJson(savedColorsJson, type)
                defaultColors.clear()
                defaultColors.addAll(savedColors)
                colorAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                // If loading fails, use default colors
                e.printStackTrace()
            }
        }

        // Update seekbars
        thicknessSeekBar.progress = currentThickness - 5 // Convert to 0-45 range
        speedSeekBar.progress = currentSpeed - 1 // Convert to 0-99 range
        lengthSeekBar.progress = currentLength - 10 // Convert to 0-90 range
        cornerSeekBar.progress = currentCorner // 0-50 range
    }

    private fun setupSeekBars() {
        // Thickness SeekBar (5 - 50)
        thicknessSeekBar.max = 45 // 0-45 maps to 5-50
        thicknessSeekBar.progress = 5 // Default 10

        thicknessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentThickness = progress + 5 // 5-50
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Speed SeekBar (1-100)
        speedSeekBar.max = 199 // 0-99 maps to 1-100
        speedSeekBar.progress = 49 // Default 50

        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSpeed = progress + 1 // 1-100
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Length SeekBar (10-100)
        lengthSeekBar.max = 90 // 0-90 maps to 10-100
        lengthSeekBar.progress = 90 // Default 100

        lengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentLength = progress + 10 // 10-100
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Corner SeekBar (0-50)
        cornerSeekBar.max = 50 // 0-50
        cornerSeekBar.progress = 0 // Default 0

        cornerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentCorner = progress // 0-50
                updatePreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        showColorPickerButton.setOnClickListener {
            showColorPicker()
        }

        applyButton.setOnClickListener {
            saveSettings()
            applyWallpaper()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun showColorPicker() {
        ColorPickerDialog.Builder(this)
            .setTitle("Choose Color")
            .setPreferenceName("ColorPickerDialog")
            .setPositiveButton("Confirm", ColorEnvelopeListener { envelope, _ ->
                val selectedColor = envelope.color
                colorAdapter.addColor(selectedColor)
                updatePreview()
            })
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .attachAlphaSlideBar(true)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)
            .show()
    }

    private fun updatePreview() {
        val colors = colorAdapter.getColors()
        borderPreview.updateBorder(currentThickness, currentSpeed, currentLength, currentCorner, colors)
    }

    private fun saveSettings() {
        val sharedPref = getSharedPreferences("border_wallpaper_settings", MODE_PRIVATE)
        val gson = Gson()
        val colorsJson = gson.toJson(colorAdapter.getColors())

        with(sharedPref.edit()) {
            putInt("thickness", currentThickness)
            putInt("speed", currentSpeed)
            putInt("length", currentLength)
            putInt("corner", currentCorner)
            putString("colors", colorsJson)
            apply()
        }
    }

    private fun applyWallpaper() {
        try {
            val intent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, BorderWallpaperService::class.java)
            )

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                finish()
            } else {
                // Fallback
                val fallbackIntent = Intent(Intent.ACTION_SET_WALLPAPER)
                startActivity(Intent.createChooser(fallbackIntent, "Select Wallpaper"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Error applying wallpaper: ${e.message}",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }
}