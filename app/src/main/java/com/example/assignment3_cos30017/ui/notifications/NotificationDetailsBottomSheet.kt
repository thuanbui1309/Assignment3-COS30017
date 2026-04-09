package com.example.assignment3_cos30017.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.AppNotification
import com.example.assignment3_cos30017.data.repository.NotificationRepository
import com.example.assignment3_cos30017.databinding.BottomSheetNotificationDetailsBinding
import com.example.assignment3_cos30017.ui.detail.DetailActivity
import com.example.assignment3_cos30017.viewmodel.DetailViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNotificationDetailsBinding? = null
    private val binding get() = _binding!!

    private val repo = NotificationRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetNotificationDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val n = requireArguments().getParcelable(ARG_NOTIFICATION) as? AppNotification
            ?: run { dismissAllowingStateLoss(); return }

        val (title, body) = NotificationTextRenderer(requireContext()).render(n)

        binding.tvTitle.text = title
        binding.tvBody.text = body
        binding.tvTime.text = DATE_FORMAT.format(Date(n.createdAt))

        val iconRes = when (n.type) {
            AppNotification.TYPE_NEW_BID -> R.drawable.ic_car_logo
            AppNotification.TYPE_BID_APPROVED_WINNER -> R.drawable.ic_check
            AppNotification.TYPE_BID_APPROVED_LOSER -> R.drawable.ic_close
            AppNotification.TYPE_BID_APPROVED_OWNER -> R.drawable.ic_check
            AppNotification.TYPE_BID_REJECTED_BY_OWNER -> R.drawable.ic_close
            AppNotification.TYPE_BID_CANCELLED -> R.drawable.ic_close
            AppNotification.TYPE_BIDS_CANCELLED_BY_OWNER -> R.drawable.ic_close
            else -> R.drawable.ic_notifications
        }
        binding.ivIcon.setImageResource(iconRes)
        val tint = if (!n.read) R.color.accent else R.color.text_secondary
        binding.ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), tint))

        // Meta rows (richer details)
        renderMeta(binding.layoutMeta, n)

        if (!n.read && n.id.isNotBlank()) {
            viewLifecycleOwner.lifecycleScope.launch { runCatching { repo.markRead(n.id, true) } }
        }

        val canOpenCar = !n.carId.isNullOrBlank()
        if (canOpenCar) {
            binding.btnOpen.visibility = View.VISIBLE
            binding.btnOpen.setOnClickListener {
                startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_CAR_ID, n.carId)
                    putExtra(DetailActivity.EXTRA_ENTRY_CONTEXT, DetailViewModel.CONTEXT_MARKETPLACE)
                })
                dismiss()
            }
        }

        if (n.read && n.id.isNotBlank()) {
            binding.btnMarkUnread.visibility = View.VISIBLE
            binding.btnMarkUnread.text = getString(R.string.mark_as_unread)
            binding.btnMarkUnread.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { repo.markRead(n.id, false) }
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderMeta(container: ViewGroup, n: AppNotification) {
        container.removeAllViews()
        fun addRow(label: String, value: String) {
            val tv = TextView(requireContext()).apply {
                text = getString(R.string.sheet_kv_format, label, value)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                textSize = 13f
            }
            container.addView(tv)
        }

        n.carTitle?.takeIf { it.isNotBlank() }?.let { addRow(getString(R.string.sheet_label_car), it) }
        n.actorName?.takeIf { it.isNotBlank() }?.let { addRow(getString(R.string.sheet_label_actor), it) }
        n.amount?.let { addRow(getString(R.string.sheet_label_amount), getString(R.string.credits_format, it)) }
        n.rentalDays?.let { addRow(getString(R.string.sheet_label_days), resources.getQuantityString(R.plurals.sheet_days, it, it)) }
        n.bidId?.takeIf { it.isNotBlank() }?.let { addRow(getString(R.string.sheet_label_reference), it) }
    }

    companion object {
        private const val ARG_NOTIFICATION = "arg_notification"
        private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        fun newInstance(notification: AppNotification): NotificationDetailsBottomSheet {
            return NotificationDetailsBottomSheet().apply {
                arguments = Bundle().apply { putParcelable(ARG_NOTIFICATION, notification) }
            }
        }
    }
}

