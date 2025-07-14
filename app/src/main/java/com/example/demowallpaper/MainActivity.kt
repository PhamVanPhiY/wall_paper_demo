package com.example.demowallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var wallpaper_image: ImageView
    private lateinit var set_wallpaper: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wallpaper_image = findViewById<ImageView>(R.id.wallpaper_image)
        wallpaper_image.setImageResource(R.drawable.background)


        set_wallpaper = findViewById<Button>(R.id.set_wallpaper)
        set_wallpaper.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, GifWallpaperService::class.java)
            )
            startActivity(intent)
        }

    }
}