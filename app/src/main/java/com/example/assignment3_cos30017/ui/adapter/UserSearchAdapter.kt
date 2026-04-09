package com.example.assignment3_cos30017.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.data.model.User
import com.example.assignment3_cos30017.databinding.ItemUserSearchBinding

class UserSearchAdapter(
    private val onClick: (User) -> Unit
) : ListAdapter<User, UserSearchAdapter.ViewHolder>(UserDiffCallback()) {

    class ViewHolder(val binding: ItemUserSearchBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = getItem(position)
        holder.binding.tvUserName.text = user.displayName
        holder.binding.tvUserEmail.text = user.email
        val url = user.photoUrl
        if (!url.isNullOrBlank()) {
            holder.binding.ivAvatar.visibility = View.VISIBLE
            holder.binding.tvInitials.visibility = View.GONE
            Glide.with(holder.binding.root).load(url).into(holder.binding.ivAvatar)
        } else {
            holder.binding.ivAvatar.visibility = View.GONE
            holder.binding.tvInitials.visibility = View.VISIBLE
            holder.binding.tvInitials.text = user.displayName.take(1).uppercase()
        }
        holder.itemView.setOnClickListener { onClick(user) }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(o: User, n: User) = o.uid == n.uid
        override fun areContentsTheSame(o: User, n: User) = o == n
    }
}
