package com.example.assignment3_cos30017.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionTest {

    @Test
    fun default_transaction_has_zero_amount() {
        val txn = Transaction()
        assertEquals(0, txn.amount)
        assertEquals(0, txn.balanceAfter)
    }

    @Test
    fun default_transaction_has_null_referenceId() {
        val txn = Transaction()
        assertNull(txn.referenceId)
    }

    @Test
    fun type_constants_are_correct() {
        assertEquals("TOP_UP", Transaction.TYPE_TOP_UP)
        assertEquals("RENTAL_PAYMENT", Transaction.TYPE_RENTAL_PAYMENT)
        assertEquals("RENTAL_REFUND", Transaction.TYPE_RENTAL_REFUND)
        assertEquals("RENTAL_INCOME", Transaction.TYPE_RENTAL_INCOME)
        assertEquals("BID_HOLD", Transaction.TYPE_BID_HOLD)
        assertEquals("BID_REFUND", Transaction.TYPE_BID_REFUND)
    }

    @Test
    fun collection_name_is_transactions() {
        assertEquals("transactions", Transaction.COLLECTION)
    }

    @Test
    fun top_up_transaction_has_positive_amount() {
        val txn = Transaction(
            userId = "u1", type = Transaction.TYPE_TOP_UP,
            amount = 500, balanceAfter = 1000
        )
        assert(txn.amount > 0)
        assertEquals(1000, txn.balanceAfter)
    }

    @Test
    fun payment_transaction_has_negative_amount() {
        val txn = Transaction(
            userId = "u1", type = Transaction.TYPE_RENTAL_PAYMENT,
            amount = -200, balanceAfter = 300
        )
        assert(txn.amount < 0)
    }
}
