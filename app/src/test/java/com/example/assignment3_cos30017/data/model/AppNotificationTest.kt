package com.example.assignment3_cos30017.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AppNotificationTest {

    @Test
    fun default_notification_is_unread() {
        val notif = AppNotification()
        assertFalse(notif.read)
    }

    @Test
    fun default_notification_has_null_optional_fields() {
        val notif = AppNotification()
        assertNull(notif.carId)
        assertNull(notif.carTitle)
        assertNull(notif.actorUserId)
        assertNull(notif.actorName)
        assertNull(notif.bidId)
        assertNull(notif.amount)
        assertNull(notif.rentalDays)
    }

    @Test
    fun type_constants_are_correct() {
        assertEquals("NEW_BID", AppNotification.TYPE_NEW_BID)
        assertEquals("BID_APPROVED_WINNER", AppNotification.TYPE_BID_APPROVED_WINNER)
        assertEquals("BID_APPROVED_LOSER", AppNotification.TYPE_BID_APPROVED_LOSER)
        assertEquals("BID_APPROVED_OWNER", AppNotification.TYPE_BID_APPROVED_OWNER)
        assertEquals("BID_REJECTED_BY_OWNER", AppNotification.TYPE_BID_REJECTED_BY_OWNER)
        assertEquals("BID_CANCELLED", AppNotification.TYPE_BID_CANCELLED)
        assertEquals("BIDS_CANCELLED_BY_OWNER", AppNotification.TYPE_BIDS_CANCELLED_BY_OWNER)
    }

    @Test
    fun collection_name_is_notifications() {
        assertEquals("notifications", AppNotification.COLLECTION)
    }

    @Test
    fun new_bid_notification_carries_bid_info() {
        val notif = AppNotification(
            type = AppNotification.TYPE_NEW_BID,
            toUserId = "owner1",
            carId = "car1", carTitle = "BMW M3",
            actorUserId = "bidder1", actorName = "John",
            bidId = "bid1", amount = 100, rentalDays = 3
        )
        assertEquals("owner1", notif.toUserId)
        assertEquals("car1", notif.carId)
        assertEquals(100, notif.amount)
        assertEquals(3, notif.rentalDays)
    }
}
