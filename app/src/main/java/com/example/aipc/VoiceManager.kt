package com.example.aipc

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class CustomVoice(
    val name: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("filePath", filePath)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): CustomVoice = CustomVoice(
            name = json.getString("name"),
            filePath = json.getString("filePath"),
            createdAt = json.getLong("createdAt")
        )
    }
}

class VoiceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
    private val KEY_VOICES = "custom_voices"

    fun getAllVoices(): List<CustomVoice> {
        val jsonStr = prefs.getString(KEY_VOICES, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        return (0 until jsonArray.length()).map { CustomVoice.fromJson(jsonArray.getJSONObject(it)) }
    }

    fun addVoice(voice: CustomVoice) {
        val list = getAllVoices().toMutableList()
        list.add(voice)
        save(list)
    }

    fun removeVoice(name: String) {
        val list = getAllVoices().filter { it.name != name }
        save(list)
    }

    private fun save(voices: List<CustomVoice>) {
        val jsonArray = JSONArray()
        voices.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString(KEY_VOICES, jsonArray.toString()).apply()
    }

    fun getVoiceFilePath(name: String): String? {
        return getAllVoices().find { it.name == name }?.filePath
    }
}