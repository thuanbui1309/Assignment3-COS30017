package com.example.assignment3_cos30017.data.network

import com.example.assignment3_cos30017.BuildConfig
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.random.Random

/**
 * SePay configuration for VietQR payment integration.
 * Set real credentials in local.properties (not committed to VCS):
 *   sepay.api.token=your_real_token
 *   sepay.bank.id=MB
 *   sepay.account.number=123456789
 *   sepay.account.name=YOUR NAME
 */
object SePayConfig {

    val API_TOKEN: String = BuildConfig.SEPAY_API_TOKEN
    val BANK_ID: String = BuildConfig.SEPAY_BANK_ID
    val ACCOUNT_NUMBER: String = BuildConfig.SEPAY_ACCOUNT_NUMBER
    val ACCOUNT_NAME: String = BuildConfig.SEPAY_ACCOUNT_NAME
    const val TEMPLATE = "compact2"

    const val SEPAY_BASE_URL = "https://my.sepay.vn/userapi/"

    // Credit to VND conversion (1 Credit = 1 VND)
    const val CREDIT_TO_VND = 1

    private val ALPHANUMERIC = ('A'..'Z') + ('0'..'9')

    /**
     * Generate a short, alphanumeric-only reference code.
     * Format: "SEVQR TP" + 6 random chars (e.g. "SEVQR TP4F7K2X").
     *
     * - "SEVQR" prefix helps SePay identify the transaction faster.
     * - "TP" stands for Top-Up so we can distinguish from other payment types.
     * - Only uppercase letters + digits — no underscores or special chars that
     *   banking apps might strip or mangle.
     * - Short enough (14 chars) to survive memo truncation on all major VN banks.
     */
    fun generateReference(): String {
        val code = (1..6).map { ALPHANUMERIC.random(Random) }.joinToString("")
        return "SEVQR TP$code"
    }

    /**
     * Opens a web pay page. Some VietQR deep links require an `app` parameter
     * (bank app identifier) which we don't have reliably, so web is the safest fallback.
     */
    fun generatePayUrl(creditAmount: Int, reference: String): String {
        val vndAmount = creditAmount * CREDIT_TO_VND
        val memo = URLEncoder.encode(reference, StandardCharsets.UTF_8.toString())
        val accountName = URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8.toString())
        return "https://dl.vietqr.io/pay" +
                "?bank=$BANK_ID" +
                "&account=$ACCOUNT_NUMBER" +
                "&amount=$vndAmount" +
                "&memo=$memo" +
                "&accountName=$accountName"
    }

    fun generateQrUrl(creditAmount: Int, reference: String): String {
        val vndAmount = creditAmount * CREDIT_TO_VND
        // Use SePay QR generator so SePay can detect transactions reliably by description (payment code).
        // Docs: https://developer.sepay.vn/en/sepay-webhooks/tao-qr-va-form-thanh-toan
        val des = URLEncoder.encode(reference, StandardCharsets.UTF_8.toString())
        val bank = URLEncoder.encode(BANK_ID, StandardCharsets.UTF_8.toString())
        return "https://qr.sepay.vn/img" +
                "?acc=$ACCOUNT_NUMBER" +
                "&bank=$bank" +
                "&amount=$vndAmount" +
                "&des=$des" +
                "&template=$TEMPLATE"
    }
}
