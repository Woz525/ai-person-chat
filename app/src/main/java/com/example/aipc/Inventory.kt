package com.example.aipc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class InventoryItem(
    val id: String,
    val name: String,
    val description: String = "",
    val obtainedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("description", description)
        put("obtainedAt", obtainedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): InventoryItem = InventoryItem(
            id = json.getString("id"),
            name = json.getString("name"),
            description = json.optString("description"),
            obtainedAt = json.optLong("obtainedAt")
        )
    }
}

class Inventory(private val context: Context) {
    private val prefs = context.getSharedPreferences("inventory", Context.MODE_PRIVATE)
    private val items = mutableListOf<InventoryItem>()
    private val maxSize = 25

    init {
        load()
    }

    fun getItems(): List<InventoryItem> = items
    fun getSize(): Int = items.size
    fun getMaxSize(): Int = maxSize

    fun addItem(item: InventoryItem): Boolean {
        return if (items.size < maxSize) {
            items.add(item)
            save()
            true
        } else false
    }

    fun removeItem(itemId: String): Boolean {
        val removed = items.removeAll { it.id == itemId }
        if (removed) save()
        return removed
    }

    fun removeItemByName(name: String): Boolean {
        val item = items.find { it.name == name }
        return if (item != null) removeItem(item.id) else false
    }

    fun contains(itemId: String): Boolean = items.any { it.id == itemId }

    private fun save() {
        val jsonArray = JSONArray()
        items.forEach { jsonArray.put(it.toJson()) }
        prefs.edit().putString("items", jsonArray.toString()).apply()
    }

    private fun load() {
        val jsonStr = prefs.getString("items", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        items.clear()
        for (i in 0 until jsonArray.length()) {
            items.add(InventoryItem.fromJson(jsonArray.getJSONObject(i)))
        }
    }
}