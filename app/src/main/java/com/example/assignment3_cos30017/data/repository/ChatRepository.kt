package com.example.assignment3_cos30017.data.repository

import android.net.Uri
import android.util.Log
import com.example.assignment3_cos30017.data.model.Conversation
import com.example.assignment3_cos30017.data.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun getConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = db.collection(Conversation.COLLECTION)
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getConversations", error)
                    return@addSnapshotListener
                }
                val convos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()
                trySend(convos)
            }
        awaitClose { listener.remove() }
    }

    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection(Conversation.COLLECTION)
            .document(conversationId)
            .collection(Message.SUBCOLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getMessages", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    fun observeConversation(conversationId: String): Flow<Conversation> = callbackFlow {
        val listener = db.collection(Conversation.COLLECTION)
            .document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeConversation", error)
                    return@addSnapshotListener
                }
                val convo = snapshot?.toObject(Conversation::class.java) ?: return@addSnapshotListener
                trySend(convo.copy(id = snapshot.id))
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(conversationId: String, message: Message) {
        val convoRef = db.collection(Conversation.COLLECTION).document(conversationId)
        convoRef.collection(Message.SUBCOLLECTION).add(message).await()

        val lastMessage = if (message.type == Message.TYPE_IMAGE && message.text.isBlank()) {
            Message.TEXT_TOKEN_PHOTO
        } else {
            message.text
        }
        // Keep sender's read time in sync so global "unread" badges don't count your own messages.
        convoRef.update(
            mapOf(
                "lastMessage" to lastMessage,
                "lastMessageTime" to message.timestamp,
                "lastReadTime.${message.senderId}" to message.timestamp
            )
        ).await()
    }

    fun observeUnreadConversationCount(userId: String): Flow<Int> = callbackFlow {
        val listener = db.collection(Conversation.COLLECTION)
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeUnreadConversationCount", error)
                    return@addSnapshotListener
                }
                val convos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                val count = convos.sumOf { it.getUnreadCount(userId) }
                trySend(count)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendImageMessage(
        conversationId: String,
        imageUri: Uri,
        senderId: String,
        senderName: String,
        captionText: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        val ref = storage.reference.child("chat/${conversationId}/$timestamp")
        ref.putFile(imageUri).await()
        val url = ref.downloadUrl.await().toString()

        val message = Message(
            senderId = senderId, senderName = senderName,
            text = captionText, type = Message.TYPE_IMAGE, imageUrl = url,
            timestamp = timestamp
        )
        sendMessage(conversationId, message)
    }

    suspend fun getOrCreateConversation(
        currentUserId: String, currentUserName: String,
        otherUserId: String, otherUserName: String
    ): String {
        val convoId = Conversation.generateId(currentUserId, otherUserId)
        val doc = db.collection(Conversation.COLLECTION).document(convoId).get().await()

        if (!doc.exists()) {
            val conversation = Conversation(
                id = convoId,
                participants = listOf(currentUserId, otherUserId),
                participantNames = mapOf(currentUserId to currentUserName, otherUserId to otherUserName)
            )
            db.collection(Conversation.COLLECTION).document(convoId).set(conversation).await()
        }
        return convoId
    }

    suspend fun markAsRead(conversationId: String, userId: String) {
        db.collection(Conversation.COLLECTION).document(conversationId)
            .update("lastReadTime.$userId", System.currentTimeMillis())
            .await()
    }

    suspend fun setTyping(conversationId: String, userId: String, isTyping: Boolean) {
        db.collection(Conversation.COLLECTION).document(conversationId)
            .update("typing.$userId", isTyping)
            .await()
    }

    fun observeReadTime(conversationId: String, otherUserId: String): Flow<Long> = callbackFlow {
        val listener = db.collection(Conversation.COLLECTION).document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeReadTime", error)
                    return@addSnapshotListener
                }
                val convo = snapshot?.toObject(Conversation::class.java) ?: return@addSnapshotListener
                trySend(convo.lastReadTime[otherUserId] ?: 0L)
            }
        awaitClose { listener.remove() }
    }

    fun getUnreadCount(conversationId: String, userId: String): Flow<Int> = callbackFlow {
        val convoRef = db.collection(Conversation.COLLECTION).document(conversationId)
        val listener = convoRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "getUnreadCount", error)
                return@addSnapshotListener
            }
            val convo = snapshot?.toObject(Conversation::class.java) ?: return@addSnapshotListener
            val readTime = convo.lastReadTime[userId] ?: 0L
            val msgRef = convoRef.collection(Message.SUBCOLLECTION)
                .whereGreaterThan("timestamp", readTime)
                .whereNotEqualTo("senderId", userId)
            msgRef.get().addOnSuccessListener { trySend(it.size()) }
        }
        awaitClose { listener.remove() }
    }

    companion object {
        private const val TAG = "ChatRepository"
    }
}
