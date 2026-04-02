package com.example.assignment3_cos30017.data.repository

import android.util.Log
import com.example.assignment3_cos30017.data.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WalletRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getBalance(userId: String): Flow<Int> = callbackFlow {
        val listener = db.collection("wallets").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getBalance listener", error)
                    return@addSnapshotListener
                }
                val balance = snapshot?.getLong("balance")?.toInt() ?: 0
                trySend(balance)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getBalanceOnce(userId: String): Int {
        val doc = db.collection("wallets").document(userId).get().await()
        return doc.getLong("balance")?.toInt() ?: 0
    }

    fun getTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = db.collection(Transaction.COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getTransactions listener", error)
                    return@addSnapshotListener
                }
                val txns = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(txns)
            }
        awaitClose { listener.remove() }
    }

    suspend fun transferCredits(
        fromUserId: String,
        toUserId: String,
        amount: Int,
        fromDescription: String,
        toDescription: String
    ) {
        val (fromAfter, toAfter) = db.runTransaction { txn ->
            val fromWalletRef = db.collection("wallets").document(fromUserId)
            val toWalletRef = db.collection("wallets").document(toUserId)

            val fromBalance = txn.get(fromWalletRef).getLong("balance")?.toInt() ?: 0
            if (fromBalance < amount) throw Exception("Insufficient credits")

            val fromNew = fromBalance - amount
            txn.update(fromWalletRef, "balance", fromNew)
            val toBalance = txn.get(toWalletRef).getLong("balance")?.toInt() ?: 0
            val toNew = toBalance + amount
            txn.update(toWalletRef, "balance", toNew)
            fromNew to toNew
        }.await()

        db.collection(Transaction.COLLECTION).add(
            Transaction(
                userId = fromUserId, type = Transaction.TYPE_RENTAL_PAYMENT,
                amount = -amount, description = fromDescription, balanceAfter = fromAfter
            )
        ).await()
        db.collection(Transaction.COLLECTION).add(
            Transaction(
                userId = toUserId, type = Transaction.TYPE_RENTAL_INCOME,
                amount = amount, description = toDescription, balanceAfter = toAfter
            )
        ).await()
    }

    suspend fun topUp(userId: String, amount: Int, description: String, referenceId: String? = null) {
        val newBalance = db.runTransaction { txn ->
            val walletRef = db.collection("wallets").document(userId)
            val balance = txn.get(walletRef).getLong("balance")?.toInt() ?: 0
            val updated = balance + amount
            txn.update(walletRef, "balance", updated)
            updated
        }.await()
        db.collection(Transaction.COLLECTION).add(
            Transaction(
                userId = userId, type = Transaction.TYPE_TOP_UP,
                amount = amount, description = description,
                referenceId = referenceId, balanceAfter = newBalance
            )
        ).await()
    }

    suspend fun refund(userId: String, amount: Int, description: String) {
        val newBalance = db.runTransaction { txn ->
            val walletRef = db.collection("wallets").document(userId)
            val balance = txn.get(walletRef).getLong("balance")?.toInt() ?: 0
            val updated = balance + amount
            txn.update(walletRef, "balance", updated)
            updated
        }.await()
        db.collection(Transaction.COLLECTION).add(
            Transaction(
                userId = userId, type = Transaction.TYPE_RENTAL_REFUND,
                amount = amount, description = description, balanceAfter = newBalance
            )
        ).await()
    }

    suspend fun holdBid(userId: String, amount: Int, description: String) {
        val newBalance = db.runTransaction { txn ->
            val walletRef = db.collection("wallets").document(userId)
            val balance = txn.get(walletRef).getLong("balance")?.toInt() ?: 0
            if (balance < amount) throw Exception("Insufficient credits")
            val updated = balance - amount
            txn.update(walletRef, "balance", updated)
            updated
        }.await()
        db.collection(Transaction.COLLECTION).add(
            Transaction(
                userId = userId, type = Transaction.TYPE_BID_HOLD,
                amount = -amount, description = description, balanceAfter = newBalance
            )
        ).await()
    }

    suspend fun refundBid(userId: String, amount: Int, description: String) {
        val newBalance = db.runTransaction { txn ->
            val walletRef = db.collection("wallets").document(userId)
            val balance = txn.get(walletRef).getLong("balance")?.toInt() ?: 0
            val updated = balance + amount
            txn.update(walletRef, "balance", updated)
            updated
        }.await()
        db.collection(Transaction.COLLECTION).add(
            Transaction(
                userId = userId, type = Transaction.TYPE_BID_REFUND,
                amount = amount, description = description, balanceAfter = newBalance
            )
        ).await()
    }

    suspend fun creditOwner(ownerId: String, amount: Int, description: String) {
        val newBalance = db.runTransaction { txn ->
            val walletRef = db.collection("wallets").document(ownerId)
            val balance = txn.get(walletRef).getLong("balance")?.toInt() ?: 0
            val updated = balance + amount
            txn.update(walletRef, "balance", updated)
            updated
        }.await()
        db.collection(Transaction.COLLECTION).add(
            Transaction(
                userId = ownerId, type = Transaction.TYPE_RENTAL_INCOME,
                amount = amount, description = description, balanceAfter = newBalance
            )
        ).await()
    }

    companion object {
        private const val TAG = "WalletRepository"
        const val INITIAL_CREDIT = 500
        const val MAX_RENTAL_COST = 400
    }
}
