package com.example.assignment3_cos30017.ui.cars

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.databinding.ActivityCarsBinding
import com.example.assignment3_cos30017.ui.chat.ChatListActivity
import com.example.assignment3_cos30017.ui.main.MainActivity
import com.example.assignment3_cos30017.ui.me.MeActivity
import com.example.assignment3_cos30017.ui.notifications.NotificationsActivity
import androidx.activity.viewModels
import com.example.assignment3_cos30017.util.BottomNavBadgeHelper
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.util.NavigationHelper
import com.example.assignment3_cos30017.util.ToolbarBadgeHelper

import com.example.assignment3_cos30017.viewmodel.ChatBadgeViewModel
import com.example.assignment3_cos30017.viewmodel.NotificationsViewModel
import com.google.android.material.tabs.TabLayoutMediator

class CarsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarsBinding
    private val notificationsViewModel: NotificationsViewModel by viewModels()
    private val chatBadgeViewModel: ChatBadgeViewModel by viewModels()
    private var chatBadgeViews: ToolbarBadgeHelper.BadgeViews? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NavigationHelper.setup(this, binding.toolbar)

        chatBadgeViews = ToolbarBadgeHelper.bindActionIconWithBadge(
            activity = this,
            toolbar = binding.toolbar,
            menuItemId = R.id.action_chat,
            iconRes = R.drawable.ic_chat,
            onClick = { startActivity(Intent(this, ChatListActivity::class.java)) }
        )

        val tabTitles = arrayOf(
            getString(R.string.tab_my_cars),
            getString(R.string.tab_my_rentals)
        )

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> MyCarsFragment()
                1 -> MyRentalsFragment()
                else -> MyCarsFragment()
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // Bottom Navigation
        binding.bottomNav.selectedItemId = R.id.nav_cars
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_cars -> true
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_me -> {
                    startActivity(Intent(this, MeActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        BottomNavBadgeHelper.observeNotificationUnread(
            this,
            binding.bottomNav,
            notificationsViewModel.unreadCount
        )
        chatBadgeViewModel.unreadConversations.observe(this) { count ->
            ToolbarBadgeHelper.renderCount(chatBadgeViews, count)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_cars
    }
}
