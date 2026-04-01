package com.example.assignment3_cos30017.data.model

data class Car(
    val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val model: String = "",
    val year: Int = 2024,
    val kilometres: Int = 0,
    val dailyCost: Int = 0,
    val maxRentalDays: Int = 0,
    val description: String = "",
    val notes: String = "",
    val imageUrls: List<String> = emptyList(),
    val status: String = STATUS_UNLISTED,
    @Deprecated("Use status instead. Kept for Firestore backward compat.")
    val isAvailable: Boolean = true,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val listedAt: Long = 0L,
    val listingId: String = ""
) {
    val effectiveStatus: String
        get() = if (status.isNotBlank()) status
                else if (isAvailable) STATUS_LISTED else STATUS_RENTED

    companion object {
        const val COLLECTION = "cars"
        const val STATUS_UNLISTED = "UNLISTED"
        const val STATUS_LISTED = "LISTED"
        const val STATUS_RENTED = "RENTED"
    }
}
