package com.example.assignment3_cos30017.ui.me

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.repository.AuthRepository
import com.example.assignment3_cos30017.data.repository.WalletRepository
import com.example.assignment3_cos30017.databinding.ActivityMeBinding
import com.example.assignment3_cos30017.ui.auth.LoginActivity
import com.example.assignment3_cos30017.ui.cars.CarsActivity
import com.example.assignment3_cos30017.ui.main.MainActivity
import com.example.assignment3_cos30017.ui.notifications.NotificationsActivity
import com.example.assignment3_cos30017.ui.wallet.WalletActivity
import com.example.assignment3_cos30017.util.BottomNavBadgeHelper
import com.example.assignment3_cos30017.util.DialogHelper
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.util.NavigationHelper
import com.example.assignment3_cos30017.util.ToolbarBadgeHelper

import com.example.assignment3_cos30017.viewmodel.ChatBadgeViewModel
import com.example.assignment3_cos30017.viewmodel.NotificationsViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeBinding
    private val authRepository = AuthRepository()
    private val walletRepository = WalletRepository()
    private val notificationsViewModel: NotificationsViewModel by viewModels()
    private val chatBadgeViewModel: ChatBadgeViewModel by viewModels()
    private var chatBadgeViews: ToolbarBadgeHelper.BadgeViews? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            binding.tvUserName.text = user.displayName ?: getString(R.string.generic_user)
            binding.tvUserEmail.text = user.email ?: ""

            val photoUrl = user.photoUrl
            if (photoUrl != null) {
                binding.ivAvatar.visibility = View.VISIBLE
                binding.tvAvatarInitials.visibility = View.GONE
                com.bumptech.glide.Glide.with(this).load(photoUrl).into(binding.ivAvatar)
            } else {
                binding.ivAvatar.visibility = View.GONE
                binding.tvAvatarInitials.visibility = View.VISIBLE
                binding.tvAvatarInitials.text = (user.displayName ?: getString(R.string.generic_user)).take(1).uppercase()
            }
        }

        NavigationHelper.setup(this, binding.toolbar)

        chatBadgeViews = ToolbarBadgeHelper.bindActionIconWithBadge(
            activity = this,
            toolbar = binding.toolbar,
            menuItemId = R.id.action_chat,
            iconRes = R.drawable.ic_chat,
            onClick = { startActivity(Intent(this, com.example.assignment3_cos30017.ui.chat.ChatListActivity::class.java)) }
        )

        // Load credits balance
        if (user != null) {
            lifecycleScope.launch {
                walletRepository.getBalance(user.uid).collectLatest { balance ->
                    binding.tvCreditsValue.text = getString(R.string.credits_format, balance)
                }
            }
        }

        binding.btnTopUp.setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            binding.loadingOverlay.visibility = View.VISIBLE
            binding.btnLogout.isEnabled = false
            lifecycleScope.launch {
                authRepository.logout()
                startActivity(Intent(this@MeActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
        }

        // Bottom Navigation
        binding.bottomNav.selectedItemId = R.id.nav_me
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
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_me -> true
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
        binding.bottomNav.selectedItemId = R.id.nav_me
    }
}
