package com.example.assignment3_cos30017.ui.notifications

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.AppNotification
import com.example.assignment3_cos30017.data.repository.NotificationRepository
import com.example.assignment3_cos30017.databinding.FragmentNotificationsBinding
import com.example.assignment3_cos30017.ui.adapter.NotificationAdapter
import com.example.assignment3_cos30017.viewmodel.NotificationsViewModel
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationsViewModel by activityViewModels()

    private lateinit var adapter: NotificationAdapter
    private val repo = NotificationRepository()

    private var loadedOnce = false
    private var allNotifs: List<AppNotification> = emptyList()

    private var searchQuery: String = ""
    private var sortDesc: Boolean = true
    private var visibleCount: Int = PAGE_SIZE
    private var sortRotation = 0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NotificationAdapter(
            renderTitle = { renderTitle(it) },
            renderBody = { renderBody(it) },
            onClick = { notif -> showNotificationDetails(notif) }
        )
        binding.rvNotifications.layoutManager = LinearLayoutManager(context)
        binding.rvNotifications.adapter = adapter

        binding.progressLoading.visibility = View.VISIBLE
        binding.tvNoNotifications.visibility = View.GONE

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

        viewModel.notifications.observe(viewLifecycleOwner) { notifs ->
            loadedOnce = true
            binding.progressLoading.visibility = View.GONE
            allNotifs = notifs
            applySearchSortAndPagination()
        }
    }

    private fun renderTitle(n: AppNotification): String = when (n.type) {
        AppNotification.TYPE_NEW_BID ->
            getString(R.string.notif_title_new_bid, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_APPROVED_WINNER ->
            getString(R.string.notif_title_bid_won, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_APPROVED_LOSER ->
            getString(R.string.notif_title_bid_lost, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_APPROVED_OWNER ->
            getString(R.string.notif_title_bid_approved_owner, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_REJECTED_BY_OWNER ->
            getString(R.string.notif_title_bid_rejected, n.carTitle.orEmpty())
        AppNotification.TYPE_BID_CANCELLED ->
            getString(R.string.notif_title_bid_cancelled, n.carTitle.orEmpty())
        AppNotification.TYPE_BIDS_CANCELLED_BY_OWNER ->
            getString(R.string.notif_title_bids_cancelled_by_owner, n.carTitle.orEmpty())
        else -> getString(R.string.notifications_title)
    }

    private fun renderBody(n: AppNotification): String = when (n.type) {
        AppNotification.TYPE_NEW_BID -> {
            val name = n.actorName ?: getString(R.string.notif_unknown_user)
            val amount = n.amount ?: 0
            val days = n.rentalDays ?: 0
            getString(R.string.notif_body_new_bid, name, amount, days)
        }
        AppNotification.TYPE_BID_APPROVED_WINNER -> {
            val amount = n.amount ?: 0
            val days = n.rentalDays ?: 0
            getString(R.string.notif_body_bid_won, amount, days)
        }
        AppNotification.TYPE_BID_APPROVED_LOSER -> {
            val winner = n.actorName ?: getString(R.string.notif_unknown_user)
            getString(R.string.notif_body_bid_lost, winner)
        }
        AppNotification.TYPE_BID_APPROVED_OWNER -> {
            val winner = n.actorName ?: getString(R.string.notif_unknown_user)
            val amount = n.amount ?: 0
            val days = n.rentalDays ?: 0
            getString(R.string.notif_body_bid_approved_owner, winner, amount, days)
        }
        AppNotification.TYPE_BID_REJECTED_BY_OWNER -> {
            val owner = n.actorName ?: getString(R.string.notif_unknown_user)
            val amount = n.amount ?: 0
            val days = n.rentalDays ?: 0
            getString(R.string.notif_body_bid_rejected, owner, amount, days)
        }
        AppNotification.TYPE_BID_CANCELLED -> {
            val name = n.actorName ?: getString(R.string.notif_unknown_user)
            getString(R.string.notif_body_bid_cancelled, name)
        }
        AppNotification.TYPE_BIDS_CANCELLED_BY_OWNER ->
            getString(R.string.notif_body_bids_cancelled_by_owner)
        else -> ""
    }

    private fun applySearchSortAndPagination() {
        val q = searchQuery.lowercase()
        val filtered = if (q.isBlank()) allNotifs else {
            allNotifs.filter { n ->
                val haystack = (renderTitle(n) + " " + renderBody(n)).lowercase()
                haystack.contains(q)
            }
        }
        val sorted = if (sortDesc) filtered.sortedByDescending { it.createdAt } else filtered.sortedBy { it.createdAt }
        val page = sorted.take(visibleCount)

        binding.rvNotifications.visibility = if (page.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvNoNotifications.visibility = if (page.isEmpty() && loadedOnce) View.VISIBLE else View.GONE
        binding.btnLoadMore.visibility = if (sorted.size > page.size) View.VISIBLE else View.GONE
        adapter.submitList(page)
    }

    private fun showNotificationDetails(n: AppNotification) {
        // Mark read optimistically; failures can be ignored for UX.
        if (!n.read && n.id.isNotBlank()) {
            viewLifecycleOwner.lifecycleScope.launch { runCatching { repo.markRead(n.id, true) } }
        }
        NotificationDetailsBottomSheet
            .newInstance(n)
            .show(childFragmentManager, "notif_details")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}

