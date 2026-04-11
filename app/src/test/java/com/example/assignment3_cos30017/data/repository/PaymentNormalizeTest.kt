package com.example.assignment3_cos30017.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNormalizeTest {

    @Test
    fun normalize_lowercases_text() {
        assertEquals("hello", PaymentRepository.normalize("HELLO"))
    }

    @Test
    fun normalize_strips_spaces() {
        assertEquals("sevqrtpabc123", PaymentRepository.normalize("SEVQR TP ABC 123"))
    }

    @Test
    fun normalize_strips_dashes() {
        assertEquals("abc123def", PaymentRepository.normalize("ABC-123-DEF"))
    }

    @Test
    fun normalize_strips_special_characters() {
        assertEquals("ref123", PaymentRepository.normalize("ref@#$%123!"))
    }

    @Test
    fun normalize_preserves_digits() {
        assertEquals("1234567890", PaymentRepository.normalize("1234567890"))
    }

    @Test
    fun normalize_empty_string_returns_empty() {
        assertEquals("", PaymentRepository.normalize(""))
    }

    @Test
    fun normalize_only_special_chars_returns_empty() {
        assertEquals("", PaymentRepository.normalize("@#$%^&*"))
    }

    @Test
    fun normalize_makes_matching_resilient() {
        val ref = PaymentRepository.normalize("SEVQR TPABC123")
        val bankContent = PaymentRepository.normalize("sevqr-tp-abc-123 payment received")
        assertTrue(bankContent.contains(ref))
    }

    @Test
    fun normalize_handles_unicode_letters() {
        val result = PaymentRepository.normalize("café123")
        assertEquals("café123", result)
    }

    @Test
    fun normalize_strips_dots_and_commas() {
        assertEquals("1000000", PaymentRepository.normalize("1,000,000"))
        assertEquals("123456", PaymentRepository.normalize("123.456"))
    }
}
