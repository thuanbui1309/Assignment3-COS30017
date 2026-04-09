package com.example.assignment3_cos30017.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.assignment3_cos30017.R

class ImageSliderAdapter : RecyclerView.Adapter<ImageSliderAdapter.SlideViewHolder>() {

    private var urls: List<String> = emptyList()
    private var lastPreloadedRange: IntRange? = null

    fun submitList(newUrls: List<String>) {
        urls = newUrls
        lastPreloadedRange = null
        notifyDataSetChanged()
    }

    fun preloadAll(context: Context) {
        urls.forEach { url ->
            Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload()
        }
    }

    /**
     * Preload a small window of images around the current page to reduce
     * swipe jank without the memory spike of preloading the whole list.
     */
    fun preloadAround(context: Context, centerIndex: Int, radius: Int = 1) {
        if (urls.isEmpty()) return
        val start = (centerIndex - radius).coerceAtLeast(0)
        val end = (centerIndex + radius).coerceAtMost(urls.lastIndex)
        val range = start..end
        if (range == lastPreloadedRange) return
        lastPreloadedRange = range

        for (i in range) {
            Glide.with(context)
                .load(urls[i])
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload()
        }
    }

    override fun getItemCount() = urls.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val iv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_slide, parent, false) as ImageView
        return SlideViewHolder(iv)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        Glide.with(holder.imageView.context)
            .load(urls[position])
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerInside()
            .into(holder.imageView)
    }

    class SlideViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
}
