package com.example.assignment3_cos30017.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BidTest {

    @Test
    fun default_bid_has_pending_status() {
        val bid = Bid()
        assertEquals(Bid.STATUS_PENDING, bid.status)
    }

    @Test
    fun default_bid_has_zero_amounts() {
        val bid = Bid()
        assertEquals(0, bid.dailyRate)
        assertEquals(0, bid.rentalDays)
        assertEquals(0, bid.totalAmount)
    }

    @Test
    fun status_constants_are_correct() {
        assertEquals("PENDING", Bid.STATUS_PENDING)
        assertEquals("APPROVED", Bid.STATUS_APPROVED)
        assertEquals("REJECTED", Bid.STATUS_REJECTED)
        assertEquals("CANCELLED", Bid.STATUS_CANCELLED)
    }

    @Test
    fun collection_name_is_bids() {
        assertEquals("bids", Bid.COLLECTION)
    }

    @Test
    fun copy_changes_status_preserves_rest() {
        val bid = Bid(id = "b1", carId = "c1", dailyRate = 50, rentalDays = 3, totalAmount = 150)
        val approved = bid.copy(status = Bid.STATUS_APPROVED)
        assertEquals(Bid.STATUS_APPROVED, approved.status)
        assertEquals("b1", approved.id)
        assertEquals(150, approved.totalAmount)
    }
}
