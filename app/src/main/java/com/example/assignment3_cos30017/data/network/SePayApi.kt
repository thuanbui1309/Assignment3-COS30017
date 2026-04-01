package com.example.assignment3_cos30017.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface SePayApi {

    @GET("transactions/list")
    suspend fun getTransactions(
        @Query("limit") limit: Int = 10,
        @Query("transaction_date_min") transactionDateMin: String? = null,
        @Query("account_number") accountNumber: String? = null
    ): SePayTransactionResponse
}

data class SePayTransactionResponse(
    val status: Int,
    val messages: SePayMessages?,
    val transactions: List<SePayTransaction>
)

data class SePayMessages(
    val success: Boolean
)

data class SePayTransaction(
    val id: String,
    val transaction_content: String?,
    val amount_in: Long,
    val amount_out: Long,
    val transaction_date: String?
)
