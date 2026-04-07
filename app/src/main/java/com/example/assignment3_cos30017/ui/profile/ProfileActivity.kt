package com.example.assignment3_cos30017.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.BuildConfig
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.repository.WalletRepository
import com.example.assignment3_cos30017.databinding.ActivityProfileBinding
import com.example.assignment3_cos30017.ui.auth.LoginActivity
import com.example.assignment3_cos30017.ui.wallet.WalletActivity
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.util.NavigationHelper
import com.example.assignment3_cos30017.util.SeedData
import com.example.assignment3_cos30017.util.ToolbarBadgeHelper
import com.example.assignment3_cos30017.viewmodel.ChatBadgeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val walletRepository = WalletRepository()
    private val chatBadgeViewModel: ChatBadgeViewModel by viewModels()
    private var chatBadgeViews: ToolbarBadgeHelper.BadgeViews? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        NavigationHelper.setup(this, binding.toolbar)
        chatBadgeViews = ToolbarBadgeHelper.bindActionIconWithBadge(
            activity = this,
            toolbar = binding.toolbar,
            menuItemId = R.id.action_chat,
            iconRes = R.drawable.ic_chat,
            onClick = { /* handled by NavigationHelper */ }
        )

        val user = auth.currentUser ?: return

        // Name + email
        binding.tvUserName.text = user.displayName ?: getString(R.string.generic_user)
        binding.tvUserEmail.text = user.email ?: ""

        // Avatar: Google photo or initials
        val photoUrl = user.photoUrl?.toString()
        if (!photoUrl.isNullOrEmpty()) {
            binding.ivAvatar.visibility = View.VISIBLE
            binding.tvAvatarInitials.visibility = View.GONE
            Glide.with(this).load(photoUrl).into(binding.ivAvatar)
        } else {
            binding.ivAvatar.visibility = View.GONE
            binding.tvAvatarInitials.visibility = View.VISIBLE
            val initials = (user.displayName ?: getString(R.string.generic_user)).take(1).uppercase()
            binding.tvAvatarInitials.text = initials
        }

        // Load credits (live updates)
        lifecycleScope.launch {
            walletRepository.getBalance(user.uid).collect { balance ->
                binding.tvCredits.text = getString(R.string.credits_format, balance)
            }
        }

        binding.btnTopUp.setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }

        if (BuildConfig.DEBUG) {
            binding.tvUserName.setOnLongClickListener {
                showSeedDialog()
                true
            }
        }

        chatBadgeViewModel.unreadConversations.observe(this) { count ->
            ToolbarBadgeHelper.renderCount(chatBadgeViews, count)
        }
    }

    private fun showSeedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.seed_dialog_title)
            .setMessage(R.string.seed_dialog_message)
            .setPositiveButton(R.string.seed_action) { _, _ -> runSeed() }
            .setNegativeButton(R.string.seed_cancel, null)
            .show()
    }

    private fun runSeed() {
        val snackbar = Snackbar.make(binding.root, R.string.seed_in_progress, Snackbar.LENGTH_INDEFINITE)
        snackbar.show()

        lifecycleScope.launch {
            val result = SeedData.seed()
            snackbar.dismiss()
            result.onSuccess { count ->
                Snackbar.make(binding.root, getString(R.string.seed_success, count), Snackbar.LENGTH_LONG).show()
                startActivity(Intent(this@ProfileActivity, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }.onFailure { e ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.seed_failed, e.message ?: getString(R.string.generic_error)),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
}
