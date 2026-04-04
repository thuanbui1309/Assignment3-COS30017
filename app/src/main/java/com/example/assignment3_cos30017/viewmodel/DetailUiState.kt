package com.example.assignment3_cos30017.viewmodel

import com.example.assignment3_cos30017.data.model.Bid
import com.example.assignment3_cos30017.data.model.Booking
import com.example.assignment3_cos30017.data.model.Car

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class RenterBrowsing(val car: Car, val balance: Int, val bids: List<Bid>) : DetailUiState()
    data class RenterAlreadyBid(val car: Car, val balance: Int, val myBid: Bid, val bids: List<Bid>) : DetailUiState()
    data class MarketplaceRented(val car: Car) : DetailUiState()
    data class OwnerMarketplace(val car: Car, val bids: List<Bid>) : DetailUiState()
    data class OwnerGarage(val car: Car) : DetailUiState()
    data class OwnerListed(val car: Car, val bids: List<Bid>) : DetailUiState()
    data class OwnerRented(val car: Car, val booking: Booking) : DetailUiState()
    data class RenterActiveBooking(val car: Car, val booking: Booking) : DetailUiState()
    data class RenterBookingHistory(val car: Car, val booking: Booking) : DetailUiState()
}
