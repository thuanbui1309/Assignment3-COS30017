package com.example.assignment3_cos30017.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletConstantsTest {

    @Test
    fun initial_credit_is_500() {
        assertEquals(500, WalletRepository.INITIAL_CREDIT)
    }

    @Test
    fun max_rental_cost_is_400() {
        assertEquals(400, WalletRepository.MAX_RENTAL_COST)
    }

    @Test
    fun max_rental_cost_is_less_than_initial_credit() {
        assertTrue(WalletRepository.MAX_RENTAL_COST < WalletRepository.INITIAL_CREDIT)
    }
}
