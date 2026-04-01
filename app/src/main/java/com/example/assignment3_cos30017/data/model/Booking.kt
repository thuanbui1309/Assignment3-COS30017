package com.example.assignment3_cos30017.data.model

data class Booking(
    val id: String = "",
    val carId: String = "",
    val carTitle: String = "",
    val renterId: String = "",
    val ownerId: String = "",
    val renterName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val rentalDays: Int = 0,
    val dailyCost: Int = 0,
    val totalCost: Int = 0,
    val status: String = STATUS_ACTIVE,
    val bookingDate: Long = System.currentTimeMillis()
) {
    companion object {
        const val COLLECTION = "bookings"
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}
