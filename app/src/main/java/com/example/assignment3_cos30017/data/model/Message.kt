package com.example.assignment3_cos30017.data.model

import com.google.firebase.firestore.Exclude

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = TYPE_TEXT,
    val imageUrl: String? = null,
    val read: Boolean = false,
    /**
     * Local-only UI state (not stored in Firestore).
     */
    @get:Exclude @set:Exclude
    var localStatus: LocalStatus = LocalStatus.SENT
) {
    enum class LocalStatus { SENDING, SENT, FAILED }

    companion object {
        const val SUBCOLLECTION = "messages"
        const val TYPE_TEXT = "TEXT"
        const val TYPE_IMAGE = "IMAGE"

        /**
         * Stored as a token in Firestore so the UI can localize at runtime.
         */
        const val TEXT_TOKEN_PHOTO = "__PHOTO__"
    }
}
