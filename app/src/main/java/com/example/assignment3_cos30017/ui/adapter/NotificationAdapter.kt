package com.example.assignment3_cos30017.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.AppNotification
import com.example.assignment3_cos30017.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val renderTitle: (AppNotification) -> String,
    private val renderBody: (AppNotification) -> String,
    private val onClick: ((AppNotification) -> Unit)? = null
) : ListAdapter<AppNotification, NotificationAdapter.ViewHolder>(Diff()) {

    class ViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val n = getItem(position)
        val context = holder.binding.root.context

        holder.binding.tvTitle.text = renderTitle(n)
        holder.binding.tvBody.text = renderBody(n)
        holder.binding.tvDate.text = DATE_FORMAT.format(Date(n.createdAt))

        val iconRes = when (n.type) {
            AppNotification.TYPE_NEW_BID -> R.drawable.ic_wallet
            AppNotification.TYPE_BID_APPROVED_WINNER -> R.drawable.ic_check
            AppNotification.TYPE_BID_APPROVED_LOSER -> R.drawable.ic_close
            AppNotification.TYPE_BID_APPROVED_OWNER -> R.drawable.ic_check
            AppNotification.TYPE_BID_REJECTED_BY_OWNER -> R.drawable.ic_close
            AppNotification.TYPE_BID_CANCELLED -> R.drawable.ic_close
            AppNotification.TYPE_BIDS_CANCELLED_BY_OWNER -> R.drawable.ic_close
            else -> R.drawable.ic_notifications
        }
        holder.binding.ivTypeIcon.setImageResource(iconRes)

        val unreadTint = if (!n.read) R.color.accent else R.color.text_secondary
        holder.binding.ivTypeIcon.setColorFilter(ContextCompat.getColor(context, unreadTint))

        holder.binding.root.alpha = if (n.read) 0.85f else 1f
        holder.binding.root.setOnClickListener { onClick?.invoke(n) }
    }

    private class Diff : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification) = oldItem == newItem
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }
}

