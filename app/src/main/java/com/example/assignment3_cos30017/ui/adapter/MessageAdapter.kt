package com.example.assignment3_cos30017.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Message
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val currentUserId: String,
    private val onImageClick: ((String) -> Unit)? = null
) : ListAdapter<Message, MessageAdapter.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_SENT = 0
        private const val VIEW_RECEIVED = 1
        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    private var otherReadTime: Long = 0L

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_SENT else VIEW_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == VIEW_SENT) R.layout.item_message_sent else R.layout.item_message_received
        return ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = getItem(position)
        val view = holder.itemView

        val tvMessage = view.findViewById<TextView>(R.id.tv_message)
        val ivImage = view.findViewById<ImageView>(R.id.iv_image)
        val cardText = view.findViewById<MaterialCardView?>(R.id.card_text)
        val pbSending = view.findViewById<ProgressBar?>(R.id.pb_sending)
        val ivSendFailed = view.findViewById<ImageView?>(R.id.iv_send_failed)

        val hasImage = msg.type == Message.TYPE_IMAGE && !msg.imageUrl.isNullOrBlank()
        if (hasImage) {
            ivImage?.visibility = View.VISIBLE
            Glide.with(view.context).load(msg.imageUrl).into(ivImage!!)
            ivImage.setOnClickListener { onImageClick?.invoke(msg.imageUrl!!) }
        } else {
            ivImage?.visibility = View.GONE
            ivImage?.setOnClickListener(null)
        }

        val hasText = msg.text.isNotBlank()
        if (hasText) {
            tvMessage.text = msg.text
            tvMessage.visibility = View.VISIBLE
            cardText?.visibility = View.VISIBLE
        } else {
            tvMessage.visibility = View.GONE
            cardText?.visibility = View.GONE
        }

        view.findViewById<TextView>(R.id.tv_time).text = TIME_FORMAT.format(Date(msg.timestamp))
        view.findViewById<TextView>(R.id.tv_sender)?.text = msg.senderName

        val ivReadStatus = view.findViewById<ImageView>(R.id.iv_read_status)
        if (ivReadStatus != null && msg.senderId == currentUserId) {
            val isPending = msg.localStatus == Message.LocalStatus.SENDING
            val isFailed = msg.localStatus == Message.LocalStatus.FAILED

            pbSending?.visibility = if (isPending) View.VISIBLE else View.GONE
            ivSendFailed?.visibility = if (isFailed) View.VISIBLE else View.GONE
            ivReadStatus.visibility = if (isPending || isFailed) View.GONE else View.VISIBLE

            val isRead = msg.timestamp <= otherReadTime
            ivReadStatus.setImageResource(
                if (isRead) R.drawable.ic_double_check else R.drawable.ic_check
            )
        }
    }

    fun updateReadTime(readTime: Long) {
        otherReadTime = readTime
        notifyDataSetChanged()
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(o: Message, n: Message) = o.id == n.id
        override fun areContentsTheSame(o: Message, n: Message) = o == n
    }
}
