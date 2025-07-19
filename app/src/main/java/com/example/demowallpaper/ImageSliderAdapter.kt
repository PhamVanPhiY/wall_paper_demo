package com.example.demowallpaper

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageSliderAdapter(
    private val context: Context,
    private val wallpaperList: List<WallpaperItem>
) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val tvWallpaperName: TextView = view.findViewById(R.id.tvWallpaperName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = Int.MAX_VALUE // để vuốt vô hạn

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val actualPosition = position % wallpaperList.size
        val wallpaperItem = wallpaperList[actualPosition]

        holder.tvWallpaperName.text = wallpaperItem.name

        Glide.with(context)
            .asDrawable()
            .load(wallpaperItem.imageUrl)
            .into(holder.imageView)
    }
}