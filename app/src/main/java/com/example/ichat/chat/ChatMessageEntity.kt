package com.example.ichat.chat

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "chat_messages", indices = [Index(value = ["owner","sender","receiver","ts"])])
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val owner: String,
    val sender: String,
    val receiver: String,
    val content: String,
    val ts: Long,
    val messageId: String = "",
    val status: String = "delivered"
)
