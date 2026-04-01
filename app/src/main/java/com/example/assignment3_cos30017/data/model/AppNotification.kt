package com.example.assignment3_cos30017.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Stored in Firestore so the UI can render localized text at runtime.
 */
@Parcelize
data class AppNotification(
    val id: String = "",
    val toUserId: String = "",
    val type: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val carId: String? = null,
    val carTitle: String? = null,
    val actorUserId: String? = null,
    val actorName: String? = null,
    val bidId: String? = null,
    val amount: Int? = null,
    val rentalDays: Int? = null
) : Parcelable {
    companion object {
        const val COLLECTION = "notifications"

        const val TYPE_NEW_BID = "NEW_BID"
        const val TYPE_BID_APPROVED_WINNER = "BID_APPROVED_WINNER"
        const val TYPE_BID_APPROVED_LOSER = "BID_APPROVED_LOSER"
        const val TYPE_BID_APPROVED_OWNER = "BID_APPROVED_OWNER"
        const val TYPE_BID_REJECTED_BY_OWNER = "BID_REJECTED_BY_OWNER"
        const val TYPE_BID_CANCELLED = "BID_CANCELLED"
        const val TYPE_BIDS_CANCELLED_BY_OWNER = "BIDS_CANCELLED_BY_OWNER"
    }
}

