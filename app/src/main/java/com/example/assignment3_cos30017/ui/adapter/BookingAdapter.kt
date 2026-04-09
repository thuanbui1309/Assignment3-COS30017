package com.example.assignment3_cos30017.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Booking
import com.example.assignment3_cos30017.databinding.ItemBookingBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingAdapter(
    private val onItemClick: (Booking) -> Unit
) : ListAdapter<Booking, BookingAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            val context = binding.root.context

            binding.tvCarTitle.text = booking.carTitle.ifEmpty {
                context.getString(R.string.car_details)
            }

            val (statusText, statusColor, barColor) = when (booking.status) {
                Booking.STATUS_ACTIVE    -> Triple(
                    context.getString(R.string.status_active),
                    context.getColor(R.color.accent),
                    context.getColor(R.color.accent)
                )
                Booking.STATUS_COMPLETED -> Triple(
                    context.getString(R.string.status_completed),
                    context.getColor(R.color.credit_positive),
                    context.getColor(R.color.credit_positive)
                )
                else                     -> Triple(
                    context.getString(R.string.status_cancelled),
                    context.getColor(R.color.text_secondary),
                    context.getColor(R.color.divider)
                )
            }

            binding.tvStatus.text = statusText
            binding.tvStatus.setTextColor(statusColor)
            binding.viewStatusBar.setBackgroundColor(barColor)

            binding.tvSummary.text = context.getString(
                R.string.booking_days_format, booking.rentalDays, booking.totalCost
            )
            binding.tvDate.text = dateFormat.format(Date(booking.bookingDate))

            binding.root.setOnClickListener { onItemClick(booking) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Booking, newItem: Booking) = oldItem == newItem
    }
}
