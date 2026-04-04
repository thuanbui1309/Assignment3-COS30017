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

class MyListingsViewModel : ViewModel() {

    private val carRepository = CarRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _displayedCars = MutableLiveData<List<Car>>()
    val displayedCars: LiveData<List<Car>> = _displayedCars

    private var allCars: List<Car> = emptyList()
    private var searchQuery = ""
    private var filterMode: ListingFilterMode = ListingFilterMode.ALL
    private var sortMode: CarRepository.SortMode = CarRepository.SortMode.NEWEST

    enum class ListingFilterMode { ALL, LISTED_ONLY, RENTED_ONLY }

    init {
        if (currentUserId.isNotEmpty()) observeCars()
    }

    private fun observeCars() {
        viewModelScope.launch {
            carRepository.getCarsByOwner(currentUserId).collectLatest { cars ->
                allCars = cars.filter {
                    it.effectiveStatus == Car.STATUS_LISTED || it.effectiveStatus == Car.STATUS_RENTED
                }
                applyFilter()
            }
        }
    }

    fun searchCars(query: String) {
        searchQuery = query
        applyFilter()
    }

    fun filterByStatus(mode: ListingFilterMode) {
        filterMode = mode
        applyFilter()
    }

    fun sortCars(mode: CarRepository.SortMode) {
        sortMode = mode
        applyFilter()
    }

    private fun applyFilter() {
        var cars = when (filterMode) {
            ListingFilterMode.ALL -> allCars
            ListingFilterMode.LISTED_ONLY -> allCars.filter { it.effectiveStatus == Car.STATUS_LISTED }
            ListingFilterMode.RENTED_ONLY -> allCars.filter { it.effectiveStatus == Car.STATUS_RENTED }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            cars = cars.filter {
                it.title.lowercase().contains(q) || it.model.lowercase().contains(q)
            }
        }
        cars = CarRepository.sortCars(cars, sortMode)
        _displayedCars.postValue(cars)
    }
}
