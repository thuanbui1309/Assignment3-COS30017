package com.example.assignment3_cos30017.data.repository

import android.util.Log
import com.example.assignment3_cos30017.data.model.AppNotification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val notifRef = db.collection(AppNotification.COLLECTION)

    fun observeNotificationsForUser(userId: String): Flow<List<AppNotification>> = callbackFlow {
        val listener = notifRef
            .whereEqualTo("toUserId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeNotificationsForUser", error)
                    return@addSnapshotListener
                }
                val notifs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(notifs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun create(notification: AppNotification) {
        notifRef.add(notification).await()
    }

    suspend fun markRead(notificationId: String, read: Boolean) {
        notifRef.document(notificationId).update("read", read).await()
    }

    companion object {
        private const val TAG = "NotificationRepository"
    }
}

