package com.example.demowallpaper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var setWallpaper: Button
    private lateinit var borderWallpaper: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        setWallpaper = findViewById(R.id.set_wallpaper)
        setWallpaper.setOnClickListener {
            // Chuyển đến màn hình settings thay vì trực tiếp apply wallpaper
            val intent = Intent(this, WallpaperSettingsActivity::class.java)
            startActivity(intent)
        }

        borderWallpaper = findViewById(R.id.wallpaper_border)
        borderWallpaper.setOnClickListener {
            // Chuyển đến màn hình border wallpaper settings
            val intent = Intent(this, BorderWallpaperActivity::class.java)
            startActivity(intent)
        }
    }
}