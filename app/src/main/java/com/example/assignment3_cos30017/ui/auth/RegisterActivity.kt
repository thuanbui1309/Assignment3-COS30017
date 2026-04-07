package com.example.assignment3_cos30017.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.repository.AuthRepository
import com.example.assignment3_cos30017.databinding.ActivityRegisterBinding
import com.example.assignment3_cos30017.ui.main.MainActivity
import com.example.assignment3_cos30017.util.DialogHelper
import com.example.assignment3_cos30017.util.LocaleHelper
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { register() }
        binding.tvLogin.setOnClickListener { finish() }
        binding.btnSettings.setOnClickListener { DialogHelper.showSettingsBottomSheet(this) }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            binding.etName.text?.clear()
            binding.etEmail.text?.clear()
            binding.etPassword.text?.clear()
            binding.etConfirmPassword.text?.clear()
            binding.tilName.error = null
            binding.tilEmail.error = null
            binding.tilPassword.error = null
            binding.tilConfirmPassword.error = null
        }
    }

    private fun register() {
        val name = binding.etName.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

        var valid = true

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_field_required); valid = false
        } else if (!name.all { it.isLetter() || it.isWhitespace() }) {
            binding.tilName.error = getString(R.string.error_name_letters_only); valid = false
        } else binding.tilName.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_field_required); valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.invalid_email); valid = false
        } else binding.tilEmail.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_field_required); valid = false
        } else if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.error_password_short); valid = false
        } else if (!password.any { it.isDigit() }) {
            binding.tilPassword.error = getString(R.string.error_password_no_digit); valid = false
        } else binding.tilPassword.error = null

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.error_field_required); valid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch); valid = false
        } else binding.tilConfirmPassword.error = null

        if (!valid) return

        setLoading(true)
        lifecycleScope.launch {
            authRepository.registerWithEmail(email, password, name)
                .onSuccess {
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finishAffinity()
                }
                .onFailure { showError(mapAuthError(it)) }
            setLoading(false)
        }
    }

    private fun mapAuthError(e: Throwable): String {
        if (e is FirebaseAuthException) {
            return when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE"  -> getString(R.string.error_register_email_exists)
                "ERROR_NETWORK_REQUEST_FAILED"-> getString(R.string.error_login_network)
                "ERROR_TOO_MANY_REQUESTS"     -> getString(R.string.error_login_too_many_requests)
                else                          -> getString(R.string.error_register)
            }
        }
        return getString(R.string.error_register)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
