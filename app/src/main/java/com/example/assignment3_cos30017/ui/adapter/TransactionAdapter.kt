package com.example.assignment3_cos30017.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Transaction
import com.example.assignment3_cos30017.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val onClick: ((Transaction) -> Unit)? = null
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    class ViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = getItem(position)
        val context = holder.binding.root.context

        holder.binding.tvDescription.text = txn.description
        holder.binding.tvDate.text = DATE_FORMAT.format(Date(txn.timestamp))

        val isPositive = txn.amount > 0
        val formatted = NUMBER_FORMAT.format(kotlin.math.abs(txn.amount))
        val signed = if (isPositive) "+$formatted" else "-$formatted"
        holder.binding.tvAmount.text = context.getString(R.string.credits_amount_format, signed)

        val amountColor = if (isPositive) R.color.credit_positive else R.color.credit_negative
        holder.binding.tvAmount.setTextColor(ContextCompat.getColor(context, amountColor))

        val iconRes = when (txn.type) {
            Transaction.TYPE_TOP_UP -> R.drawable.ic_wallet
            Transaction.TYPE_RENTAL_PAYMENT -> R.drawable.ic_car_logo
            Transaction.TYPE_RENTAL_REFUND -> R.drawable.ic_arrow_back
            Transaction.TYPE_RENTAL_INCOME -> R.drawable.ic_wallet
            else -> R.drawable.ic_wallet
        }
        holder.binding.ivTypeIcon.setImageResource(iconRes)

        val iconTint = if (isPositive) R.color.credit_positive else R.color.credit_negative
        holder.binding.ivTypeIcon.setColorFilter(ContextCompat.getColor(context, iconTint))

        holder.binding.root.setOnClickListener { onClick?.invoke(txn) }
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        private val NUMBER_FORMAT = NumberFormat.getIntegerInstance()
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem == newItem
    }
}
