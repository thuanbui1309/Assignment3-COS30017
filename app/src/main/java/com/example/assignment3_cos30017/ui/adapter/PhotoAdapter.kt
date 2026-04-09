package com.example.assignment3_cos30017.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.databinding.ItemPhotoPreviewBinding

class PhotoAdapter(
    private val onRemove: (Int) -> Unit,
    private val onClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    sealed class PhotoItem {
        data class Local(val uri: Uri) : PhotoItem()
        data class Remote(val url: String) : PhotoItem()
    }

    private val items = mutableListOf<PhotoItem>()

    fun setLocalUris(uris: List<Uri>) {
        items.clear()
        items.addAll(uris.map { PhotoItem.Local(it) })
        notifyDataSetChanged()
    }

    fun setMixed(remoteUrls: List<String>, localUris: List<Uri>) {
        items.clear()
        items.addAll(remoteUrls.map { PhotoItem.Remote(it) })
        items.addAll(localUris.map { PhotoItem.Local(it) })
        notifyDataSetChanged()
    }

    fun getItem(position: Int): PhotoItem = items[position]

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PhotoItem, position: Int) {
            when (item) {
                is PhotoItem.Local -> Glide.with(binding.root.context)
                    .load(item.uri)
                    .centerCrop()
                    .into(binding.ivPhoto)
                is PhotoItem.Remote -> Glide.with(binding.root.context)
                    .load(item.url)
                    .centerCrop()
                    .into(binding.ivPhoto)
            }
            binding.btnRemove.setOnClickListener { onRemove(position) }
            binding.ivPhoto.setOnClickListener { onClick?.invoke(position) }
        }
    }
}
