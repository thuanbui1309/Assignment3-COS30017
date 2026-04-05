package com.example.assignment3_cos30017.util

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.repository.AuthRepository
import com.example.assignment3_cos30017.ui.auth.LoginActivity
import com.example.assignment3_cos30017.ui.chat.ChatListActivity
import com.example.assignment3_cos30017.ui.listings.MyListingsActivity
import com.example.assignment3_cos30017.ui.mycars.MyCarsActivity
import com.example.assignment3_cos30017.ui.profile.ProfileActivity
import com.example.assignment3_cos30017.ui.rentals.MyRentalsActivity
import com.google.android.material.appbar.MaterialToolbar

object NavigationHelper {

    fun setup(activity: AppCompatActivity, toolbar: MaterialToolbar) {
        toolbar.inflateMenu(R.menu.menu_toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_chat -> {
                    if (activity !is ChatListActivity) {
                        activity.startActivity(Intent(activity, ChatListActivity::class.java))
                    }
                    true
                }
                R.id.action_my_cars -> {
                    if (activity !is MyCarsActivity) {
                        activity.startActivity(Intent(activity, MyCarsActivity::class.java))
                    }
                    true
                }
                R.id.action_my_listings -> {
                    if (activity !is MyListingsActivity) {
                        activity.startActivity(Intent(activity, MyListingsActivity::class.java))
                    }
                    true
                }
                R.id.action_my_rentals -> {
                    if (activity !is MyRentalsActivity) {
                        activity.startActivity(Intent(activity, MyRentalsActivity::class.java))
                    }
                    true
                }
                R.id.action_profile -> {
                    if (activity !is ProfileActivity) {
                        activity.startActivity(Intent(activity, ProfileActivity::class.java))
                    }
                    true
                }
                R.id.action_settings -> {
                    DialogHelper.showSettingsBottomSheet(activity)
                    true
                }
                R.id.action_logout -> {
                    AuthRepository().logout()
                    activity.startActivity(
                        Intent(activity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    true
                }
                else -> false
            }
        }
    }
}
