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
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.databinding.FragmentMyCarsBinding
import com.example.assignment3_cos30017.ui.adapter.CarGridAdapter
import com.example.assignment3_cos30017.ui.addcar.AddCarActivity
import com.example.assignment3_cos30017.ui.detail.DetailActivity
import com.example.assignment3_cos30017.viewmodel.DetailViewModel
import com.example.assignment3_cos30017.viewmodel.MyCarsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class MyCarsFragment : Fragment() {

    private var _binding: FragmentMyCarsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyCarsViewModel by viewModels()
    private lateinit var carGridAdapter: CarGridAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyCarsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        carGridAdapter = CarGridAdapter(
            onCarClick = { car ->
                startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra(DetailViewModel.EXTRA_CAR_ID, car.id)
                    putExtra(DetailViewModel.EXTRA_ENTRY_CONTEXT, DetailViewModel.CONTEXT_MY_CARS)
                })
            },
            onFavouriteClick = {},
            showPrice = false,
            garageMode = true,
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
            binding.rvCars.visibility = if (cars.isNotEmpty()) View.VISIBLE else View.GONE
            if (cars.isEmpty()) {
                binding.tvEmpty.text = when (viewModel.currentFilter) {
                    CarRepository.FilterMode.ALL -> getString(R.string.no_cars_in_garage)
                    CarRepository.FilterMode.UNLISTED -> getString(R.string.empty_unlisted)
                    CarRepository.FilterMode.LISTED -> getString(R.string.empty_listed)
                    CarRepository.FilterMode.RENTED -> getString(R.string.empty_rented)
                }
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
            }
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddCarActivity::class.java))
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
                R.id.chip_unlisted -> CarRepository.FilterMode.UNLISTED
                R.id.chip_listed -> CarRepository.FilterMode.LISTED
                R.id.chip_rented -> CarRepository.FilterMode.RENTED
                else -> CarRepository.FilterMode.ALL
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
