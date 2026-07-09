package com.example.aipc

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class DataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aipc_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_TYPE = "api_type"
        private const val KEY_CUSTOM_API_KEY = "custom_api_key"
        private const val KEY_CUSTOM_API_URL = "custom_api_url"
        private const val KEY_CUSTOM_MODEL = "custom_model"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_GENDER = "user_gender"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_MODE = "mode"
        private const val KEY_STORY_TYPE = "story_type"
        private const val KEY_FAVORABILITY = "favorability"
        private const val KEY_LOVE_VALUE = "love_value"
        private const val KEY_KNOWN_INFO = "known_info"
        private const val KEY_MEMORIES = "key_memories"
        private const val KEY_AVATAR_PATH = "avatar_path"
        private const val KEY_BOT_NAME = "bot_name"
        private const val KEY_BOT_AVATAR = "bot_avatar"
        private const val KEY_BOT_BACKGROUND = "bot_background"
        private const val KEY_BOT_FIRST_MESSAGE = "bot_first_message"
        private const val KEY_LOCK_FAVORABILITY = "lock_favorability"
        private const val KEY_CONTEXT_LIMIT = "context_limit"
        private const val KEY_CUSTOM_VOICES = "custom_voices"

        // 生图配置
        private const val KEY_IMAGE_API_URL = "image_api_url"
        private const val KEY_IMAGE_API_KEY = "image_api_key"
        private const val KEY_IMAGE_MODEL = "image_model"

        // ★ UI 模式
        private const val KEY_UI_MODE = "ui_mode"
        private const val KEY_BACKGROUND_SOURCE = "background_source" // "ai" 或 "manual"
        private const val KEY_MANUAL_BACKGROUND_PATH = "manual_background_path"

        private const val KEY_BG_MUSIC_ENABLED = "bg_music_enabled"

    }

    // ---- 对话 API ----
    fun setApiType(type: String) { prefs.edit().putString(KEY_API_TYPE, type).apply() }
    fun getApiType(): String = prefs.getString(KEY_API_TYPE, "official") ?: "official"

    fun setCustomApiKey(key: String) { prefs.edit().putString(KEY_CUSTOM_API_KEY, key).apply() }
    fun getCustomApiKey(): String = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""

    fun setCustomApiUrl(url: String) { prefs.edit().putString(KEY_CUSTOM_API_URL, url).apply() }
    fun getCustomApiUrl(): String = prefs.getString(KEY_CUSTOM_API_URL, "") ?: ""

    fun setCustomModel(model: String) { prefs.edit().putString(KEY_CUSTOM_MODEL, model).apply() }
    fun getCustomModel(): String = prefs.getString(KEY_CUSTOM_MODEL, "") ?: ""

    // ---- 生图 API ----
    fun setImageApiUrl(url: String) { prefs.edit().putString(KEY_IMAGE_API_URL, url).apply() }
    fun getImageApiUrl(): String = prefs.getString(KEY_IMAGE_API_URL, "") ?: ""

    fun setImageApiKey(key: String) { prefs.edit().putString(KEY_IMAGE_API_KEY, key).apply() }
    fun getImageApiKey(): String = prefs.getString(KEY_IMAGE_API_KEY, "") ?: ""

    fun setImageModel(model: String) { prefs.edit().putString(KEY_IMAGE_MODEL, model).apply() }
    fun getImageModel(): String = prefs.getString(KEY_IMAGE_MODEL, "") ?: ""

    // ---- UI 模式 ----
    fun setUiMode(mode: String) { prefs.edit().putString(KEY_UI_MODE, mode).apply() }
    fun getUiMode(): String = prefs.getString(KEY_UI_MODE, "classic") ?: "classic"

    // ---- 用户资料 ----
    fun setUserName(name: String) { prefs.edit().putString(KEY_USER_NAME, name).apply() }
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "玩家") ?: "玩家"

    fun setUserGender(gender: String) { prefs.edit().putString(KEY_USER_GENDER, gender).apply() }
    fun getUserGender(): String = prefs.getString(KEY_USER_GENDER, "保密") ?: "保密"

    fun setLanguage(lang: String) { prefs.edit().putString(KEY_LANGUAGE, lang).apply() }
    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "Chinese") ?: "Chinese"

    fun setMode(mode: String) { prefs.edit().putString(KEY_MODE, mode).apply() }
    fun getMode(): String = prefs.getString(KEY_MODE, "纯净") ?: "纯净"

    fun setStoryType(type: String) { prefs.edit().putString(KEY_STORY_TYPE, type).apply() }
    fun getStoryType(): String = prefs.getString(KEY_STORY_TYPE, "真实") ?: "真实"

    // ---- 好感度/爱意值 ----
    fun setFavorability(agentId: String, value: Int) {
        prefs.edit().putInt("${KEY_FAVORABILITY}_$agentId", value).apply()
    }
    fun getFavorability(agentId: String): Int = prefs.getInt("${KEY_FAVORABILITY}_$agentId", 0)

    fun setLoveValue(agentId: String, value: Int) {
        prefs.edit().putInt("${KEY_LOVE_VALUE}_$agentId", value).apply()
    }
    fun getLoveValue(agentId: String): Int = prefs.getInt("${KEY_LOVE_VALUE}_$agentId", 0)

    fun setKnownInfo(agentId: String, info: String) {
        prefs.edit().putString("${KEY_KNOWN_INFO}_$agentId", info).apply()
    }
    fun getKnownInfo(agentId: String): String = prefs.getString("${KEY_KNOWN_INFO}_$agentId", "") ?: ""

    fun setKeyMemories(agentId: String, memories: String) {
        prefs.edit().putString("${KEY_MEMORIES}_$agentId", memories).apply()
    }
    fun getKeyMemories(agentId: String): String = prefs.getString("${KEY_MEMORIES}_$agentId", "") ?: ""

    fun setLockFavorability(agentId: String, lock: Boolean) {
        prefs.edit().putBoolean("${KEY_LOCK_FAVORABILITY}_$agentId", lock).apply()
    }
    fun getLockFavorability(agentId: String): Boolean = prefs.getBoolean("${KEY_LOCK_FAVORABILITY}_$agentId", false)

    // ---- 头像 ----
    fun setAvatarPath(path: String) { prefs.edit().putString(KEY_AVATAR_PATH, path).apply() }
    fun getAvatarPath(): String = prefs.getString(KEY_AVATAR_PATH, "") ?: ""

    // ---- 智能体基础信息 ----
    fun setBotName(name: String) { prefs.edit().putString(KEY_BOT_NAME, name).apply() }
    fun getBotName(): String = prefs.getString(KEY_BOT_NAME, "智能体") ?: "智能体"

    fun setBotAvatar(path: String) { prefs.edit().putString(KEY_BOT_AVATAR, path).apply() }
    fun getBotAvatar(): String = prefs.getString(KEY_BOT_AVATAR, "") ?: ""

    fun setBotBackground(background: String) { prefs.edit().putString(KEY_BOT_BACKGROUND, background).apply() }
    fun getBotBackground(): String = prefs.getString(KEY_BOT_BACKGROUND, "") ?: ""

    fun setBotFirstMessage(message: String) { prefs.edit().putString(KEY_BOT_FIRST_MESSAGE, message).apply() }
    fun getBotFirstMessage(): String = prefs.getString(KEY_BOT_FIRST_MESSAGE, "你好，我是你的智能助手") ?: "你好，我是你的智能助手"

    // ---- 上下文限制 ----
    fun setContextLimit(limit: Int) { prefs.edit().putInt(KEY_CONTEXT_LIMIT, limit).apply() }
    fun getContextLimit(): Int = prefs.getInt(KEY_CONTEXT_LIMIT, 8)

    // ---- 自定义音色 ----
    fun saveCustomVoice(name: String, filePath: String) {
        val voices = getCustomVoices().toMutableMap()
        voices[name] = filePath
        prefs.edit().putString(KEY_CUSTOM_VOICES, JSONObject(voices).toString()).apply()
    }

    fun getCustomVoices(): Map<String, String> {
        val json = prefs.getString(KEY_CUSTOM_VOICES, "{}") ?: "{}"
        val obj = JSONObject(json)
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key ->
            map[key] = obj.getString(key)
        }
        return map
    }


    fun deleteCustomVoice(name: String) {
        val voices = getCustomVoices().toMutableMap()
        voices.remove(name)
        prefs.edit().putString(KEY_CUSTOM_VOICES, JSONObject(voices).toString()).apply()
    }
    fun setBackgroundSource(agentId: String, source: String) {
        prefs.edit().putString("${KEY_BACKGROUND_SOURCE}_$agentId", source).apply()
    }
    fun getBackgroundSource(agentId: String): String {
        return prefs.getString("${KEY_BACKGROUND_SOURCE}_$agentId", "ai") ?: "ai"
    }

    fun setManualBackgroundPath(agentId: String, path: String) {
        prefs.edit().putString("${KEY_MANUAL_BACKGROUND_PATH}_$agentId", path).apply()
    }
    fun getManualBackgroundPath(agentId: String): String {
        return prefs.getString("${KEY_MANUAL_BACKGROUND_PATH}_$agentId", "") ?: ""
    }
    fun setBgMusicEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BG_MUSIC_ENABLED, enabled).apply()
    }
    fun isBgMusicEnabled(): Boolean = prefs.getBoolean(KEY_BG_MUSIC_ENABLED, false)
}