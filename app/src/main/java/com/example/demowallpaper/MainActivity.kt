package com.example.demowallpaper

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var setWallpaper: Button
    private lateinit var borderWallpaper: Button
    private lateinit var transparentWallpaper: Button
    private lateinit var parallaxWallpaper: Button
    private lateinit var viewPager: ViewPager2
    private lateinit var imageSliderAdapter: ImageSliderAdapter

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setWallpaper = findViewById(R.id.set_wallpaper)
        setWallpaper.setOnClickListener {
            val intent = Intent(this, WallpaperSettingsActivity::class.java)
            startActivity(intent)
        }

        borderWallpaper = findViewById(R.id.wallpaper_border)
        borderWallpaper.setOnClickListener {
            val intent = Intent(this, BorderWallpaperActivity::class.java)
            startActivity(intent)
        }

        // SỬA ĐỔI: Mở TransparentWallpaperSettingsActivity thay vì set trực tiếp
        transparentWallpaper = findViewById(R.id.transparent_wallpaper)
        transparentWallpaper.setOnClickListener {
            openTransparentWallpaperSettings()
        }

        parallaxWallpaper = findViewById(R.id.set_parallax_wallpaper)
        parallaxWallpaper.setOnClickListener {
            try {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                intent.putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this, ParallaxWallpaperService::class.java)
                )
                startActivity(intent)
                Toast.makeText(this, "Chọn 'Parallax Wallpaper' để đặt làm background", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Không thể mở cài đặt wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }


        val wallpapers = listOf(
            WallpaperItem("Animated Cat", "https://media.giphy.com/media/ICOgUNjpvO0PC/giphy.gif"),
            WallpaperItem("Nature View", "https://picsum.photos/id/1011/600/400"),
            WallpaperItem("Cool Animation", "https://media.giphy.com/media/3o6Zt481isNVuQI1l6/giphy.gif"),
            WallpaperItem("Landscape", "https://picsum.photos/id/1012/600/400")
        )

        viewPager = findViewById(R.id.viewPager)
        imageSliderAdapter = ImageSliderAdapter(this, wallpapers)
        viewPager.adapter = imageSliderAdapter

        setupViewPager()

        // Bắt đầu ở giữa để vuốt được 2 chiều
        viewPager.setCurrentItem(Int.MAX_VALUE / 2, false)
    }





    /**
     * Mở activity cài đặt transparent wallpaper
     */
    private fun openTransparentWallpaperSettings() {
        // Kiểm tra quyền camera trước
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
            return
        }

        // Mở TransparentWallpaperSettingsActivity
        val intent = Intent(this, TransparentWallpaperSettingsActivity::class.java)
        startActivity(intent)
    }

    /**
     * Set transparent wallpaper trực tiếp (được gọi từ settings activity)
     */
    fun setTransparentWallpaper() {
        // Mở cài đặt live wallpaper
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, TransparentCameraWallpaperService::class.java)
            )
            startActivity(intent)
            Toast.makeText(this, "Chọn 'Transparent Camera Wallpaper' để đặt làm background", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở cài đặt wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Mở settings thay vì set trực tiếp
                val intent = Intent(this, TransparentWallpaperSettingsActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cần cấp quyền Camera để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViewPager() {
        // Đảm bảo ViewPager2 không clip các item bên cạnh
        viewPager.apply {
            clipToPadding = false
            clipChildren = false
            offscreenPageLimit = 3
        }

        // Cấu hình RecyclerView bên trong ViewPager2 sau khi adapter đã được set
        viewPager.post {
            val recyclerView = viewPager.getChildAt(0) as RecyclerView
            recyclerView.apply {
                clipToPadding = false
                clipChildren = false
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            }
        }

        // Thêm padding để hiển thị item bên cạnh - sử dụng giá trị cố định trước
        val paddingHorizontal = 80 // dp converted to px
        val paddingPx = (paddingHorizontal * resources.displayMetrics.density).toInt()
        viewPager.setPadding(paddingPx, 0, paddingPx, 0)

        // Tạo transformer với hiệu ứng rõ ràng hơn
        val transformer = CompositePageTransformer()

        // Thêm margin giữa các item
        transformer.addTransformer(MarginPageTransformer(0))

        // Custom transformer để scale và hiển thị item bên cạnh
        transformer.addTransformer { page, position ->
            val absPosition = abs(position)

            when {
                absPosition >= 2 -> {
                    // Item quá xa - ẩn hoàn toàn
                    page.alpha = 0f
                    page.scaleY = 0.7f
                    page.scaleX = 0.7f
                }

                absPosition >= 1 -> {
                    // Item bên cạnh xa - hiển thị nhỏ
                    page.alpha = 0.5f
                    page.scaleY = 0.8f
                    page.scaleX = 0.8f
                }

                else -> {
                    // Item trung tâm và item bên cạnh gần
                    page.alpha = 1f - absPosition * 0.3f
                    val scale = 1f - absPosition * 0.2f // Scale từ 1.0 xuống 0.8
                    page.scaleY = scale
                    page.scaleX = scale
                }
            }
        }

        viewPager.setPageTransformer(transformer)
    }
}