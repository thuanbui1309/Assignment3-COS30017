package com.example.assignment3_cos30017.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Bid
import com.example.assignment3_cos30017.databinding.ItemBidBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BidAdapter(
    private val showActions: Boolean,
    private val showRejectButton: Boolean = true,
    private val highlightBidderId: String? = null,
    private val onApprove: (Bid) -> Unit,
    private val onReject: (Bid) -> Unit,
    private val onChat: (Bid) -> Unit
) : ListAdapter<Bid, BidAdapter.BidViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BidViewHolder {
        val binding = ItemBidBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BidViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BidViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BidViewHolder(private val binding: ItemBidBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bid: Bid) {
            val ctx = binding.root.context
            binding.tvBidderName.text = bid.bidderName
            binding.tvBidSummary.text = ctx.getString(R.string.bid_summary_format, bid.dailyRate, bid.rentalDays)
            binding.tvBidTotal.text = ctx.getString(R.string.credits_format, bid.totalAmount)

            if (bid.message.isNotBlank()) {
                binding.tvBidMessage.text = ctx.getString(R.string.quoted_text, bid.message)
                binding.tvBidMessage.visibility = View.VISIBLE
            } else {
                binding.tvBidMessage.visibility = View.GONE
            }

            val (statusText, statusColor) = when (bid.status) {
                Bid.STATUS_PENDING -> ctx.getString(R.string.status_pending) to R.color.accent
                Bid.STATUS_APPROVED -> ctx.getString(R.string.status_approved) to R.color.credit_positive
                Bid.STATUS_REJECTED -> ctx.getString(R.string.status_rejected) to R.color.text_secondary
                Bid.STATUS_CANCELLED -> ctx.getString(R.string.status_cancelled) to R.color.text_secondary
                else -> bid.status to R.color.text_secondary
            }
            binding.tvBidStatus.text = statusText
            val bg = GradientDrawable().apply {
                cornerRadius = 8f * ctx.resources.displayMetrics.density
                setColor(ContextCompat.getColor(ctx, statusColor))
            }
            binding.tvBidStatus.background = bg

            val isPending = bid.status == Bid.STATUS_PENDING
            binding.layoutBidActions.visibility = if (showActions && isPending) View.VISIBLE else View.GONE
            binding.btnReject.visibility = if (showRejectButton) View.VISIBLE else View.GONE

            // Highlight current user's bid
            if (highlightBidderId != null && bid.bidderId == highlightBidderId) {
                val highlightBg = GradientDrawable().apply {
                    cornerRadius = 12f * ctx.resources.displayMetrics.density
                    setStroke(
                        (2 * ctx.resources.displayMetrics.density).toInt(),
                        ContextCompat.getColor(ctx, R.color.accent)
                    )
                }
                (binding.root as? com.google.android.material.card.MaterialCardView)?.strokeColor =
                    ContextCompat.getColor(ctx, R.color.accent)
                (binding.root as? com.google.android.material.card.MaterialCardView)?.strokeWidth =
                    (2 * ctx.resources.displayMetrics.density).toInt()
            } else {
                (binding.root as? com.google.android.material.card.MaterialCardView)?.strokeColor =
                    ContextCompat.getColor(ctx, R.color.divider)
                (binding.root as? com.google.android.material.card.MaterialCardView)?.strokeWidth =
                    (1 * ctx.resources.displayMetrics.density).toInt()
            }

            binding.btnApprove.setOnClickListener { onApprove(bid) }
            binding.btnReject.setOnClickListener { onReject(bid) }
            binding.btnChatBidder.setOnClickListener { onChat(bid) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Bid>() {
            override fun areItemsTheSame(oldItem: Bid, newItem: Bid) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Bid, newItem: Bid) = oldItem == newItem
        }
    }
}
