package com.example.aipc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*

data class Agent(
    val id: String,
    val name: String,
    val avatarBase64: String,
    val personality: String,
    val background: String,
    val identity: String,
    val age: String,
    val gender: String,
    val firstMessage: String,
    val hobby: String = "",
    val voice: String = "",
    val backgroundPrompt: String = "",
    var backgroundSource: String = "ai",            // ★ 改为 var
    var manualBackgroundPath: String = "",         // ★ 改为 var
    val createdAt: Long = System.currentTimeMillis(),
    var bgMusicPath: String = "",
) {
    fun toJson(): String = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("avatarBase64", avatarBase64)
        put("personality", personality)
        put("background", background)
        put("identity", identity)
        put("age", age)
        put("gender", gender)
        put("firstMessage", firstMessage)
        put("hobby", hobby)
        put("voice", voice)
        put("backgroundPrompt", backgroundPrompt)
        put("backgroundSource", backgroundSource)    // ★
        put("manualBackgroundPath", manualBackgroundPath) // ★
        put("createdAt", createdAt)
        put("bgMusicPath", bgMusicPath)
    }.toString()

    companion object {
        fun fromJson(json: String): Agent {
            val obj = JSONObject(json)
            return Agent(
                id = obj.getString("id"),
                name = obj.getString("name"),
                avatarBase64 = obj.getString("avatarBase64"),
                personality = obj.getString("personality"),
                background = obj.getString("background"),
                identity = obj.getString("identity"),
                age = obj.getString("age"),
                gender = obj.getString("gender"),
                firstMessage = obj.getString("firstMessage"),
                hobby = obj.optString("hobby", ""),
                voice = obj.optString("voice", ""),
                backgroundPrompt = obj.optString("backgroundPrompt", ""),
                backgroundSource = obj.optString("backgroundSource", "ai"), // ★
                manualBackgroundPath = obj.optString("manualBackgroundPath", ""), // ★
                createdAt = obj.getLong("createdAt"),
                bgMusicPath = obj.optString("bgMusicPath", ""),
            )
        }

        fun bitmapToBase64(bitmap: Bitmap): String {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        }

        fun base64ToBitmap(base64: String): Bitmap {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}