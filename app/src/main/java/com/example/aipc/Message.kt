package com.example.aipc

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(), // 唯一标识
    val content: String,
    val isUser: Boolean,
    val isNarration: Boolean = false,
    val isTyping: Boolean = false,
    val isLoading: Boolean = false,
    val isAgentInfo: Boolean = false,   // ★ 新增
    val timestamp: Long = System.currentTimeMillis(),
    var cardVisible: Boolean = true
)