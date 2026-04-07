package com.example.assignment3_cos30017.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.data.repository.AuthRepository
import com.example.assignment3_cos30017.databinding.ActivityLoginBinding
import com.example.assignment3_cos30017.ui.main.MainActivity
import com.example.assignment3_cos30017.util.DialogHelper
import com.example.assignment3_cos30017.util.LocaleHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { handleGoogleSignIn(it) }
        } catch (e: ApiException) {
            showError(getString(R.string.error_google_sign_in))
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (authRepository.isLoggedIn) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()

        binding.btnLogin.setOnClickListener { loginWithEmail() }
        binding.btnGoogle.setOnClickListener { loginWithGoogle() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.btnSettings.setOnClickListener { DialogHelper.showSettingsBottomSheet(this) }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun loginWithEmail() {
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""

        var valid = true

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

        if (!valid) return
        setLoading(true)

        lifecycleScope.launch {
            authRepository.loginWithEmail(email, password)
                .onSuccess { navigateToMain() }
                .onFailure { showError(mapAuthError(it)) }
            setLoading(false)
        }
    }

    private fun loginWithGoogle() {
        googleSignInClient.signOut()
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun handleGoogleSignIn(idToken: String) {
        setLoading(true)
        lifecycleScope.launch {
            authRepository.loginWithGoogle(idToken)
                .onSuccess { navigateToMain() }
                .onFailure { showError(mapAuthError(it)) }
            setLoading(false)
        }
    }

    private fun mapAuthError(e: Throwable): String {
        if (e is FirebaseAuthException) {
            return when (e.errorCode) {
                "ERROR_USER_NOT_FOUND"        -> getString(R.string.error_login_user_not_found)
                "ERROR_WRONG_PASSWORD"        -> getString(R.string.error_login_wrong_password)
                "ERROR_INVALID_CREDENTIAL"    -> getString(R.string.error_login_invalid_credential)
                "ERROR_TOO_MANY_REQUESTS"     -> getString(R.string.error_login_too_many_requests)
                "ERROR_NETWORK_REQUEST_FAILED"-> getString(R.string.error_login_network)
                "ERROR_USER_DISABLED"         -> getString(R.string.error_login_user_disabled)
                else                          -> getString(R.string.error_login)
            }
        }
        return getString(R.string.error_login)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            binding.etEmail.text?.clear()
            binding.etPassword.text?.clear()
            binding.tilEmail.error = null
            binding.tilPassword.error = null
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.layoutLoading?.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnGoogle.isEnabled = !loading
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
