package com.example.assignment3_cos30017.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.data.repository.FavouriteRepository
import com.example.assignment3_cos30017.data.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CarViewModel : ViewModel() {

    private val carRepository = CarRepository()
    private val walletRepository = WalletRepository()
    private val favouriteRepository = FavouriteRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _displayedCars = MutableLiveData<List<Car>>()
    val displayedCars: LiveData<List<Car>> = _displayedCars

    private val _creditBalance = MutableLiveData<Int>()
    val creditBalance: LiveData<Int> = _creditBalance

    private val _favouriteIds = MutableLiveData<Set<String>>(emptySet())
    val favouriteIds: LiveData<Set<String>> = _favouriteIds

    private var allListedCars: List<Car> = emptyList()
    private var currentSearchQuery = ""
    private var currentSortMode: CarRepository.SortMode? = null
    private var filterMode: FilterMode = FilterMode.ALL

    enum class FilterMode { ALL, MY_CARS, FAVOURITES }

    init {
        observeCars()
        observeBalance()
        observeFavourites()
    }

    private fun observeCars() {
        viewModelScope.launch {
            carRepository.getAllCars().collectLatest { cars ->
                allListedCars = cars.filter { it.effectiveStatus == Car.STATUS_LISTED }
                applyFilters()
            }
        }
    }

    private fun observeBalance() {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            walletRepository.getBalance(currentUserId).collectLatest { balance ->
                _creditBalance.value = balance
            }
        }
    }

    private fun observeFavourites() {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            favouriteRepository.getFavouriteIds(currentUserId).collectLatest { ids ->
                _favouriteIds.value = ids
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        var cars = allListedCars

        // Apply filter mode
        when (filterMode) {
            FilterMode.MY_CARS -> cars = cars.filter { it.ownerId == currentUserId }
            FilterMode.FAVOURITES -> {
                val favIds = _favouriteIds.value ?: emptySet()
                cars = cars.filter { it.id in favIds }
            }
            FilterMode.ALL -> {}
        }

        // Apply search
        if (currentSearchQuery.isNotBlank()) {
            val query = currentSearchQuery.lowercase()
            cars = cars.filter {
                it.title.lowercase().contains(query) || it.model.lowercase().contains(query)
            }
        }

        // Apply sort
        currentSortMode?.let { cars = CarRepository.sortCars(cars, it) }
        _displayedCars.value = cars
    }

    fun searchCars(query: String) { currentSearchQuery = query; applyFilters() }
    fun sortCars(mode: CarRepository.SortMode) { currentSortMode = mode; applyFilters() }
    fun setFilterMode(mode: FilterMode) { filterMode = mode; applyFilters() }

    fun toggleFavourite(carId: String) {
        if (currentUserId.isEmpty()) return
        val currentFavs = _favouriteIds.value ?: emptySet()
        val isFav = carId in currentFavs
        viewModelScope.launch {
            favouriteRepository.toggleFavourite(currentUserId, carId, isFav)
        }
    }
}
