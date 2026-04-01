package com.example.assignment3_cos30017.data.model

data class Bid(
    val id: String = "",
    val carId: String = "",
    val carTitle: String = "",
    val bidderId: String = "",
    val bidderName: String = "",
    val ownerId: String = "",
    val dailyRate: Int = 0,
    val rentalDays: Int = 0,
    val totalAmount: Int = 0,
    val listingId: String = "",
    val message: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val COLLECTION = "bids"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_APPROVED = "APPROVED"
        const val STATUS_REJECTED = "REJECTED"
        const val STATUS_CANCELLED = "CANCELLED"
    }
}
