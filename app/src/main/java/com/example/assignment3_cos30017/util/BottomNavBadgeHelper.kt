package com.example.assignment3_cos30017.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.example.assignment3_cos30017.R
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavBadgeHelper {

    fun observeNotificationUnread(
        lifecycleOwner: LifecycleOwner,
        bottomNav: BottomNavigationView,
        unreadCount: LiveData<Int>
    ) {
        unreadCount.observe(lifecycleOwner) { count ->
            val badge = bottomNav.getOrCreateBadge(R.id.nav_notifications)
            if (count <= 0) {
                badge.clearNumber()
                badge.isVisible = false
            } else {
                badge.number = count.coerceAtMost(99)
                badge.isVisible = true
            }
        }
    }
}
