package com.example.assignment3_cos30017.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.model.Car
import com.example.assignment3_cos30017.data.repository.CarRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val carRepository = CarRepository()

    private val _cars = MutableLiveData<List<Car>>()
    val cars: LiveData<List<Car>> = _cars

    init {
        viewModelScope.launch {
            carRepository.getAllCars().collectLatest { cars ->
                _cars.value = cars.filter { it.effectiveStatus == Car.STATUS_LISTED }
            }
        }
    }
}
