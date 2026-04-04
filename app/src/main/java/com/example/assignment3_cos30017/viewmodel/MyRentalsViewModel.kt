package com.example.assignment3_cos30017.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.model.Bid
import com.example.assignment3_cos30017.data.model.Booking
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.repository.BidRepository
import com.example.assignment3_cos30017.data.repository.BookingRepository
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyRentalsViewModel : ViewModel() {

    private val bidRepository = BidRepository()
    private val bookingRepository = BookingRepository()
    private val carRepository = CarRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _displayedCars = MutableLiveData<List<Car>>()
    val displayedCars: LiveData<List<Car>> = _displayedCars

    private val _carContextMap = MutableLiveData<Map<String, RentalItemContext>>()
    val carContextMap: LiveData<Map<String, RentalItemContext>> = _carContextMap

    private var allBiddingCars: List<Car> = emptyList()
    private var allRentingCars: List<Car> = emptyList()
    private var contextMap: Map<String, RentalItemContext> = emptyMap()

    var currentFilter: RentalFilterMode = RentalFilterMode.ALL
        private set
    private var searchQuery = ""
    private var sortMode: CarRepository.SortMode = CarRepository.SortMode.NEWEST

    enum class RentalFilterMode { ALL, BIDDING, RENTING }

    data class RentalItemContext(
        val type: ItemType,
        val bidId: String? = null,
        val bookingId: String? = null
    )

    enum class ItemType { BID, BOOKING }

    private val carCache = mutableMapOf<String, Car>()

    init {
        if (currentUserId.isNotEmpty()) observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            val bidsFlow = bidRepository.getBidsByUser(currentUserId)
            val bookingsFlow = bookingRepository.getBookingsByRenter(currentUserId)

            combine(bidsFlow, bookingsFlow) { bids, bookings ->
                bids to bookings
            }.collectLatest { (bids, bookings) ->
                processData(bids, bookings)
            }
        }
    }

    private suspend fun processData(allBids: List<Bid>, allBookings: List<Booking>) {
        val pendingBids = allBids.filter { it.status == Bid.STATUS_PENDING }
        val activeBookings = allBookings.filter { it.status == Booking.STATUS_ACTIVE }

        // Collect all unique car IDs
        val bidCarIds = pendingBids.map { it.carId }.toSet()
        val bookingCarIds = activeBookings.map { it.carId }.toSet()
        val allCarIds = bidCarIds + bookingCarIds

        // Fetch missing cars
        val missing = allCarIds - carCache.keys
        for (carId in missing) {
            try {
                carRepository.getCarById(carId).first()?.let { car ->
                    carCache[car.id] = car
                }
            } catch (_: Exception) { }
        }

        // Build context map (booking takes precedence over bid for same car)
        val newContextMap = mutableMapOf<String, RentalItemContext>()
        for (bid in pendingBids) {
            newContextMap[bid.carId] = RentalItemContext(ItemType.BID, bidId = bid.id)
        }
        for (booking in activeBookings) {
            newContextMap[booking.carId] = RentalItemContext(ItemType.BOOKING, bookingId = booking.id)
        }
        contextMap = newContextMap
        _carContextMap.postValue(newContextMap)

        // Build car lists
        allBiddingCars = pendingBids.mapNotNull { carCache[it.carId] }.distinctBy { it.id }
        allRentingCars = activeBookings.mapNotNull { carCache[it.carId] }.distinctBy { it.id }

        applyFilter()
    }

    fun filterByStatus(mode: RentalFilterMode) {
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
        var cars = when (currentFilter) {
            RentalFilterMode.ALL -> (allBiddingCars + allRentingCars).distinctBy { it.id }
            RentalFilterMode.BIDDING -> allBiddingCars
            RentalFilterMode.RENTING -> allRentingCars
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
