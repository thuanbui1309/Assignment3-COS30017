package com.example.assignment3_cos30017.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment3_cos30017.databinding.ActivityChatListBinding
import com.example.assignment3_cos30017.R
import com.example.assignment3_cos30017.ui.adapter.ConversationAdapter
import com.example.assignment3_cos30017.ui.adapter.UserSearchAdapter
import com.example.assignment3_cos30017.util.DialogHelper
import com.example.assignment3_cos30017.util.LocaleHelper
import com.example.assignment3_cos30017.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private val viewModel: ChatViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.inflateMenu(R.menu.menu_me_toolbar)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    DialogHelper.showSettingsBottomSheet(this)
                    true
                }
                else -> false
            }
        }

        val conversationAdapter = ConversationAdapter { conversation ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.id)
                putExtra(ChatActivity.EXTRA_TITLE, conversation.participantNames.values
                    .firstOrNull { it != com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName } ?: "Chat")
            })
        }

        val searchAdapter = UserSearchAdapter { user ->
            lifecycleScope.launch {
                val convoId = viewModel.getOrCreateConversation(user.uid, user.displayName)
                startActivity(Intent(this@ChatListActivity, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CONVERSATION_ID, convoId)
                    putExtra(ChatActivity.EXTRA_TITLE, user.displayName)
                })
                binding.etSearchUser.text?.clear()
            }
        }

        binding.rvConversations.layoutManager = LinearLayoutManager(this)
        binding.rvConversations.adapter = conversationAdapter

        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter

        // Search text watcher
        binding.etSearchUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    viewModel.searchUsers(query)
                    binding.rvConversations.visibility = View.GONE
                    binding.rvSearchResults.visibility = View.VISIBLE
                    binding.tvNoConversations.visibility = View.GONE
                } else {
                    viewModel.clearSearch()
                    binding.rvSearchResults.visibility = View.GONE
                    binding.rvConversations.visibility = View.VISIBLE
                }
            }
        })

        viewModel.loadConversations()
        viewModel.conversations.observe(this) { convos ->
            val isSearching = binding.etSearchUser.text?.isNotBlank() == true
            if (!isSearching) {
                val hasConvos = convos.isNotEmpty()
                binding.rvConversations.visibility = if (hasConvos) View.VISIBLE else View.GONE
                binding.tvNoConversations.visibility = if (hasConvos) View.GONE else View.VISIBLE
            }
            conversationAdapter.submitList(convos)
        }

        viewModel.unreadCounts.observe(this) { counts ->
            conversationAdapter.updateUnreadCounts(counts)
        }

        viewModel.userAvatarUrls.observe(this) { urlsByUserId ->
            conversationAdapter.updateAvatarUrls(urlsByUserId)
        }

        viewModel.searchResults.observe(this) { users ->
            searchAdapter.submitList(users)
        }
    }
}
