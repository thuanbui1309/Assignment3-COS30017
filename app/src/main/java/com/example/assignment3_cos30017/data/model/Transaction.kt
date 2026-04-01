package com.example.assignment3_cos30017.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Transaction(
    val id: String = "",
    val userId: String = "",
    val type: String = "",
    val amount: Int = 0,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val referenceId: String? = null,
    val balanceAfter: Int = 0
) : Parcelable {
    companion object {
        const val COLLECTION = "transactions"
        const val TYPE_TOP_UP = "TOP_UP"
        const val TYPE_RENTAL_PAYMENT = "RENTAL_PAYMENT"
        const val TYPE_RENTAL_REFUND = "RENTAL_REFUND"
        const val TYPE_RENTAL_INCOME = "RENTAL_INCOME"
        const val TYPE_BID_HOLD = "BID_HOLD"
        const val TYPE_BID_REFUND = "BID_REFUND"
    }
}
