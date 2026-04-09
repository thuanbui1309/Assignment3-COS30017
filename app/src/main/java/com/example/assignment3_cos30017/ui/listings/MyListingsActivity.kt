package com.example.assignment3_cos30017.ui.listings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.databinding.ActivityMyListingsBinding
import com.example.assignment3_cos30017.ui.adapter.CarGridAdapter
import com.example.assignment3_cos30017.ui.detail.DetailActivity
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.util.NavigationHelper
import com.example.assignment3_cos30017.util.ToolbarBadgeHelper
import com.example.assignment3_cos30017.viewmodel.ChatBadgeViewModel
import com.example.assignment3_cos30017.viewmodel.DetailViewModel
import com.example.assignment3_cos30017.viewmodel.MyListingsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class MyListingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyListingsBinding
    private val viewModel: MyListingsViewModel by viewModels()
    private val chatBadgeViewModel: ChatBadgeViewModel by viewModels()
    private var chatBadgeViews: ToolbarBadgeHelper.BadgeViews? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyListingsBinding.inflate(layoutInflater)
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

        val adapter = CarGridAdapter(
            onCarClick = { car ->
                startActivity(Intent(this, DetailActivity::class.java).apply {
                    putExtra(DetailViewModel.EXTRA_CAR_ID, car.id)
                    putExtra(DetailViewModel.EXTRA_ENTRY_CONTEXT, DetailViewModel.CONTEXT_MY_LISTINGS)
                })
            },
            onFavouriteClick = {},
            showFavourite = false
        )

        val spanCount = (resources.configuration.screenWidthDp / 180).coerceIn(2, 4)
        binding.rvListings.layoutManager = GridLayoutManager(this, spanCount)
        binding.rvListings.adapter = adapter

        setupSearch()
        setupFilterChips()
        setupSort()

        viewModel.displayedCars.observe(this) { cars ->
            adapter.submitList(cars)
            binding.rvListings.visibility = if (cars.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvNoListings.visibility = if (cars.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, com.example.assignment3_cos30017.ui.addcar.AddCarActivity::class.java))
        }

        chatBadgeViewModel.unreadConversations.observe(this) { count ->
            ToolbarBadgeHelper.renderCount(chatBadgeViews, count)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { viewModel.searchCars(s?.toString() ?: "") }
        })
    }

    private fun setupFilterChips() {
        binding.chipAll.isChecked = true
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when (checkedIds.firstOrNull()) {
                R.id.chip_listed -> MyListingsViewModel.ListingFilterMode.LISTED_ONLY
                R.id.chip_rented -> MyListingsViewModel.ListingFilterMode.RENTED_ONLY
                else -> MyListingsViewModel.ListingFilterMode.ALL
            }
            viewModel.filterByStatus(mode)
        }
    }

    private fun setupSort() {
        binding.btnSort.visibility = android.view.View.GONE
    }
}
