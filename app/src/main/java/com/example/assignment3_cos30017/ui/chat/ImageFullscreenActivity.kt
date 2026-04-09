package com.example.assignment3_cos30017.ui.chat

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.assignment3_cos30017.R

class ImageFullscreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_fullscreen)

        val url = intent.getStringExtra(EXTRA_IMAGE_URL) ?: run { finish(); return }
        val iv = findViewById<ImageView>(R.id.iv_image_fullscreen)
        Glide.with(this).load(url).into(iv)

        findViewById<ImageView>(R.id.btn_close).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }
}

