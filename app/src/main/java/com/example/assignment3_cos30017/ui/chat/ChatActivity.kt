package com.example.assignment3_cos30017.ui.chat

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.databinding.ActivityChatBinding
import com.example.assignment3_cos30017.ui.adapter.MessageAdapter
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_TITLE = "chat_title"
    }

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var conversationId: String
    private var currentUserId = ""
    private var isAtBottom = true

    private var pendingImageUri: android.net.Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImageUri = it
            binding.layoutImagePreview.visibility = View.VISIBLE
            com.bumptech.glide.Glide.with(this).load(it).into(binding.ivPendingImage)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: run { finish(); return }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Chat"

        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val adapter = MessageAdapter(
            currentUserId = currentUserId,
            onImageClick = { url ->
                startActivity(android.content.Intent(this, ImageFullscreenActivity::class.java).apply {
                    putExtra(ImageFullscreenActivity.EXTRA_IMAGE_URL, url)
                })
            }
        )
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter
        binding.rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
                isAtBottom = lastVisible >= (adapter.itemCount - 1).coerceAtLeast(0)
                if (isAtBottom) viewModel.markAsRead(conversationId)
            }
        })

        val otherUserId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
            ?.split("_")?.firstOrNull { it != currentUserId } ?: ""

        viewModel.loadMessages(conversationId)
        viewModel.observeTyping(conversationId)
        viewModel.observeReadTime(conversationId, otherUserId)

        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages) {
                if (messages.isNotEmpty()) binding.rvMessages.scrollToPosition(messages.size - 1)
            }
            if (isAtBottom) viewModel.markAsRead(conversationId)
        }

        viewModel.otherReadTime.observe(this) { readTime ->
            adapter.updateReadTime(readTime)
        }

        viewModel.isSendingImage.observe(this) { sending ->
            binding.btnSend.isEnabled = !sending
            binding.btnAttach.isEnabled = !sending
            binding.btnSend.alpha = if (sending) 0.5f else 1.0f
        }

        viewModel.sendImageError.observe(this) { failed ->
            if (failed == true) {
                Toast.makeText(this, getString(R.string.chat_send_image_failed), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.typingUsers.observe(this) { typingMap ->
            val othersTyping = typingMap.filter { it.key != currentUserId && it.value }
            binding.tvTypingIndicator.visibility = if (othersTyping.isNotEmpty()) View.VISIBLE else View.GONE
            if (othersTyping.isNotEmpty()) {
                binding.tvTypingIndicator.text = getString(R.string.typing_indicator)
            }
        }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setTyping(conversationId, !s.isNullOrBlank())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString()?.trim() ?: ""
            if (pendingImageUri != null) {
                val safeUri = copyUriToCache(pendingImageUri!!)
                viewModel.sendImage(conversationId, safeUri, text)
                pendingImageUri = null
                binding.layoutImagePreview.visibility = View.GONE
                binding.etMessage.text?.clear()
            } else if (text.isNotEmpty()) {
                viewModel.sendMessage(conversationId, text)
                binding.etMessage.text?.clear()
            }
        }

        binding.btnAttach.setOnClickListener { imagePicker.launch("image/*") }

        binding.btnRemoveImage.setOnClickListener {
            pendingImageUri = null
            binding.layoutImagePreview.visibility = View.GONE
        }
    }

    private fun copyUriToCache(sourceUri: Uri): Uri {
        val input = contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Cannot open input stream for uri: $sourceUri")
        input.use { ins ->
            val temp = File.createTempFile("chat_image_", ".tmp", cacheDir)
            FileOutputStream(temp).use { outs -> ins.copyTo(outs) }
            return Uri.fromFile(temp)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.setTyping(conversationId, false)
    }

    override fun onResume() {
        super.onResume()
        if (isAtBottom) viewModel.markAsRead(conversationId)
    }
}
