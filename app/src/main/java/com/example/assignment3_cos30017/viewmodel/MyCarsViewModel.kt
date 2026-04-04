package com.example.assignment3_cos30017.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyCarsViewModel : ViewModel() {

    private val carRepository = CarRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _displayedCars = MutableLiveData<List<Car>>()
    val displayedCars: LiveData<List<Car>> = _displayedCars

    private var allCars: List<Car> = emptyList()
    var currentFilter: CarRepository.FilterMode = CarRepository.FilterMode.ALL
        private set
    private var searchQuery = ""
    private var sortMode: CarRepository.SortMode = CarRepository.SortMode.NEWEST

    init {
        if (currentUserId.isNotEmpty()) observeCars()
    }

    private fun observeCars() {
        viewModelScope.launch {
            carRepository.getCarsByOwner(currentUserId).collectLatest { cars ->
                allCars = cars
                applyFilter()
            }
        }
    }

    fun filterByStatus(mode: CarRepository.FilterMode) {
        currentFilter = mode
        applyFilter()
    }

    fun searchCars(query: String) {
        searchQuery = query
        applyFilter()
    }

    fun sortCars(mode: CarRepository.SortMode) {
        sortMode = mode
        applyFilter()
    }

    private fun applyFilter() {
        var cars = CarRepository.filterCars(allCars, currentFilter)
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            cars = cars.filter {
                it.title.lowercase().contains(q) || it.model.lowercase().contains(q)
            }
        }
        cars = CarRepository.sortCars(cars, sortMode)
        _displayedCars.value = cars
    }
}
