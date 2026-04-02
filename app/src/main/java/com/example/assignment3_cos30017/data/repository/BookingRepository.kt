package com.example.assignment3_cos30017.data.repository

import com.example.assignment3_cos30017.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookingsRef = db.collection(Booking.COLLECTION)

    fun getBookingsByRenter(renterId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingsRef.whereEqualTo("renterId", renterId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.bookingDate } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }

    fun getBookingsByOwner(ownerId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingsRef.whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.bookingDate } ?: emptyList()
                trySend(bookings)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getActiveBookingForCar(carId: String): Booking? {
        val snapshot = bookingsRef
            .whereEqualTo("carId", carId)
            .whereEqualTo("status", Booking.STATUS_ACTIVE)
            .limit(1)
            .get().await()
        return snapshot.documents.firstOrNull()?.let { doc ->
            doc.toObject(Booking::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun createBooking(booking: Booking): String {
        val docRef = bookingsRef.add(booking).await()
        return docRef.id
    }

    fun getBookingById(bookingId: String): Flow<Booking?> = callbackFlow {
        val listener = bookingsRef.document(bookingId).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val booking = snapshot?.toObject(Booking::class.java)?.copy(id = snapshot.id)
            trySend(booking)
        }
        awaitClose { listener.remove() }
    }

    suspend fun completeBooking(bookingId: String) {
        bookingsRef.document(bookingId).update("status", Booking.STATUS_COMPLETED).await()
    }

    suspend fun cancelBooking(bookingId: String) {
        bookingsRef.document(bookingId).update("status", Booking.STATUS_CANCELLED).await()
    }
}
