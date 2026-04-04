package com.example.assignment3_cos30017.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.model.Booking
import com.example.assignment3_cos30017.data.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyRentalsActivityViewModel : ViewModel() {

    private val bookingRepository = BookingRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _displayedBookings = MutableLiveData<List<Booking>>()
    val displayedBookings: LiveData<List<Booking>> = _displayedBookings

    private var allBookings: List<Booking> = emptyList()

    var currentFilter: FilterMode = FilterMode.ALL
        private set
    private var searchQuery = ""
    private var sortAscending = false

    enum class FilterMode { ALL, ACTIVE, COMPLETED, CANCELLED }

    init {
        if (currentUserId.isNotEmpty()) {
            viewModelScope.launch {
                bookingRepository.getBookingsByRenter(currentUserId).collectLatest { bookings ->
                    allBookings = bookings
                    applyFilter()
                }
            }
        }
    }

    fun filterByStatus(mode: FilterMode) {
        currentFilter = mode
        applyFilter()
    }

    fun searchBookings(query: String) {
        searchQuery = query
        applyFilter()
    }

    fun toggleSort() {
        sortAscending = !sortAscending
        applyFilter()
    }

    private fun applyFilter() {
        var bookings = when (currentFilter) {
            FilterMode.ALL -> allBookings
            FilterMode.ACTIVE -> allBookings.filter { it.status == Booking.STATUS_ACTIVE }
            FilterMode.COMPLETED -> allBookings.filter { it.status == Booking.STATUS_COMPLETED }
            FilterMode.CANCELLED -> allBookings.filter { it.status == Booking.STATUS_CANCELLED }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            bookings = bookings.filter { it.carTitle.lowercase().contains(q) }
        }
        bookings = if (sortAscending) {
            bookings.sortedBy { it.bookingDate }
        } else {
            bookings.sortedByDescending { it.bookingDate }
        }
        _displayedBookings.value = bookings
    }
}
