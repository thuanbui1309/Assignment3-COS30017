package com.example.assignment3_cos30017.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FavouriteRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getFavouriteIds(userId: String): Flow<Set<String>> = callbackFlow {
        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                @Suppress("UNCHECKED_CAST")
                val ids = (snapshot?.get("favouriteCarIds") as? List<String>)?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    suspend fun toggleFavourite(userId: String, carId: String, currentlyFavourited: Boolean) {
        val ref = db.collection("users").document(userId)
        if (currentlyFavourited) {
            ref.update("favouriteCarIds", FieldValue.arrayRemove(carId)).await()
        } else {
            ref.update("favouriteCarIds", FieldValue.arrayUnion(carId)).await()
        }
    }
}
