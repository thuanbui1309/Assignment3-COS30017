package com.example.assignment3_cos30017.util

import android.util.Log
import com.example.assignment3_cos30017.data.model.Transaction
import com.example.assignment3_cos30017.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object SeedData {

    private const val TAG = "SeedData"
    private const val DEFAULT_PASSWORD = "Test1234!"

    data class SeedUser(
        val displayName: String,
        val email: String,
        val balance: Int
    )

    val USERS = listOf(
        SeedUser("Nguyen Van Minh", "minh.nguyen@gmail.com", 50000),
        SeedUser("Tran Thi Lan", "lan.tran@gmail.com", 35000),
        SeedUser("Le Hoang Nam", "nam.le@gmail.com", 42000),
        SeedUser("Pham Thi Mai", "mai.pham@gmail.com", 28000),
        SeedUser("Vo Duc Thanh", "thanh.vo@gmail.com", 61000),
        SeedUser("Hoang Thi Ngoc", "ngoc.hoang@gmail.com", 19500),
        SeedUser("Dang Quoc Bao", "bao.dang@gmail.com", 55000),
        SeedUser("Bui Thi Huong", "huong.bui@gmail.com", 33000),
        SeedUser("Do Minh Tuan", "tuan.do@gmail.com", 47000),
        SeedUser("Ngo Thi Thao", "thao.ngo@gmail.com", 22000),
        SeedUser("Ly Thanh Dat", "dat.ly@gmail.com", 38000),
        SeedUser("Truong Thi Kim", "kim.truong@gmail.com", 51000),
        SeedUser("Huynh Van Phuc", "phuc.huynh@gmail.com", 29500),
        SeedUser("Dinh Thi Yen", "yen.dinh@gmail.com", 44000),
        SeedUser("Luu Quang Huy", "huy.luu@gmail.com", 36500),
        SeedUser("Phan Thi Linh", "linh.phan@gmail.com", 58000),
        SeedUser("Mai Duc Khoa", "khoa.mai@gmail.com", 41000),
        SeedUser("Duong Thi Ha", "ha.duong@gmail.com", 26000),
        SeedUser("Tang Van Long", "long.tang@gmail.com", 63000),
        SeedUser("Cao Thi Anh", "anh.cao@gmail.com", 31500)
    )

    suspend fun seed(): Result<Int> {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val originalUser = auth.currentUser
        var seeded = 0

        return try {
            for (seedUser in USERS) {
                try {
                    val result = auth.createUserWithEmailAndPassword(
                        seedUser.email, DEFAULT_PASSWORD
                    ).await()
                    val firebaseUser = result.user ?: continue

                    firebaseUser.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(seedUser.displayName)
                            .build()
                    ).await()

                    val uid = firebaseUser.uid
                    val user = User(
                        uid = uid,
                        displayName = seedUser.displayName,
                        email = seedUser.email
                    )
                    db.collection("users").document(uid).set(user).await()
                    db.collection("wallets").document(uid)
                        .set(mapOf("balance" to seedUser.balance)).await()

                    db.collection(Transaction.COLLECTION).add(
                        Transaction(
                            userId = uid,
                            type = Transaction.TYPE_TOP_UP,
                            amount = seedUser.balance,
                            description = "Initial credit deposit",
                            balanceAfter = seedUser.balance
                        )
                    ).await()

                    seeded++
                    Log.d(TAG, "Seeded: ${seedUser.displayName} (${seedUser.email})")
                } catch (e: Exception) {
                    Log.w(TAG, "Skipped ${seedUser.email}: ${e.message}")
                }
            }

            // Sign back into original account
            auth.signOut()
            if (originalUser != null) {
                Log.d(TAG, "Seed complete. Please log in again.")
            }

            Log.d(TAG, "Seeded $seeded / ${USERS.size} users")
            Result.success(seeded)
        } catch (e: Exception) {
            Log.e(TAG, "Seed failed", e)
            Result.failure(e)
        }
    }
}
