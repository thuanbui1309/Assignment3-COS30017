package com.example.assignment3_cos30017.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CarTest {

    // --- effectiveStatus ---

    @Test
    fun effectiveStatus_returns_status_when_not_blank() {
        val car = Car(status = Car.STATUS_RENTED, isAvailable = true)
        assertEquals(Car.STATUS_RENTED, car.effectiveStatus)
    }

    @Test
    fun effectiveStatus_returns_listed_when_status_blank_and_available() {
        val car = Car(status = "", isAvailable = true)
        assertEquals(Car.STATUS_LISTED, car.effectiveStatus)
    }

    @Test
    fun effectiveStatus_returns_rented_when_status_blank_and_not_available() {
        val car = Car(status = "", isAvailable = false)
        assertEquals(Car.STATUS_RENTED, car.effectiveStatus)
    }

    @Test
    fun effectiveStatus_prefers_status_over_isAvailable_flag() {
        val car = Car(status = Car.STATUS_UNLISTED, isAvailable = true)
        assertEquals(Car.STATUS_UNLISTED, car.effectiveStatus)
    }

    // --- default values ---

    @Test
    fun default_car_has_unlisted_status() {
        val car = Car()
        assertEquals(Car.STATUS_UNLISTED, car.status)
        assertEquals(Car.STATUS_UNLISTED, car.effectiveStatus)
    }

    @Test
    fun default_car_has_empty_image_urls() {
        val car = Car()
        assertEquals(emptyList<String>(), car.imageUrls)
    }

    @Test
    fun default_car_has_zero_cost_and_days() {
        val car = Car()
        assertEquals(0, car.dailyCost)
        assertEquals(0, car.maxRentalDays)
    }

    // --- data class copy ---

    @Test
    fun copy_preserves_all_fields() {
        val original = Car(
            id = "car1", ownerId = "owner1", title = "BMW M3",
            model = "Sedan", year = 2023, dailyCost = 100,
            status = Car.STATUS_LISTED
        )
        val copied = original.copy(dailyCost = 200)
        assertEquals("car1", copied.id)
        assertEquals("BMW M3", copied.title)
        assertEquals(200, copied.dailyCost)
        assertEquals(Car.STATUS_LISTED, copied.status)
    }

    // --- constants ---

    @Test
    fun status_constants_are_correct() {
        assertEquals("UNLISTED", Car.STATUS_UNLISTED)
        assertEquals("LISTED", Car.STATUS_LISTED)
        assertEquals("RENTED", Car.STATUS_RENTED)
    }

    @Test
    fun collection_name_is_cars() {
        assertEquals("cars", Car.COLLECTION)
    }
}
