package com.example.assignment3_cos30017.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTest {

    // --- generateId ---

    @Test
    fun generateId_orders_alphabetically_first_uid_smaller() {
        val id = Conversation.generateId("alice", "bob")
        assertEquals("alice_bob", id)
    }

    @Test
    fun generateId_orders_alphabetically_first_uid_larger() {
        val id = Conversation.generateId("bob", "alice")
        assertEquals("alice_bob", id)
    }

    @Test
    fun generateId_same_result_regardless_of_order() {
        val id1 = Conversation.generateId("user1", "user2")
        val id2 = Conversation.generateId("user2", "user1")
        assertEquals(id1, id2)
    }

    @Test
    fun generateId_with_identical_uids() {
        val id = Conversation.generateId("same", "same")
        assertEquals("same_same", id)
    }

    // --- getUnreadCount ---

    @Test
    fun getUnreadCount_returns_1_when_message_newer_than_read_time() {
        val conv = Conversation(
            lastMessageTime = 2000,
            lastReadTime = mapOf("user1" to 1000)
        )
        assertEquals(1, conv.getUnreadCount("user1"))
    }

    @Test
    fun getUnreadCount_returns_0_when_read_time_equals_message_time() {
        val conv = Conversation(
            lastMessageTime = 1000,
            lastReadTime = mapOf("user1" to 1000)
        )
        assertEquals(0, conv.getUnreadCount("user1"))
    }

    @Test
    fun getUnreadCount_returns_0_when_read_time_after_message_time() {
        val conv = Conversation(
            lastMessageTime = 1000,
            lastReadTime = mapOf("user1" to 2000)
        )
        assertEquals(0, conv.getUnreadCount("user1"))
    }

    @Test
    fun getUnreadCount_returns_1_when_user_has_no_read_time() {
        val conv = Conversation(
            lastMessageTime = 1000,
            lastReadTime = emptyMap()
        )
        assertEquals(1, conv.getUnreadCount("user1"))
    }

    @Test
    fun getUnreadCount_returns_0_when_no_messages() {
        val conv = Conversation(
            lastMessageTime = 0,
            lastReadTime = emptyMap()
        )
        assertEquals(0, conv.getUnreadCount("user1"))
    }

    @Test
    fun getUnreadCount_independent_per_user() {
        val conv = Conversation(
            lastMessageTime = 2000,
            lastReadTime = mapOf("user1" to 2000, "user2" to 500)
        )
        assertEquals(0, conv.getUnreadCount("user1"))
        assertEquals(1, conv.getUnreadCount("user2"))
    }

    // --- defaults ---

    @Test
    fun default_conversation_has_empty_participants() {
        val conv = Conversation()
        assertEquals(emptyList<String>(), conv.participants)
        assertEquals(emptyMap<String, String>(), conv.participantNames)
    }

    @Test
    fun collection_name_is_conversations() {
        assertEquals("conversations", Conversation.COLLECTION)
    }
}
