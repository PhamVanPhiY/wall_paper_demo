    package com.example.demowallpaper

    import android.content.Intent
    import android.os.Bundle
    import android.widget.Button
    import androidx.appcompat.app.AppCompatActivity
    import androidx.recyclerview.widget.RecyclerView
    import androidx.viewpager2.widget.CompositePageTransformer
    import androidx.viewpager2.widget.MarginPageTransformer
    import androidx.viewpager2.widget.ViewPager2
    import kotlin.math.abs

    class MainActivity : AppCompatActivity() {

        private lateinit var setWallpaper: Button
        private lateinit var borderWallpaper: Button
        private lateinit var viewPager: ViewPager2
        private lateinit var imageSliderAdapter: ImageSliderAdapter

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