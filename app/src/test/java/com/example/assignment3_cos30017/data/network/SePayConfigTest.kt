package com.example.assignment3_cos30017.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SePayConfigTest {

    // --- generateReference ---

    @Test
    fun generateReference_starts_with_prefix() {
        val ref = SePayConfig.generateReference()
        assertTrue("Expected prefix 'SEVQR TP', got: $ref", ref.startsWith("SEVQR TP"))
    }

    @Test
    fun generateReference_has_correct_length() {
        val ref = SePayConfig.generateReference()
        // "SEVQR TP" (8 chars) + 6 random chars = 14 total
        assertEquals(14, ref.length)
    }

    @Test
    fun generateReference_random_part_is_alphanumeric() {
        val ref = SePayConfig.generateReference()
        val randomPart = ref.removePrefix("SEVQR TP")
        assertEquals(6, randomPart.length)
        assertTrue(
            "Random part should be uppercase alphanumeric: $randomPart",
            randomPart.all { it in 'A'..'Z' || it in '0'..'9' }
        )
    }

    @Test
    fun generateReference_produces_different_values() {
        val refs = (1..10).map { SePayConfig.generateReference() }.toSet()
        // With 36^6 possible values, 10 calls should produce at least 2 unique references
        assertTrue("Expected multiple unique references, got ${refs.size}", refs.size > 1)
    }

    // --- generatePayUrl ---

    @Test
    fun generatePayUrl_contains_amount_in_vnd() {
        val url = SePayConfig.generatePayUrl(500, "SEVQR TPABC123")
        assertTrue("URL should contain amount=500, got: $url", url.contains("amount=500"))
    }

    @Test
    fun generatePayUrl_contains_encoded_reference() {
        val url = SePayConfig.generatePayUrl(100, "SEVQR TPABC123")
        // Space in reference should be encoded
        assertTrue("URL should contain encoded reference", url.contains("memo=SEVQR"))
    }

    @Test
    fun generatePayUrl_starts_with_vietqr_base() {
        val url = SePayConfig.generatePayUrl(100, "REF")
        assertTrue(url.startsWith("https://dl.vietqr.io/pay"))
    }

    // --- generateQrUrl ---

    @Test
    fun generateQrUrl_contains_amount_in_vnd() {
        val url = SePayConfig.generateQrUrl(200, "SEVQR TPXYZ789")
        assertTrue("URL should contain amount=200", url.contains("amount=200"))
    }

    @Test
    fun generateQrUrl_starts_with_sepay_base() {
        val url = SePayConfig.generateQrUrl(100, "REF")
        assertTrue(url.startsWith("https://qr.sepay.vn/img"))
    }

    @Test
    fun generateQrUrl_contains_template() {
        val url = SePayConfig.generateQrUrl(100, "REF")
        assertTrue(url.contains("template=${SePayConfig.TEMPLATE}"))
    }

    // --- constants ---

    @Test
    fun credit_to_vnd_conversion_is_one() {
        assertEquals(1, SePayConfig.CREDIT_TO_VND)
    }

    @Test
    fun sepay_base_url_is_correct() {
        assertEquals("https://my.sepay.vn/userapi/", SePayConfig.SEPAY_BASE_URL)
    }
}
