package com.example.assignment3_cos30017.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.model.Conversation
import com.example.assignment3_cos30017.data.model.Message
import com.example.assignment3_cos30017.data.model.User
import com.example.assignment3_cos30017.data.repository.ChatRepository
import com.example.assignment3_cos30017.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _conversations = MutableLiveData<List<Conversation>>()
    val conversations: LiveData<List<Conversation>> = _conversations

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _typingUsers = MutableLiveData<Map<String, Boolean>>()
    val typingUsers: LiveData<Map<String, Boolean>> = _typingUsers

    private val _otherReadTime = MutableLiveData<Long>()
    val otherReadTime: LiveData<Long> = _otherReadTime

    private val _unreadCounts = MutableLiveData<Map<String, Int>>()
    val unreadCounts: LiveData<Map<String, Int>> = _unreadCounts

    private val _searchResults = MutableLiveData<List<User>>()
    val searchResults: LiveData<List<User>> = _searchResults

    private val _userAvatarUrls = MutableLiveData<Map<String, String?>>()
    val userAvatarUrls: LiveData<Map<String, String?>> = _userAvatarUrls

    private val _isSendingImage = MutableLiveData(false)
    val isSendingImage: LiveData<Boolean> = _isSendingImage

    private val _sendImageError = MutableLiveData(false)
    val sendImageError: LiveData<Boolean> = _sendImageError

    private val unreadJobs = mutableMapOf<String, Job>()
    private var searchJob: Job? = null
    private val avatarCache = mutableMapOf<String, String?>()

    /**
     * Local optimistic messages to support "sending" indicator.
     * Key is local temporary id (e.g., "local_...").
     */
    private val pendingMessages = linkedMapOf<String, Message>()

    fun loadConversations() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.getConversations(uid).collectLatest { convos ->
                _conversations.postValue(convos)
                convos.forEach { convo -> loadUnreadCount(convo.id) }
                preloadAvatarsForConversations(convos, uid)
            }
        }
    }

    private fun preloadAvatarsForConversations(convos: List<Conversation>, currentUid: String) {
        viewModelScope.launch {
            convos.forEach { convo ->
                val otherUserId = convo.participants.firstOrNull { it != currentUid }.orEmpty()
                if (otherUserId.isBlank() || avatarCache.containsKey(otherUserId)) return@forEach
                val user = runCatching { userRepository.getUserById(otherUserId) }.getOrNull()
                avatarCache[otherUserId] = user?.photoUrl
                _userAvatarUrls.postValue(avatarCache.toMap())
            }
        }
    }

    private fun loadUnreadCount(conversationId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (unreadJobs.containsKey(conversationId)) return
        unreadJobs[conversationId] = viewModelScope.launch {
            chatRepository.getUnreadCount(conversationId, uid).collectLatest { count ->
                val current = _unreadCounts.value?.toMutableMap() ?: mutableMapOf()
                current[conversationId] = count
                _unreadCounts.postValue(current)
            }
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(conversationId).collectLatest { remote ->
                _messages.postValue(mergeRemoteWithPending(remote))
            }
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        val user = auth.currentUser ?: return
        val now = System.currentTimeMillis()
        val localId = "local_$now"
        val pending = Message(
            id = localId,
            senderId = user.uid,
            senderName = user.displayName ?: "User",
            text = text,
            timestamp = now,
            localStatus = Message.LocalStatus.SENDING
        )
        addPending(pending)
        viewModelScope.launch {
            try {
                // Send the same timestamp so we can match remote snapshot and drop pending.
                val toSend = pending.copy(id = "", localStatus = Message.LocalStatus.SENT)
                chatRepository.sendMessage(conversationId, toSend)
            } catch (_: Exception) {
                markPendingFailed(localId)
            }
            setTyping(conversationId, false)
        }
    }

    fun sendImage(conversationId: String, imageUri: Uri, captionText: String = "") {
        val user = auth.currentUser ?: return
        _isSendingImage.postValue(true)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val localId = "local_$now"
            val pending = Message(
                id = localId,
                senderId = user.uid,
                senderName = user.displayName ?: "User",
                text = captionText,
                timestamp = now,
                type = Message.TYPE_IMAGE,
                imageUrl = null,
                localStatus = Message.LocalStatus.SENDING
            )
            addPending(pending)
            try {
                chatRepository.sendImageMessage(
                    conversationId,
                    imageUri,
                    user.uid,
                    user.displayName ?: "User",
                    captionText,
                    now
                )
                _sendImageError.postValue(false)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "sendImage failed", e)
                _sendImageError.postValue(true)
                markPendingFailed(localId)
            } finally {
                _isSendingImage.postValue(false)
            }
        }
    }

    fun markAsRead(conversationId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch { chatRepository.markAsRead(conversationId, uid) }
    }

    fun setTyping(conversationId: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch { chatRepository.setTyping(conversationId, uid, isTyping) }
    }

    fun observeReadTime(conversationId: String, otherUserId: String) {
        viewModelScope.launch {
            chatRepository.observeReadTime(conversationId, otherUserId)
                .collectLatest { _otherReadTime.postValue(it) }
        }
    }

    fun observeTyping(conversationId: String) {
        viewModelScope.launch {
            chatRepository.observeConversation(conversationId)
                .collectLatest { convo -> _typingUsers.postValue(convo.typing) }
        }
    }

    suspend fun getOrCreateConversation(otherUserId: String, otherUserName: String): String {
        val user = auth.currentUser ?: throw Exception("Not logged in")
        return chatRepository.getOrCreateConversation(
            user.uid, user.displayName ?: "User", otherUserId, otherUserName
        )
    }

    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.postValue(emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            try {
                val users = userRepository.searchUsers(query)
                _searchResults.postValue(users)
            } catch (_: Exception) {
                _searchResults.postValue(emptyList())
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.postValue(emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        unreadJobs.values.forEach { it.cancel() }
    }

    private fun addPending(message: Message) {
        pendingMessages[message.id] = message
        val currentRemote = _messages.value.orEmpty().filterNot { it.id.startsWith("local_") }
        _messages.postValue(mergeRemoteWithPending(currentRemote))
    }

    private fun markPendingFailed(localId: String) {
        val msg = pendingMessages[localId] ?: return
        pendingMessages[localId] = msg.apply { localStatus = Message.LocalStatus.FAILED }
        val currentRemote = _messages.value.orEmpty().filterNot { it.id.startsWith("local_") }
        _messages.postValue(mergeRemoteWithPending(currentRemote))
    }

    private fun mergeRemoteWithPending(remote: List<Message>): List<Message> {
        // Remove pending messages that have been confirmed by remote snapshot.
        val confirmedKeys = remote.asSequence().map { fingerprintKey(it) }.toSet()
        val iterator = pendingMessages.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (confirmedKeys.contains(fingerprintKey(entry.value))) {
                iterator.remove()
            }
        }

        val pending = pendingMessages.values
            .map { it.copy() } // defensive copy for adapter diff
            .sortedBy { it.timestamp }

        return (remote + pending).sortedBy { it.timestamp }
    }

    private fun fingerprintKey(m: Message): String {
        // Good-enough matching for optimistic UI: same sender/timestamp/type/text.
        // For image messages, the remote will have imageUrl but we ignore it.
        return "${m.senderId}|${m.timestamp}|${m.type}|${m.text}"
    }
}
