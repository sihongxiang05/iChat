package com.example.ichat.friend

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.ichat.chat.ChatMessage
import com.example.ichat.chat.ChatDatabase
import com.example.ichat.chat.ChatRepository
import com.example.ichat.network.RawTcpClient

class FriendViewModel(app: Application) : AndroidViewModel(app) {
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val prefs = app.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private var me: String = prefs.getString("user_id", "user") ?: "user"
    private val repo = ChatRepository(ChatDatabase.get(app).chatDao())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            RawTcpClient.incoming.collect { msg ->
                _messages.value = _messages.value + msg
                repo.save(owner = me, sender = msg.from, receiver = msg.to, content = msg.content, ts = msg.ts, messageId = msg.id, status = "delivered")
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            RawTcpClient.acks.collect { (id, status) ->
                repo.updateStatus(owner = me, messageId = id, status = status)
            }
        }
    }

    fun loadFriends() {
        viewModelScope.launch(Dispatchers.IO) {
            val t = prefs.getString("token", null) ?: return@launch
            _friends.value = FriendApi.listFriends(t)
        }
    }

    fun addFriend(target: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val t = prefs.getString("token", null) ?: return@launch
            val ok = FriendApi.addFriend(t, target)
            if (ok) {
                _friends.value = FriendApi.listFriends(t)
            }
        }
    }

    fun sendMessage(to: String, content: String) {
        val id = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        RawTcpClient.sendChat(me, to, content, id, now)
        val m = ChatMessage(id = id, from = me, to = to, content = content, ts = now)
        _messages.value = _messages.value + m
        viewModelScope.launch(Dispatchers.IO) {
            repo.save(owner = me, sender = me, receiver = to, content = content, ts = now, messageId = id, status = "pending")
        }
    }

    fun conversation(peer: String) = repo.conversation(owner = me, peer = peer)

    fun setUser(user: String) {
        me = user
    }
}
