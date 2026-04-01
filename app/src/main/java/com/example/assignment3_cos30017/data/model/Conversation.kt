package com.example.assignment3_cos30017.data.model

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastReadTime: Map<String, Long> = emptyMap(),
    val typing: Map<String, Boolean> = emptyMap(),
    val unreadCount: Int = 0
) {
    fun getUnreadCount(userId: String): Int {
        val readTime = lastReadTime[userId] ?: 0L
        return if (lastMessageTime > readTime) 1 else 0
    }

    companion object {
        const val COLLECTION = "conversations"

        fun generateId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }
    }
}
