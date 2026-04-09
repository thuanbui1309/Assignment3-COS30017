package com.example.assignment3_cos30017.ui.wallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Transaction
import com.example.assignment3_cos30017.databinding.FragmentWalletBinding
import com.example.assignment3_cos30017.ui.adapter.TransactionAdapter
import com.example.assignment3_cos30017.viewmodel.WalletViewModel

class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!
    private val walletViewModel: WalletViewModel by activityViewModels()

    private lateinit var adapter: TransactionAdapter
    private var loadedOnce = false
    private var allTxns: List<Transaction> = emptyList()

    private var searchQuery: String = ""
    private var sortDesc: Boolean = true
    private var visibleCount: Int = PAGE_SIZE
    private var sortRotation = 0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        walletViewModel.balance.observe(viewLifecycleOwner) { balance ->
            binding.tvBalance.text = getString(R.string.credits_format, balance)
        }

        binding.btnTopUp.setOnClickListener {
            TopUpDialogFragment().show(childFragmentManager, "topUp")
        }

        adapter = TransactionAdapter(onClick = { txn -> showTransactionDetails(txn) })
        binding.rvTransactions.layoutManager = LinearLayoutManager(context)
        binding.rvTransactions.adapter = adapter

        binding.progressLoading.visibility = View.VISIBLE
        binding.tvNoTransactions.visibility = View.GONE

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim().orEmpty()
                visibleCount = PAGE_SIZE
                applySearchSortAndPagination()
            }
        })

        binding.btnSort.setOnClickListener {
            sortDesc = !sortDesc
            sortRotation += 180f
            binding.btnSort.animate()
                .rotation(sortRotation)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
            visibleCount = PAGE_SIZE
            applySearchSortAndPagination()
        }

        binding.btnLoadMore.setOnClickListener {
            visibleCount += PAGE_SIZE
            applySearchSortAndPagination()
        }

        walletViewModel.transactions.observe(viewLifecycleOwner) { txns ->
            loadedOnce = true
            binding.progressLoading.visibility = View.GONE
            allTxns = txns
            applySearchSortAndPagination()
        }
    }

    private fun applySearchSortAndPagination() {
        val q = searchQuery.lowercase()
        val filtered = if (q.isBlank()) allTxns else {
            allTxns.filter { txn ->
                txn.description.lowercase().contains(q) ||
                    txn.type.lowercase().contains(q) ||
                    (txn.referenceId?.lowercase()?.contains(q) == true)
            }
        }
        val sorted = if (sortDesc) filtered.sortedByDescending { it.timestamp } else filtered.sortedBy { it.timestamp }
        val page = sorted.take(visibleCount)

        binding.rvTransactions.visibility = if (page.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvNoTransactions.visibility = if (page.isEmpty() && loadedOnce) View.VISIBLE else View.GONE
        binding.btnLoadMore.visibility = if (sorted.size > page.size) View.VISIBLE else View.GONE
        adapter.submitList(page)
    }

    private fun showTransactionDetails(txn: Transaction) {
        TransactionDetailsBottomSheet
            .newInstance(txn)
            .show(childFragmentManager, "txn_details")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}

