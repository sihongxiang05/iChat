package com.example.ichat.chat

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: ChatMessageDao) {
    suspend fun save(owner: String, sender: String, receiver: String, content: String, ts: Long, messageId: String, status: String) {
        dao.insert(ChatMessageEntity(owner = owner, sender = sender, receiver = receiver, content = content, ts = ts, messageId = messageId, status = status))
    }

    fun conversation(owner: String, peer: String): Flow<List<ChatMessageEntity>> = dao.conversation(owner, peer)

    suspend fun clearByOwner(owner: String) = dao.clearByOwner(owner)
    suspend fun updateStatus(owner: String, messageId: String, status: String) = dao.updateStatus(owner, messageId, status)
}
