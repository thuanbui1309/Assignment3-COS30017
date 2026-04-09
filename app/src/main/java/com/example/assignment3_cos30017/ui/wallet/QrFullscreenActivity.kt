package com.example.assignment3_cos30017.ui.wallet

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.R

class QrFullscreenActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QR_URL = "extra_qr_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_fullscreen)

        val qrUrl = intent.getStringExtra(EXTRA_QR_URL) ?: run { finish(); return }
        val ivQr = findViewById<ImageView>(R.id.iv_qr_fullscreen)
        Glide.with(this).load(qrUrl).into(ivQr)

        findViewById<ImageView>(R.id.btn_close).setOnClickListener { finish() }
    }
}
