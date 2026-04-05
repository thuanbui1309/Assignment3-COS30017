package com.example.assignment3_cos30017.util

import android.content.Context
import android.util.Log
import com.example.assignment3_cos30017.data.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object FcmTokenHelper {

    private const val TAG = "FcmTokenHelper"
    private const val PREFS = "fcm_prefs"
    private const val KEY_PENDING = "pending_token"

    fun rememberToken(context: Context, token: String) {
        if (token.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING, token)
            .apply()
    }

    suspend fun syncTokenForUser(context: Context, userId: String) {
        if (userId.isBlank()) return
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var token = prefs.getString(KEY_PENDING, null)?.takeIf { it.isNotBlank() }
        if (token == null) {
            token = fetchCurrentToken()
        }
        if (token.isNullOrBlank()) return
        runCatching {
            UserRepository().updateFcmToken(userId, token)
        }.onFailure { e ->
            Log.w(TAG, "Failed to store FCM token in Firestore", e)
        }
    }

    private suspend fun fetchCurrentToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!cont.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                Log.w(TAG, "getToken failed", task.exception)
                cont.resume(null)
            }
        }
    }
}
