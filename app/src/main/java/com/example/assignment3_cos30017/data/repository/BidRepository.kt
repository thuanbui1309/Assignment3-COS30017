package com.example.assignment3_cos30017.data.repository

import com.example.assignment3_cos30017.data.model.Bid
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BidRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bidsRef = db.collection(Bid.COLLECTION)

    fun getBidsForCar(carId: String): Flow<List<Bid>> = callbackFlow {
        val listener = bidsRef.whereEqualTo("carId", carId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val bids = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Bid::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(bids)
            }
        awaitClose { listener.remove() }
    }

    fun getMyBidForCar(carId: String, userId: String): Flow<Bid?> = callbackFlow {
        val listener = bidsRef
            .whereEqualTo("carId", carId)
            .whereEqualTo("bidderId", userId)
            .whereEqualTo("status", Bid.STATUS_PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val bid = snapshot?.documents?.firstOrNull()?.let { doc ->
                    doc.toObject(Bid::class.java)?.copy(id = doc.id)
                }
                trySend(bid)
            }
        awaitClose { listener.remove() }
    }

    fun getMyBidForListing(carId: String, userId: String, listingId: String): Flow<Bid?> = callbackFlow {
        val listener = bidsRef
            .whereEqualTo("carId", carId)
            .whereEqualTo("bidderId", userId)
            .whereEqualTo("status", Bid.STATUS_PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val bid = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Bid::class.java)?.copy(id = doc.id)
                }?.firstOrNull { it.listingId == listingId }
                trySend(bid)
            }
        awaitClose { listener.remove() }
    }

    fun getBidsByUser(userId: String): Flow<List<Bid>> = callbackFlow {
        val listener = bidsRef.whereEqualTo("bidderId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val bids = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Bid::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(bids)
            }
        awaitClose { listener.remove() }
    }

    suspend fun placeBid(bid: Bid): String {
        val docRef = bidsRef.add(bid).await()
        return docRef.id
    }

    suspend fun approveBid(bidId: String) {
        bidsRef.document(bidId).update("status", Bid.STATUS_APPROVED).await()
    }

    suspend fun rejectBid(bidId: String) {
        bidsRef.document(bidId).update("status", Bid.STATUS_REJECTED).await()
    }

    suspend fun cancelBid(bidId: String) {
        bidsRef.document(bidId).update("status", Bid.STATUS_CANCELLED).await()
    }

    suspend fun updatePendingBid(
        bidId: String,
        dailyRate: Int,
        rentalDays: Int,
        totalAmount: Int,
        message: String
    ) {
        bidsRef.document(bidId).update(
            mapOf(
                "dailyRate" to dailyRate,
                "rentalDays" to rentalDays,
                "totalAmount" to totalAmount,
                "message" to message
            )
        ).await()
    }

    suspend fun getPendingBidsForCar(carId: String): List<Bid> {
        val snapshot = bidsRef
            .whereEqualTo("carId", carId)
            .whereEqualTo("status", Bid.STATUS_PENDING)
            .get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Bid::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun rejectAllPendingBids(carId: String, exceptBidId: String) {
        val pendingBids = getPendingBidsForCar(carId)
        pendingBids.filter { it.id != exceptBidId }.forEach { bid ->
            rejectBid(bid.id)
        }
    }
}
