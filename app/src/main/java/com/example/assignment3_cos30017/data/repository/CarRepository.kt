package com.example.assignment3_cos30017.data.repository

import android.net.Uri
import android.util.Log
import com.example.assignment3_cos30017.data.model.Car
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CarRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val carsRef = db.collection(Car.COLLECTION)

    private fun Car.migrated(docId: String): Car {
        val base = copy(id = docId)
        return if (base.status.isBlank()) base.copy(status = base.effectiveStatus) else base
    }

    fun getAllCars(): Flow<List<Car>> = callbackFlow {
        val listener = carsRef.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getAllCars", error)
                    return@addSnapshotListener
                }
                val cars = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Car::class.java)?.migrated(doc.id)
                } ?: emptyList()
                trySend(cars)
            }
        awaitClose { listener.remove() }
    }

    fun getCarById(carId: String): Flow<Car?> = callbackFlow {
        val listener = carsRef.document(carId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "getCarById", error)
                return@addSnapshotListener
            }
            val car = snapshot?.toObject(Car::class.java)?.migrated(snapshot.id)
            trySend(car)
        }
        awaitClose { listener.remove() }
    }

    fun getCarsByOwner(ownerId: String): Flow<List<Car>> = callbackFlow {
        val listener = carsRef.whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getCarsByOwner", error)
                    return@addSnapshotListener
                }
                val cars = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Car::class.java)?.migrated(doc.id)
                } ?: emptyList()
                trySend(cars)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addCar(car: Car, imageUris: List<Uri>): String {
        val imageUrls = imageUris.map { uri -> uploadImage(uri) }
        val carWithImages = car.copy(imageUrls = imageUrls, status = Car.STATUS_UNLISTED)
        val docRef = carsRef.add(carWithImages).await()
        return docRef.id
    }

    suspend fun updateCar(car: Car) {
        carsRef.document(car.id).set(car).await()
    }

    suspend fun setStatus(carId: String, status: String) {
        carsRef.document(carId).update(
            mapOf("status" to status, "isAvailable" to (status == Car.STATUS_LISTED))
        ).await()
    }

    suspend fun listCar(
        carId: String, dailyCost: Int, maxRentalDays: Int, notes: String,
        latitude: Double = 0.0, longitude: Double = 0.0, locationName: String = ""
    ) {
        val data = mutableMapOf<String, Any>(
            "dailyCost" to dailyCost,
            "maxRentalDays" to maxRentalDays,
            "notes" to notes,
            "status" to Car.STATUS_LISTED,
            "isAvailable" to true,
            "listedAt" to System.currentTimeMillis(),
            "listingId" to UUID.randomUUID().toString()
        )
        if (latitude != 0.0 || longitude != 0.0) {
            data["latitude"] = latitude
            data["longitude"] = longitude
            data["locationName"] = locationName
        }
        carsRef.document(carId).update(data).await()
    }

    suspend fun clearRentalTerms(carId: String) {
        carsRef.document(carId).update(
            mapOf(
                "dailyCost" to 0,
                "maxRentalDays" to 0,
                "notes" to "",
                "status" to Car.STATUS_UNLISTED,
                "isAvailable" to false
            )
        ).await()
    }

    suspend fun deleteCar(carId: String) {
        val doc = carsRef.document(carId).get().await()
        val car = doc.toObject(Car::class.java)
        car?.imageUrls?.forEach { url ->
            try { storage.getReferenceFromUrl(url).delete().await() } catch (_: Exception) {}
        }
        carsRef.document(carId).delete().await()
    }

    suspend fun uploadImagePublic(uri: Uri): String = uploadImage(uri)

    private suspend fun uploadImage(uri: Uri): String {
        val ref = storage.reference.child("cars/${System.currentTimeMillis()}_${uri.lastPathSegment}")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    enum class SortMode { YEAR_DESC, COST_ASC, NEWEST }
    enum class FilterMode { ALL, UNLISTED, LISTED, RENTED }

    companion object {
        private const val TAG = "CarRepository"

        fun filterCars(cars: List<Car>, mode: FilterMode): List<Car> = when (mode) {
            FilterMode.ALL -> cars
            FilterMode.UNLISTED -> cars.filter { it.effectiveStatus == Car.STATUS_UNLISTED }
            FilterMode.LISTED -> cars.filter { it.effectiveStatus == Car.STATUS_LISTED }
            FilterMode.RENTED -> cars.filter { it.effectiveStatus == Car.STATUS_RENTED }
        }

        fun sortCars(cars: List<Car>, mode: SortMode): List<Car> = when (mode) {
            SortMode.YEAR_DESC -> cars.sortedByDescending { it.year }
            SortMode.COST_ASC -> cars.sortedBy { it.dailyCost }
            SortMode.NEWEST -> cars.sortedByDescending { it.createdAt }
        }
    }
}
