package com.example.assignment3_cos30017.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatBadgeViewModel : ViewModel() {

    private val repo = ChatRepository()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val _unreadConversations = MutableLiveData(0)
    val unreadConversations: LiveData<Int> = _unreadConversations

    init {
        if (userId.isNotBlank()) observe()
    }

    private fun observe() {
        viewModelScope.launch {
            repo.observeUnreadConversationCount(userId).collectLatest { _unreadConversations.postValue(it) }
        }
    }
}

