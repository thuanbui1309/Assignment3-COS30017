package com.example.assignment3_cos30017.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BookingTest {

    @Test
    fun default_booking_has_active_status() {
        val booking = Booking()
        assertEquals(Booking.STATUS_ACTIVE, booking.status)
    }

    @Test
    fun default_booking_has_zero_cost() {
        val booking = Booking()
        assertEquals(0, booking.rentalDays)
        assertEquals(0, booking.dailyCost)
        assertEquals(0, booking.totalCost)
    }

    @Test
    fun status_constants_are_correct() {
        assertEquals("ACTIVE", Booking.STATUS_ACTIVE)
        assertEquals("COMPLETED", Booking.STATUS_COMPLETED)
        assertEquals("CANCELLED", Booking.STATUS_CANCELLED)
    }

    @Test
    fun collection_name_is_bookings() {
        assertEquals("bookings", Booking.COLLECTION)
    }

    @Test
    fun copy_preserves_rental_info() {
        val booking = Booking(
            carId = "c1", renterId = "r1", ownerId = "o1",
            rentalDays = 5, dailyCost = 100, totalCost = 500
        )
        val cancelled = booking.copy(status = Booking.STATUS_CANCELLED)
        assertEquals(Booking.STATUS_CANCELLED, cancelled.status)
        assertEquals(500, cancelled.totalCost)
        assertEquals("c1", cancelled.carId)
    }
}
