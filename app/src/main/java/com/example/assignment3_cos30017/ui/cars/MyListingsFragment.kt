package com.example.assignment3_cos30017.ui.cars

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.databinding.FragmentMyListingsBinding
import com.example.assignment3_cos30017.ui.adapter.CarGridAdapter
import com.example.assignment3_cos30017.ui.addcar.AddCarActivity
import com.example.assignment3_cos30017.ui.detail.DetailActivity
import com.example.assignment3_cos30017.viewmodel.DetailViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MyListingsFragment : Fragment() {

    private var _binding: FragmentMyListingsBinding? = null
    private val binding get() = _binding!!
    private val carRepository = CarRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyListingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CarGridAdapter(
            onCarClick = { car ->
                startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra(DetailViewModel.EXTRA_CAR_ID, car.id)
                    putExtra(DetailViewModel.EXTRA_ENTRY_CONTEXT, DetailViewModel.CONTEXT_MY_LISTINGS)
                })
            },
            onFavouriteClick = {},
            showFavourite = false
        )

        val spanCount = (resources.configuration.screenWidthDp / 180).coerceIn(2, 4)
        binding.rvListings.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.rvListings.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            carRepository.getCarsByOwner(uid)
                .map { cars ->
                    cars.filter {
                        it.effectiveStatus == Car.STATUS_LISTED || it.effectiveStatus == Car.STATUS_RENTED
                    }
                }
                .collectLatest { cars ->
                    adapter.submitList(cars)
                    binding.rvListings.visibility = if (cars.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.tvEmpty.visibility = if (cars.isEmpty()) View.VISIBLE else View.GONE
                }
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddCarActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
