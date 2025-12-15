package com.example.ichat.chat

data class ChatMessage(
    val id: String,
    val from: String,
    val to: String,
    val content: String,
    val ts: Long
)
