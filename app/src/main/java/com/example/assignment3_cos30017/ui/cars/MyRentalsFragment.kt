package com.example.assignment3_cos30017.ui.cars

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.databinding.FragmentMyRentalsBinding
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.ui.adapter.CarGridAdapter
import com.example.assignment3_cos30017.ui.detail.DetailActivity
import com.example.assignment3_cos30017.viewmodel.DetailViewModel
import com.example.assignment3_cos30017.viewmodel.MyRentalsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class MyRentalsFragment : Fragment() {

    private var _binding: FragmentMyRentalsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyRentalsViewModel by viewModels()
    private lateinit var carGridAdapter: CarGridAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyRentalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carGridAdapter = CarGridAdapter(
            onCarClick = { car ->
                val ctx = viewModel.carContextMap.value?.get(car.id)
                if (ctx?.type == MyRentalsViewModel.ItemType.BOOKING) {
                    startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                        putExtra(DetailActivity.EXTRA_CAR_ID, car.id)
                        putExtra(DetailActivity.EXTRA_ENTRY_CONTEXT, DetailViewModel.CONTEXT_MY_RENTALS)
                        putExtra(DetailActivity.EXTRA_BOOKING_ID, ctx.bookingId)
                    })
                } else {
                    startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                        putExtra(DetailActivity.EXTRA_CAR_ID, car.id)
                        putExtra(DetailActivity.EXTRA_ENTRY_CONTEXT, DetailViewModel.CONTEXT_MARKETPLACE)
                    })
                }
            },
            onFavouriteClick = {},
            showPrice = true,
            garageMode = false,
            showFavourite = false
        )

        val spanCount = (resources.configuration.screenWidthDp / 180).coerceIn(2, 4)
        binding.rvCars.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.rvCars.adapter = carGridAdapter

        setupSearch()
        setupFilterChips()
        setupSort()

        viewModel.displayedCars.observe(viewLifecycleOwner) { cars ->
            carGridAdapter.submitList(cars)

            // Update status overrides based on context map
            val ctxMap = viewModel.carContextMap.value ?: emptyMap()
            val overrides = mutableMapOf<String, String>()
            val drawables = mutableMapOf<String, Int>()
            for (car in cars) {
                val ctx = ctxMap[car.id]
                if (ctx?.type == MyRentalsViewModel.ItemType.BID) {
                    overrides[car.id] = getString(R.string.tab_bidding)
                    drawables[car.id] = R.drawable.bg_rented_badge
                } else if (ctx?.type == MyRentalsViewModel.ItemType.BOOKING) {
                    overrides[car.id] = getString(R.string.tab_renting)
                    drawables[car.id] = R.drawable.bg_badge_green
                }
            }
            carGridAdapter.updateStatusOverrides(overrides, drawables)

            binding.rvCars.visibility = if (cars.isNotEmpty()) View.VISIBLE else View.GONE
            if (cars.isEmpty()) {
                binding.tvEmpty.text = when (viewModel.currentFilter) {
                    MyRentalsViewModel.RentalFilterMode.ALL -> getString(R.string.empty_rentals_all)
                    MyRentalsViewModel.RentalFilterMode.BIDDING -> getString(R.string.empty_bidding)
                    MyRentalsViewModel.RentalFilterMode.RENTING -> getString(R.string.empty_renting)
                }
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
            }
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
        binding.chipRentalsAll.isChecked = true
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when (checkedIds.firstOrNull()) {
                R.id.chip_bidding -> MyRentalsViewModel.RentalFilterMode.BIDDING
                R.id.chip_renting -> MyRentalsViewModel.RentalFilterMode.RENTING
                else -> MyRentalsViewModel.RentalFilterMode.ALL
            }
            viewModel.filterByStatus(mode)
        }
    }

    private fun setupSort() {
        binding.btnSort.visibility = android.view.View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
