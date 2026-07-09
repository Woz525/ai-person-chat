package com.example.aipc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileManager {
    private const val AGENT_DIR = "agents"
    private const val CHAT_DIR = "chats"
    private const val AGENT_EXT = ".asf"
    private const val CHAT_EXT = ".acf"

    // 获取所有智能体文件列表
    fun getAllAgents(context: Context): List<Agent> {
        val agentDir = File(context.filesDir, AGENT_DIR)
        if (!agentDir.exists()) return emptyList()
        return agentDir.listFiles { file -> file.extension == "asf" }
            ?.mapNotNull { file ->
                try {
                    val json = file.readText()
                    Agent.fromJson(json)
                } catch (e: Exception) { null }
            } ?: emptyList()
    }

    // 保存智能体（创建或更新）
    fun saveAgent(context: Context, agent: Agent) {
        val agentDir = File(context.filesDir, AGENT_DIR)
        if (!agentDir.exists()) agentDir.mkdirs()
        val file = File(agentDir, "${agent.id}.asf")
        file.writeText(agent.toJson())
    }

    // 删除智能体
    fun deleteAgent(context: Context, agentId: String): Boolean {
        val file = File(context.filesDir, "$AGENT_DIR/$agentId.asf")
        val chatFile = File(context.filesDir, "$CHAT_DIR/$agentId.acf")
        return file.delete() && chatFile.delete()
    }

    // 获取智能体对应的聊天记录文件路径
    private fun getChatFile(context: Context, agentId: String): File {
        val chatDir = File(context.filesDir, CHAT_DIR)
        if (!chatDir.exists()) chatDir.mkdirs()
        return File(chatDir, "$agentId.acf")
    }

    // 保存聊天记录（消息列表）
    fun saveChatHistory(context: Context, agentId: String, messages: List<Message>) {
        val jsonArray = JSONArray()
        messages.forEach { msg ->
            val obj = org.json.JSONObject().apply {
                put("content", msg.content)
                put("isUser", msg.isUser)
                put("isNarration", msg.isNarration)
                put("timestamp", msg.timestamp)
            }
            jsonArray.put(obj)
        }
        getChatFile(context, agentId).writeText(jsonArray.toString())
    }

    // 加载聊天记录
    fun loadChatHistory(context: Context, agentId: String): List<Message> {
        val file = getChatFile(context, agentId)
        if (!file.exists()) return emptyList()
        return try {
            val jsonArray = org.json.JSONArray(file.readText())
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                Message(
                    content = obj.getString("content"),
                    isUser = obj.getBoolean("isUser"),
                    isNarration = obj.optBoolean("isNarration", false),
                    timestamp = obj.getLong("timestamp")
                )
            }
        } catch (e: Exception) { emptyList() }
    }
}