package com.example.assignment3_cos30017.ui.wallet

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.databinding.ActivityWalletBinding
import com.example.assignment3_cos30017.util.LocaleHelper

class WalletActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalletBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WalletFragment())
                .commit()
        }
    }
}
