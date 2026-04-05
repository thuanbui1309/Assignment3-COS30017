package com.example.assignment3_cos30017.service

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.repository.UserRepository
import com.example.assignment3_cos30017.ui.main.MainActivity
import com.example.assignment3_cos30017.ui.notifications.NotificationsActivity
import com.example.assignment3_cos30017.util.FcmTokenHelper
import com.example.assignment3_cos30017.util.NotificationChannelHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FCMService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenHelper.rememberToken(applicationContext, token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            scope.launch {
                runCatching { UserRepository().updateFcmToken(uid, token) }
                    .onFailure { e -> Log.w(TAG, "onNewToken: Firestore update failed", e) }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; message not shown as notification")
            return
        }

        val openNotifications = message.data["open_screen"] == "notifications"
        val contentIntent = PendingIntent.getActivity(
            this,
            message.messageId?.hashCode() ?: 0,
            Intent(
                this,
                if (openNotifications) NotificationsActivity::class.java else MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                message.data["car_id"]?.let { putExtra(EXTRA_CAR_ID_FROM_PUSH, it) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationChannelHelper.DEFAULT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify((message.messageId ?: title + body).hashCode(), notification)
    }

    companion object {
        private const val TAG = "FCMService"
        const val EXTRA_CAR_ID_FROM_PUSH = "extra_car_id_from_push"
    }
}
