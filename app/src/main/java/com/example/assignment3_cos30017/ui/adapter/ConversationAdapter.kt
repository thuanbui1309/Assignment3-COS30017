package com.example.assignment3_cos30017.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Conversation
import com.example.assignment3_cos30017.data.model.Message
import com.example.assignment3_cos30017.databinding.ItemConversationBinding
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private val onClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(ConvoDiffCallback()) {

    private var unreadCounts: Map<String, Int> = emptyMap()
    private var avatarUrlsByUserId: Map<String, String?> = emptyMap()

    class ViewHolder(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val convo = getItem(position)
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val otherName = convo.participantNames.entries.firstOrNull { it.key != currentUid }?.value
            ?: holder.binding.root.context.getString(R.string.generic_user)
        val otherUserId = convo.participants.firstOrNull { it != currentUid }.orEmpty()

        holder.binding.tvParticipantName.text = otherName
        val avatarUrl = avatarUrlsByUserId[otherUserId]
        if (!avatarUrl.isNullOrBlank()) {
            holder.binding.ivAvatar.visibility = View.VISIBLE
            holder.binding.tvAvatarInitials.visibility = View.GONE
            Glide.with(holder.binding.root).load(avatarUrl).into(holder.binding.ivAvatar)
        } else {
            holder.binding.ivAvatar.visibility = View.GONE
            holder.binding.tvAvatarInitials.visibility = View.VISIBLE
            holder.binding.tvAvatarInitials.text = otherName.take(1).uppercase()
        }
        holder.binding.tvLastMessage.text = if (convo.lastMessage == Message.TEXT_TOKEN_PHOTO) {
            holder.binding.root.context.getString(R.string.chat_last_message_photo)
        } else {
            convo.lastMessage
        }

        if (convo.lastMessageTime > 0) {
            holder.binding.tvTime.text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                .format(Date(convo.lastMessageTime))
        }

        val count = unreadCounts[convo.id] ?: 0
        if (count > 0) {
            holder.binding.tvUnreadCount.visibility = View.VISIBLE
            holder.binding.tvUnreadCount.text = if (count > 99) {
                holder.binding.root.context.getString(R.string.unread_count_overflow)
            } else {
                count.toString()
            }
            holder.binding.tvParticipantName.setTypeface(null, android.graphics.Typeface.BOLD)
            holder.binding.tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            holder.binding.tvUnreadCount.visibility = View.GONE
            holder.binding.tvParticipantName.setTypeface(null, android.graphics.Typeface.NORMAL)
            holder.binding.tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        holder.itemView.setOnClickListener { onClick(convo) }
    }

    fun updateUnreadCounts(counts: Map<String, Int>) {
        unreadCounts = counts
        notifyDataSetChanged()
    }

    fun updateAvatarUrls(urlsByUserId: Map<String, String?>) {
        avatarUrlsByUserId = urlsByUserId
        notifyDataSetChanged()
    }

    private class ConvoDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(o: Conversation, n: Conversation) = o.id == n.id
        override fun areContentsTheSame(o: Conversation, n: Conversation) = o == n
    }
}
