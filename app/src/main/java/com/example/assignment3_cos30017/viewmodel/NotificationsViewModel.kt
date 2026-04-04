package com.example.assignment3_cos30017.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.assignment3_cos30017.data.model.AppNotification
import com.example.assignment3_cos30017.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val repo = NotificationRepository()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val _notifications = MutableLiveData<List<AppNotification>>()
    val notifications: LiveData<List<AppNotification>> = _notifications

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    init {
        if (userId.isNotBlank()) observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            repo.observeNotificationsForUser(userId).collectLatest { list ->
                    _notifications.postValue(list)
                    _unreadCount.postValue(list.count { !it.read })
                }
        }
    }
}

