package com.example.assignment3_cos30017.ui.wallet

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.core.content.getSystemService
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.network.SePayConfig
import com.example.assignment3_cos30017.viewmodel.WalletViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class TopUpDialogFragment : BottomSheetDialogFragment() {

    private val walletViewModel: WalletViewModel by activityViewModels()
    private var pollingTimer: CountDownTimer? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(R.layout.dialog_top_up)

        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (sheet != null) {
                val behavior = BottomSheetBehavior.from(sheet)
                sheet.layoutParams = sheet.layoutParams.apply {
                    height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                }
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = false
            }
        }

        // Make bottom sheet resize with keyboard so important info isn't covered.
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        dialog.setOnDismissListener {
            walletViewModel.cancelPolling()
            pollingTimer?.cancel()
            pollingTimer = null
        }

        setupUi(dialog)
        return dialog
    }

    private fun setupUi(dialog: BottomSheetDialog) {
        var selectedAmount = 0
        var currentQrUrl: String? = null
        var currentReference: String? = null

        val scrollRoot = dialog.findViewById<NestedScrollView>(R.id.scroll_root)
        val btnClose = dialog.findViewById<ImageView>(R.id.btn_close)
        val tilCustom = dialog.findViewById<TextInputLayout>(R.id.til_custom_amount)!!
        val etCustom = dialog.findViewById<TextInputEditText>(R.id.et_custom_amount)!!
        val cardVnd = dialog.findViewById<View>(R.id.card_vnd_equivalent)!!
        val tvVnd = dialog.findViewById<TextView>(R.id.tv_vnd_equivalent)!!
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btn_generate_qr)!!
        val layoutForm = dialog.findViewById<LinearLayout>(R.id.layout_form)!!

        val layoutPayment = dialog.findViewById<LinearLayout>(R.id.layout_payment)!!
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progress_payment)!!
        val tvStatus = dialog.findViewById<TextView>(R.id.tv_payment_status)!!
        val ivQr = dialog.findViewById<ImageView>(R.id.iv_qr_code)!!
        val btnOpenBanking = dialog.findViewById<MaterialButton>(R.id.btn_open_banking)!!
        val btnRetry = dialog.findViewById<MaterialButton>(R.id.btn_retry_check)!!
        // Removed fullscreen QR button per UX request
        val tvTransferAmount = dialog.findViewById<TextView>(R.id.tv_transfer_amount)!!
        val tvTransferAccount = dialog.findViewById<TextView>(R.id.tv_transfer_account)!!
        val tvTransferMemo = dialog.findViewById<TextView>(R.id.tv_transfer_memo)!!
        val btnCopyMemo = dialog.findViewById<MaterialButton>(R.id.btn_copy_memo)!!
        val btnCopyAccount = dialog.findViewById<MaterialButton>(R.id.btn_copy_account)!!

        btnClose?.setOnClickListener { dismiss() }

        etCustom.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = v.context.getSystemService<InputMethodManager>()
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
                true
            } else false
        }

        etCustom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim() ?: ""
                tilCustom.error = null
                selectedAmount = text.toIntOrNull() ?: 0
                if (selectedAmount > 0) {
                    val vnd = selectedAmount * SePayConfig.CREDIT_TO_VND.toLong()
                    tvVnd.text = getString(R.string.credit_to_vnd_format, selectedAmount, getString(R.string.credits_suffix), vnd)
                    cardVnd.visibility = View.VISIBLE
                    // Ensure the "credit -> VND" line is visible above keyboard.
                    scrollRoot?.post { scrollRoot.smoothScrollTo(0, cardVnd.top) }
                } else {
                    cardVnd.visibility = View.GONE
                }
            }
        })

        btnConfirm.setOnClickListener {
            if (selectedAmount <= 0) {
                tilCustom.error = getString(R.string.invalid_amount)
                return@setOnClickListener
            }

            // Hide keyboard immediately on submit.
            val imm = requireContext().getSystemService<InputMethodManager>()
            imm?.hideSoftInputFromWindow(etCustom.windowToken, 0)
            etCustom.clearFocus()

            val reference = SePayConfig.generateReference()
            currentReference = reference

            // Switch to payment-only UI
            layoutForm.visibility = View.GONE
            layoutPayment.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            btnRetry.visibility = View.GONE
            tvStatus.text = getString(R.string.waiting_payment)

            val qrUrl = SePayConfig.generateQrUrl(selectedAmount, reference)
            currentQrUrl = qrUrl
            Glide.with(this).load(qrUrl).into(ivQr)

            val vnd = selectedAmount * SePayConfig.CREDIT_TO_VND
            tvTransferAmount.text = getString(R.string.vnd_equivalent_format, vnd.toLong()).replace("=", "").trim()
            tvTransferAccount.text = getString(
                R.string.sepay_transfer_account_format,
                SePayConfig.BANK_ID,
                SePayConfig.ACCOUNT_NUMBER,
                SePayConfig.ACCOUNT_NAME
            )
            tvTransferMemo.text = reference

            fun copy(label: String, text: String) {
                val clipboard = requireContext().getSystemService<ClipboardManager>()
                clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
                val anchor = dialog.findViewById<View>(android.R.id.content) ?: dialog.window?.decorView ?: scrollRoot
                if (anchor != null) Snackbar.make(anchor, R.string.copied, Snackbar.LENGTH_SHORT).show()
            }

            btnCopyMemo.setOnClickListener { copy("memo", reference) }
            btnCopyAccount.setOnClickListener { copy("account", SePayConfig.ACCOUNT_NUMBER) }

            // Open app chooser (with icons). Fallback: fullscreen QR.
            btnOpenBanking.setOnClickListener {
                val pm = requireContext().packageManager
                val options = BANK_APP_PACKAGES.mapNotNull { pkg ->
                    pm.getLaunchIntentForPackage(pkg)?.let { intent ->
                        val label = runCatching {
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            pm.getApplicationLabel(appInfo).toString()
                        }.getOrNull() ?: pkg
                        val icon = runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
                        AppOption(label, intent, icon)
                    }
                }.sortedBy { it.label.lowercase() }

                if (options.isEmpty()) {
                    val anchor = dialog.findViewById<View>(android.R.id.content) ?: dialog.window?.decorView ?: scrollRoot
                    if (anchor != null) Snackbar.make(anchor, R.string.no_banking_app, Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val adapter = object : ArrayAdapter<AppOption>(
                    requireContext(),
                    R.layout.item_app_option,
                    options
                ) {
                    override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                        val row = convertView ?: layoutInflater.inflate(R.layout.item_app_option, parent, false)
                        val iconView = row.findViewById<ImageView>(R.id.iv_app_icon)
                        val nameView = row.findViewById<TextView>(R.id.tv_app_name)
                        val opt = getItem(position)!!
                        nameView.text = opt.label
                        iconView.setImageDrawable(opt.icon)
                        return row
                    }
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.open_banking_app)
                    .setAdapter(adapter) { _, which ->
                        try {
                            startActivity(options[which].intent)
                        } catch (_: ActivityNotFoundException) {
                            val anchor = dialog.findViewById<View>(android.R.id.content) ?: dialog.window?.decorView ?: scrollRoot
                            if (anchor != null) Snackbar.make(anchor, R.string.no_banking_app, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            // Show countdown (10 min)
            val totalMs = WalletViewModel.POLL_MAX_ATTEMPTS * WalletViewModel.POLL_INTERVAL_MS
            pollingTimer?.cancel()
            pollingTimer = object : CountDownTimer(totalMs, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    val s = (millisUntilFinished / 1000L).toInt()
                    tvStatus.text = getString(R.string.waiting_payment_with_timer, s)
                }
                override fun onFinish() {}
            }.start()

            walletViewModel.confirmTopUp(selectedAmount, reference)
        }

        btnRetry.setOnClickListener {
            val ref = currentReference ?: return@setOnClickListener
            progressBar.visibility = View.VISIBLE
            tvStatus.text = getString(R.string.waiting_payment)
            walletViewModel.confirmTopUp(selectedAmount, ref)
        }

        walletViewModel.topUpResult.observe(this) { state ->
            if (state == null) return@observe

            when (state) {
                is WalletViewModel.TopUpState.Polling -> {
                    progressBar.visibility = View.VISIBLE
                }
                is WalletViewModel.TopUpState.Success -> {
                    progressBar.visibility = View.GONE
                    tvStatus.text = getString(R.string.top_up_success, state.credits)
                    val anchor = dialog.findViewById<View>(android.R.id.content) ?: dialog.window?.decorView ?: scrollRoot
                    if (anchor != null) Snackbar.make(anchor, getString(R.string.top_up_success, state.credits), Snackbar.LENGTH_LONG).show()
                    walletViewModel.clearTopUpResult()
                    pollingTimer?.cancel()
                    pollingTimer = null
                    dismiss()
                }
                is WalletViewModel.TopUpState.Timeout -> {
                    progressBar.visibility = View.GONE
                    tvStatus.text = getString(R.string.payment_timeout)
                    walletViewModel.clearTopUpResult()
                    pollingTimer?.cancel()
                    pollingTimer = null
                    val anchor = dialog.findViewById<View>(android.R.id.content) ?: dialog.window?.decorView ?: scrollRoot
                    if (anchor != null) Snackbar.make(anchor, R.string.payment_timeout, Snackbar.LENGTH_LONG).show()
                    btnRetry.visibility = View.VISIBLE
                }
                is WalletViewModel.TopUpState.Error -> {
                    progressBar.visibility = View.GONE
                    tvStatus.text = getString(R.string.payment_error)
                    walletViewModel.clearTopUpResult()
                    pollingTimer?.cancel()
                    pollingTimer = null
                    val anchor = dialog.findViewById<View>(android.R.id.content) ?: dialog.window?.decorView ?: scrollRoot
                    if (anchor != null) Snackbar.make(anchor, R.string.payment_error, Snackbar.LENGTH_LONG).show()
                    btnRetry.visibility = View.VISIBLE
                }
            }
        }
    }

    private data class AppOption(val label: String, val intent: Intent, val icon: android.graphics.drawable.Drawable?)

    companion object {
        private val BANK_APP_PACKAGES = listOf(
            "com.vietinbank.ipay",
            "com.vietcombank.vcbmobilebanking",
            "com.mbmobile",
            "com.bidv.smartbanking",
            "vn.com.techcombank.bb.app",
            "com.tpb.mb.gprsandroid",
            "com.mservice.momotransfer",
            "vn.zalopay.app"
        )
    }
}

