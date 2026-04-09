package com.example.assignment3_cos30017.ui.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Transaction
import com.example.assignment3_cos30017.databinding.BottomSheetTransactionDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTransactionDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetTransactionDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val txn = requireArguments().getParcelable(ARG_TXN) as? Transaction
            ?: run { dismissAllowingStateLoss(); return }

        binding.tvTitle.text = txn.description
        binding.tvSubtitle.text = DATE_FORMAT.format(Date(txn.timestamp))

        val isPositive = txn.amount > 0
        val formatted = NUMBER_FORMAT.format(kotlin.math.abs(txn.amount))
        val signed = if (isPositive) "+$formatted" else "-$formatted"
        binding.tvAmount.text = getString(R.string.credits_amount_format, signed)

        val amountColor = if (isPositive) R.color.credit_positive else R.color.credit_negative
        binding.tvAmount.setTextColor(ContextCompat.getColor(requireContext(), amountColor))

        renderMeta(binding.layoutMeta, txn)

        val ref = txn.referenceId?.takeIf { it.isNotBlank() }
        if (ref != null) {
            binding.btnCopyReference.visibility = View.VISIBLE
            binding.btnCopyReference.text = getString(R.string.copy_reference)
            binding.btnCopyReference.setOnClickListener { copyToClipboard(ref) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderMeta(container: ViewGroup, txn: Transaction) {
        container.removeAllViews()
        fun addRow(label: String, value: String) {
            val tv = TextView(requireContext()).apply {
                text = getString(R.string.sheet_kv_format, label, value)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                textSize = 13f
            }
            container.addView(tv)
        }

        addRow(getString(R.string.sheet_label_type), txn.type)
        txn.referenceId?.takeIf { it.isNotBlank() }?.let { addRow(getString(R.string.sheet_label_reference), it) }
        addRow(getString(R.string.sheet_label_balance_after), getString(R.string.credits_format, txn.balanceAfter))
    }

    private fun copyToClipboard(value: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("reference", value))
        Snackbar.make(binding.root, getString(R.string.copied), Snackbar.LENGTH_SHORT).show()
    }

    companion object {
        private const val ARG_TXN = "arg_txn"
        private val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        private val NUMBER_FORMAT = NumberFormat.getIntegerInstance()

        fun newInstance(txn: Transaction): TransactionDetailsBottomSheet {
            return TransactionDetailsBottomSheet().apply {
                arguments = Bundle().apply { putParcelable(ARG_TXN, txn) }
            }
        }
    }
}

