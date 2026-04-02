package com.example.assignment3_cos30017.data.repository

import com.example.assignment3_cos30017.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    suspend fun getUserById(userId: String): User? {
        if (userId.isBlank()) return null
        val doc = db.collection("users").document(userId).get().await()
        return doc.toObject(User::class.java)?.copy(uid = doc.id)
    }

    suspend fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()
        val capitalized = query.replaceFirstChar { it.uppercase() }
        val results = db.collection("users")
            .whereGreaterThanOrEqualTo("displayName", capitalized)
            .whereLessThanOrEqualTo("displayName", capitalized + "\uf8ff")
            .limit(20)
            .get()
            .await()
        return results.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.copy(uid = doc.id)
        }.filter { it.uid != currentUid }
    }

    suspend fun updateFcmToken(userId: String, token: String) {
        if (userId.isBlank() || token.isBlank()) return
        db.collection("users").document(userId)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .await()
    }
}
