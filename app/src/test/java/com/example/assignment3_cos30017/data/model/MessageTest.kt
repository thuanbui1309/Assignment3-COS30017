package com.example.assignment3_cos30017.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTest {

    @Test
    fun default_message_is_text_type() {
        val msg = Message()
        assertEquals(Message.TYPE_TEXT, msg.type)
    }

    @Test
    fun default_local_status_is_sent() {
        val msg = Message()
        assertEquals(Message.LocalStatus.SENT, msg.localStatus)
    }

    @Test
    fun type_constants_are_correct() {
        assertEquals("TEXT", Message.TYPE_TEXT)
        assertEquals("IMAGE", Message.TYPE_IMAGE)
    }

    @Test
    fun photo_token_is_correct() {
        assertEquals("__PHOTO__", Message.TEXT_TOKEN_PHOTO)
    }

    @Test
    fun subcollection_name_is_messages() {
        assertEquals("messages", Message.SUBCOLLECTION)
    }

    @Test
    fun local_status_enum_has_three_values() {
        val values = Message.LocalStatus.entries
        assertEquals(3, values.size)
        assert(Message.LocalStatus.SENDING in values)
        assert(Message.LocalStatus.SENT in values)
        assert(Message.LocalStatus.FAILED in values)
    }

    @Test
    fun local_status_can_be_changed() {
        val msg = Message(text = "hello")
        msg.localStatus = Message.LocalStatus.SENDING
        assertEquals(Message.LocalStatus.SENDING, msg.localStatus)
        msg.localStatus = Message.LocalStatus.FAILED
        assertEquals(Message.LocalStatus.FAILED, msg.localStatus)
    }
}
