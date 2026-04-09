package com.example.assignment3_cos30017.ui.rentals

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.databinding.ActivityMyRentalsBinding
import com.example.assignment3_cos30017.ui.adapter.BookingAdapter
import com.example.assignment3_cos30017.ui.detail.DetailActivity
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.util.NavigationHelper
import com.example.assignment3_cos30017.util.ToolbarBadgeHelper
import com.example.assignment3_cos30017.viewmodel.ChatBadgeViewModel
import com.example.assignment3_cos30017.viewmodel.DetailViewModel
import com.example.assignment3_cos30017.viewmodel.MyRentalsActivityViewModel

class MyRentalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyRentalsBinding
    private val viewModel: MyRentalsActivityViewModel by viewModels()
    private val chatBadgeViewModel: ChatBadgeViewModel by viewModels()
    private var chatBadgeViews: ToolbarBadgeHelper.BadgeViews? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyRentalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        NavigationHelper.setup(this, binding.toolbar)
        chatBadgeViews = ToolbarBadgeHelper.bindActionIconWithBadge(
            activity = this,
            toolbar = binding.toolbar,
            menuItemId = R.id.action_chat,
            iconRes = R.drawable.ic_chat,
            onClick = { /* handled by NavigationHelper */ }
        )

        val adapter = BookingAdapter { booking ->
            startActivity(Intent(this, DetailActivity::class.java).apply {
                putExtra(DetailActivity.EXTRA_CAR_ID, booking.carId)
                putExtra(DetailActivity.EXTRA_ENTRY_CONTEXT, DetailViewModel.CONTEXT_MY_RENTALS)
                putExtra(DetailActivity.EXTRA_BOOKING_ID, booking.id)
            })
        }

        binding.rvRentals.layoutManager = LinearLayoutManager(this)
        binding.rvRentals.itemAnimator = null
        binding.rvRentals.adapter = adapter

        setupSearch()
        setupFilterChips()
        setupSort()

        viewModel.displayedBookings.observe(this) { bookings ->
            adapter.submitList(bookings)
            binding.rvRentals.visibility = if (bookings.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvNoRentals.visibility = if (bookings.isEmpty()) View.VISIBLE else View.GONE
        }

        chatBadgeViewModel.unreadConversations.observe(this) { count ->
            ToolbarBadgeHelper.renderCount(chatBadgeViews, count)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { viewModel.searchBookings(s?.toString() ?: "") }
        })
    }

    private fun setupFilterChips() {
        binding.chipAll.isChecked = true
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when (checkedIds.firstOrNull()) {
                R.id.chip_active -> MyRentalsActivityViewModel.FilterMode.ACTIVE
                R.id.chip_completed -> MyRentalsActivityViewModel.FilterMode.COMPLETED
                R.id.chip_cancelled -> MyRentalsActivityViewModel.FilterMode.CANCELLED
                else -> MyRentalsActivityViewModel.FilterMode.ALL
            }
            viewModel.filterByStatus(mode)
        }
    }

    private fun setupSort() {
        binding.btnSort.setOnClickListener {
            viewModel.toggleSort()
        }
    }
}
