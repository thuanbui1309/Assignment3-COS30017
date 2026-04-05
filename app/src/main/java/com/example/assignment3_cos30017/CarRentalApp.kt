package com.example.assignment3_cos30017

import android.app.Application
import com.example.assignment3_cos30017.util.NotificationChannelHelper
import com.example.assignment3_cos30017.util.ThemeHelper
import com.google.firebase.FirebaseApp

class CarRentalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationChannelHelper.ensureDefaultChannel(this)
        ThemeHelper.applyOnStartup(this)
    }
}
