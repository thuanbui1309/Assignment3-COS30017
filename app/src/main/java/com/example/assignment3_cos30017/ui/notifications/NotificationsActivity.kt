package com.example.assignment3_cos30017.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.databinding.ActivityNotificationsBinding
import com.example.assignment3_cos30017.ui.cars.CarsActivity
import com.example.assignment3_cos30017.ui.chat.ChatListActivity
import com.example.assignment3_cos30017.ui.main.MainActivity
import com.example.assignment3_cos30017.ui.me.MeActivity
import com.example.assignment3_cos30017.util.BottomNavBadgeHelper
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.util.NavigationHelper
import com.example.assignment3_cos30017.util.ToolbarBadgeHelper
import com.example.assignment3_cos30017.viewmodel.ChatBadgeViewModel
import com.example.assignment3_cos30017.viewmodel.NotificationsViewModel

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val notificationsViewModel: NotificationsViewModel by viewModels()
    private val chatBadgeViewModel: ChatBadgeViewModel by viewModels()
    private var chatBadgeViews: ToolbarBadgeHelper.BadgeViews? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NavigationHelper.setup(this, binding.toolbar)
        chatBadgeViews = ToolbarBadgeHelper.bindActionIconWithBadge(
            activity = this,
            toolbar = binding.toolbar,
            menuItemId = R.id.action_chat,
            iconRes = R.drawable.ic_chat,
            onClick = { startActivity(Intent(this, ChatListActivity::class.java)) }
        )

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NotificationsFragment())
                .commit()
        }

        binding.bottomNav.selectedItemId = R.id.nav_notifications
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_cars -> {
                    startActivity(Intent(this, CarsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_notifications -> true
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
        binding.bottomNav.selectedItemId = R.id.nav_notifications
    }
}

