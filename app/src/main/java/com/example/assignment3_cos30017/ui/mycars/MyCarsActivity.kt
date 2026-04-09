package com.example.assignment3_cos30017.ui.mycars

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
import com.example.assignment3_cos30017.databinding.ActivityMyCarsBinding
import com.example.assignment3_cos30017.ui.adapter.CarGridAdapter
import com.example.assignment3_cos30017.ui.addcar.AddCarActivity
import com.example.assignment3_cos30017.ui.detail.DetailActivity
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.util.NavigationHelper
import com.example.assignment3_cos30017.util.ToolbarBadgeHelper
import com.example.assignment3_cos30017.viewmodel.ChatBadgeViewModel
import com.example.assignment3_cos30017.viewmodel.DetailViewModel
import com.example.assignment3_cos30017.viewmodel.MyCarsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class MyCarsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyCarsBinding
    private val viewModel: MyCarsViewModel by viewModels()
    private val chatBadgeViewModel: ChatBadgeViewModel by viewModels()
    private var chatBadgeViews: ToolbarBadgeHelper.BadgeViews? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyCarsBinding.inflate(layoutInflater)
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

        setupSearch()
        setupFilterChips()
        setupSort()
        setupRecyclerView()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, AddCarActivity::class.java))
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
        val chipToFilter = mapOf(
            R.id.chip_all to CarRepository.FilterMode.ALL,
            R.id.chip_unlisted to CarRepository.FilterMode.UNLISTED,
            R.id.chip_listed to CarRepository.FilterMode.LISTED,
            R.id.chip_rented to CarRepository.FilterMode.RENTED
        )

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_all
            val mode = chipToFilter[checkedId] ?: CarRepository.FilterMode.ALL
            viewModel.filterByStatus(mode)
        }
    }

    private fun setupSort() {
        binding.btnSort.visibility = android.view.View.GONE
    }

    private fun setupRecyclerView() {
        val adapter = CarGridAdapter(
            onCarClick = { car ->
                startActivity(Intent(this, DetailActivity::class.java).apply {
                    putExtra(DetailViewModel.EXTRA_CAR_ID, car.id)
                    putExtra(DetailViewModel.EXTRA_ENTRY_CONTEXT, DetailViewModel.CONTEXT_MY_CARS)
                })
            },
            onFavouriteClick = {},
            showPrice = false,
            garageMode = true
        )

        val spanCount = (resources.configuration.screenWidthDp / 180).coerceIn(2, 4)
        binding.rvCars.layoutManager = GridLayoutManager(this, spanCount)
        binding.rvCars.itemAnimator = null
        binding.rvCars.adapter = adapter

        viewModel.displayedCars.observe(this) { cars ->
            adapter.submitList(cars)
            binding.rvCars.visibility = if (cars.isNotEmpty()) View.VISIBLE else View.GONE
            if (cars.isEmpty()) {
                binding.tvEmpty.text = getEmptyMessage()
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
            }
        }
    }

    private fun getEmptyMessage(): String = when (viewModel.currentFilter) {
        CarRepository.FilterMode.ALL -> getString(R.string.no_cars_in_garage)
        CarRepository.FilterMode.UNLISTED -> getString(R.string.empty_unlisted)
        CarRepository.FilterMode.LISTED -> getString(R.string.empty_listed)
        CarRepository.FilterMode.RENTED -> getString(R.string.empty_rented)
    }
}
