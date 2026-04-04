package com.example.assignment3_cos30017.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.model.Booking
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.repository.BookingRepository
import com.example.assignment3_cos30017.data.repository.CarRepository
import com.example.assignment3_cos30017.data.repository.WalletRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RentViewModel(application: Application) : AndroidViewModel(application) {

    private val carRepository = CarRepository()
    private val bookingRepository = BookingRepository()
    private val walletRepository = WalletRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _selectedCar = MutableLiveData<Car>()
    val selectedCar: LiveData<Car> = _selectedCar

    private val _rentalDays = MutableLiveData(1)
    val rentalDays: LiveData<Int> = _rentalDays

    private val _totalCost = MutableLiveData(0)
    val totalCost: LiveData<Int> = _totalCost

    private val _creditBalance = MutableLiveData<Int>()
    val creditBalance: LiveData<Int> = _creditBalance

    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError

    private val _bookingSuccess = MutableLiveData<Boolean?>()
    val bookingSuccess: LiveData<Boolean?> = _bookingSuccess

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val appContext get() = getApplication<Application>()

    fun loadCar(carId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            carRepository.getCarById(carId).collect { car ->
                car?.let {
                    _selectedCar.postValue(it)
                    val balance = walletRepository.getBalanceOnce(userId)
                    _creditBalance.postValue(balance)
                    updateTotalCost()
                }
            }
        }
    }

    fun setRentalDays(days: Int) {
        _rentalDays.value = days
        updateTotalCost()
    }

    private fun updateTotalCost() {
        val car = _selectedCar.value ?: return
        val days = _rentalDays.value ?: 1
        val cost = car.dailyCost * days
        _totalCost.value = cost
        validate(cost)
    }

    private fun validate(cost: Int) {
        val balance = _creditBalance.value ?: 0
        _validationError.value = when {
            cost > WalletRepository.MAX_RENTAL_COST ->
                appContext.getString(R.string.error_max_limit_exceeded, cost, WalletRepository.MAX_RENTAL_COST)
            cost > balance ->
                appContext.getString(R.string.error_insufficient_credit, balance)
            else -> null
        }
    }

    fun confirmBooking(customerName: String, customerPhone: String, customerEmail: String) {
        val car = _selectedCar.value ?: return
        val days = _rentalDays.value ?: return
        val cost = _totalCost.value ?: return
        val user = auth.currentUser ?: return

        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                walletRepository.transferCredits(
                    fromUserId = user.uid,
                    toUserId = car.ownerId,
                    amount = cost,
                    fromDescription = "Rented ${car.title} for $days days",
                    toDescription = "Rental income: ${car.title} ($days days)"
                )

                val booking = Booking(
                    carId = car.id, carTitle = car.title, renterId = user.uid, ownerId = car.ownerId,
                    renterName = user.displayName ?: customerName,
                    customerPhone = customerPhone, customerEmail = customerEmail,
                    rentalDays = days, dailyCost = car.dailyCost, totalCost = cost
                )
                bookingRepository.createBooking(booking)
                carRepository.setStatus(car.id, com.example.assignment3_cos30017.data.model.Car.STATUS_RENTED)

                _isLoading.postValue(false)
                _bookingSuccess.postValue(true)
            } catch (e: Exception) {
                _isLoading.postValue(false)
                _bookingSuccess.postValue(false)
            }
        }
    }
}
