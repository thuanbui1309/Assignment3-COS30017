package com.example.assignment3_cos30017.data.repository

import android.util.Log
import com.example.assignment3_cos30017.BuildConfig
import com.example.assignment3_cos30017.data.network.SePayApi
import com.example.assignment3_cos30017.data.network.SePayConfig
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class PaymentRepository {

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${SePayConfig.API_TOKEN}")
                    .build()
            )
        }
        .build()

    private val sePayApi: SePayApi by lazy {
        Retrofit.Builder()
            .baseUrl(SePayConfig.SEPAY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SePayApi::class.java)
    }

    /**
     * Poll SePay API to check if a payment with matching reference has been received.
     * Returns the matched amount in VND, or null if not found within timeout.
     */
    suspend fun pollForPayment(
        reference: String,
        expectedAmountInVnd: Long? = null,
        maxAttempts: Int = 24,
        intervalMs: Long = 5000
    ): Long? {
        val normalizedRef = normalize(reference)
        // Only look at transactions from today onward to reduce noise and false positives.
        val dateMin = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        debugLog {
            "Start polling — reference=\"$reference\", normalized=\"$normalizedRef\", " +
                "expectedVnd=$expectedAmountInVnd, dateMin=$dateMin"
        }

        repeat(maxAttempts) { attempt ->
            try {
                val response = sePayApi.getTransactions(
                    limit = 100,
                    transactionDateMin = dateMin,
                    accountNumber = SePayConfig.ACCOUNT_NUMBER
                )

                debugLog {
                    "[Attempt ${attempt + 1}/$maxAttempts] " +
                        "Got ${response.transactions.size} transactions (status=${response.status})"
                }

                if (BuildConfig.DEBUG) {
                    response.transactions.take(3).forEachIndexed { i, txn ->
                        Log.d(
                            TAG,
                            "  txn[$i]: content=\"${txn.transaction_content}\", " +
                                "amount_in=${txn.amount_in}, date=${txn.transaction_date}"
                        )
                    }
                }

                // Primary match: reference code found in transaction content
                val match = response.transactions.firstOrNull { txn ->
                    val content = txn.transaction_content ?: return@firstOrNull false
                    val normalizedContent = normalize(content)
                    val found = normalizedContent.contains(normalizedRef)
                    if (found) debugLog { "  -> Reference MATCHED in: \"$content\"" }
                    found
                }

                if (match != null) {
                    debugLog { "Payment confirmed via reference match — amount=${match.amount_in}" }
                    return match.amount_in
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "[Attempt ${attempt + 1}/$maxAttempts] Network error: ${e.message}")
            }
            delay(intervalMs)
        }
        Log.w(TAG, "Polling timed out after $maxAttempts attempts for reference=\"$reference\"")
        return null
    }

    companion object {
        /**
         * Strip everything except letters and digits, then lowercase.
         * This makes matching resilient to spaces, dashes, or other separators
         * that banks may insert or remove.
         */
        internal fun normalize(input: String): String {
            return buildString(input.length) {
                input.lowercase().forEach { ch ->
                    if (ch.isLetterOrDigit()) append(ch)
                }
            }
        }
        private const val TAG = "PaymentRepository"

        private inline fun debugLog(message: () -> String) {
            if (BuildConfig.DEBUG) Log.d(TAG, message())
        }
    }
}
