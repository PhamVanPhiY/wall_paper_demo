package com.example.demowallpaper

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ColorAdapter(
    private val colors: MutableList<Int>,
    private val onColorClick: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorCircle: View = itemView.findViewById(R.id.color_circle)
        val colorText: TextView = itemView.findViewById(R.id.color_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.color_item, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colors[position]

        // Set background color for circle
        val drawable = holder.colorCircle.background as GradientDrawable
        drawable.setColor(color)

        // Set color hex text
        holder.colorText.text = String.format("#%06X", 0xFFFFFF and color)

        // Handle click to remove color
        holder.itemView.setOnClickListener {
            onColorClick(position)
        }
    }

    override fun getItemCount(): Int = colors.size

    fun addColor(color: Int) {
        if (!colors.contains(color)) {
            colors.add(color)
            notifyItemInserted(colors.size - 1)
        }
    }

    fun removeColor(position: Int) {
        if (position in 0 until colors.size) {
            colors.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getColors(): List<Int> = colors.toList()
}