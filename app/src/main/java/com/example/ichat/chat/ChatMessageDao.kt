package com.example.ichat.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(msg: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE owner=:owner AND ((sender=:owner AND receiver=:peer) OR (sender=:peer AND receiver=:owner)) ORDER BY ts ASC")
    fun conversation(owner: String, peer: String): Flow<List<ChatMessageEntity>>

    @Query("UPDATE chat_messages SET status=:status WHERE owner=:owner AND messageId=:messageId")
    suspend fun updateStatus(owner: String, messageId: String, status: String)

    @Query("DELETE FROM chat_messages WHERE owner=:owner")
    suspend fun clearByOwner(owner: String)
}
