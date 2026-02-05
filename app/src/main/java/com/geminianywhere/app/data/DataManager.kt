package com.geminianywhere.app.data

import android.content.Context
import android.util.Log
import com.geminianywhere.app.utils.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages command history for quick access to recent prompts
 * Stores last 50 commands with timestamps
 */
class CommandHistory(private val context: Context) {
    
    companion object {
        private const val TAG = "CommandHistory"
        private const val KEY_HISTORY = "command_history"
        private const val MAX_HISTORY_SIZE = 50
    }
    
    private val prefs = context.getSharedPreferences("gemini_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    data class HistoryItem(
        val id: String = UUID.randomUUID().toString(),
        val prompt: String,
        val response: String,
        val timestamp: Long = System.currentTimeMillis(),
        val context: String = "general",
        val model: String = "gemini-2.0-flash"
    ) {
        fun getFormattedDate(): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
        
        fun isToday(): Boolean {
            val today = Calendar.getInstance()
            val itemDate = Calendar.getInstance().apply { timeInMillis = timestamp }
            return today.get(Calendar.YEAR) == itemDate.get(Calendar.YEAR) &&
                   today.get(Calendar.DAY_OF_YEAR) == itemDate.get(Calendar.DAY_OF_YEAR)
        }
    }
    
    /**
     * Add new command to history
     */
    fun add(prompt: String, response: String, context: String = "general", model: String = "gemini-2.0-flash") {
        val items = getAll().toMutableList()
        
        // Check for duplicate (same prompt within last hour)
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        val duplicate = items.find { 
            it.prompt == prompt && it.timestamp > oneHourAgo 
        }
        
        if (duplicate == null) {
            val newItem = HistoryItem(prompt = prompt, response = response, context = context, model = model)
            items.add(0, newItem) // Add to beginning
            
            // Keep only last MAX_HISTORY_SIZE items
            if (items.size > MAX_HISTORY_SIZE) {
                items.subList(MAX_HISTORY_SIZE, items.size).clear()
            }
            
            save(items)
            Log.d(TAG, "Added to history: ${prompt.take(50)}...")
        }
    }
    
    /**
     * Get all history items
     */
    fun getAll(): List<HistoryItem> {
        return try {
            val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading history", e)
            emptyList()
        }
    }
    
    /**
     * Get recent items (today + yesterday)
     */
    fun getRecent(limit: Int = 10): List<HistoryItem> {
        val twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000)
        return getAll()
            .filter { it.timestamp > twoDaysAgo }
            .take(limit)
    }
    
    /**
     * Search history by text
     */
    fun search(query: String): List<HistoryItem> {
        if (query.isBlank()) return getAll()
        
        val lowerQuery = query.lowercase()
        return getAll().filter {
            it.prompt.lowercase().contains(lowerQuery) ||
            it.response.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Get history by context
     */
    fun getByContext(context: String): List<HistoryItem> {
        return getAll().filter { it.context == context }
    }
    
    /**
     * Delete specific item
     */
    fun delete(id: String) {
        val items = getAll().toMutableList()
        items.removeAll { it.id == id }
        save(items)
        Log.d(TAG, "Deleted history item: $id")
    }
    
    /**
     * Clear all history
     */
    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
        Log.d(TAG, "History cleared")
    }
    
    /**
     * Get statistics
     */
    fun getStats(): Map<String, Any> {
        val items = getAll()
        return mapOf(
            "total" to items.size,
            "today" to items.count { it.isToday() },
            "contexts" to items.map { it.context }.distinct().size,
            "avgResponseLength" to items.map { it.response.length }.average().toInt()
        )
    }
    
    private fun save(items: List<HistoryItem>) {
        try {
            val json = gson.toJson(items)
            prefs.edit().putString(KEY_HISTORY, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving history", e)
        }
    }
}

/**
 * Manages favorite prompts for quick access
 * Unlimited favorites with tags and categories
 */
class FavoritePrompts(private val context: Context) {
    
    companion object {
        private const val TAG = "FavoritePrompts"
        private const val KEY_FAVORITES = "favorite_prompts"
    }
    
    private val prefs = context.getSharedPreferences("gemini_favorites", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    data class FavoriteItem(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val prompt: String,
        val category: String = "General",
        val tags: List<String> = emptyList(),
        val timestamp: Long = System.currentTimeMillis(),
        val usageCount: Int = 0
    ) {
        fun matches(query: String): Boolean {
            val lowerQuery = query.lowercase()
            return title.lowercase().contains(lowerQuery) ||
                   prompt.lowercase().contains(lowerQuery) ||
                   category.lowercase().contains(lowerQuery) ||
                   tags.any { it.lowercase().contains(lowerQuery) }
        }
    }
    
    /**
     * Add new favorite
     */
    fun add(title: String, prompt: String, category: String = "General", tags: List<String> = emptyList()): String {
        val items = getAll().toMutableList()
        
        // Check for duplicate title
        if (items.any { it.title == title }) {
            throw IllegalArgumentException("Favorite with title '$title' already exists")
        }
        
        val newItem = FavoriteItem(
            title = title,
            prompt = prompt,
            category = category,
            tags = tags
        )
        
        items.add(newItem)
        save(items)
        
        Log.d(TAG, "Added favorite: $title")
        return newItem.id
    }
    
    /**
     * Get all favorites
     */
    fun getAll(): List<FavoriteItem> {
        return try {
            val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
            val type = object : TypeToken<List<FavoriteItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading favorites", e)
            emptyList()
        }
    }
    
    /**
     * Get favorite by ID
     */
    fun get(id: String): FavoriteItem? {
        return getAll().find { it.id == id }
    }
    
    /**
     * Update favorite
     */
    fun update(id: String, title: String, prompt: String, category: String, tags: List<String>) {
        val items = getAll().toMutableList()
        val index = items.indexOfFirst { it.id == id }
        
        if (index != -1) {
            val updated = items[index].copy(
                title = title,
                prompt = prompt,
                category = category,
                tags = tags
            )
            items[index] = updated
            save(items)
            Log.d(TAG, "Updated favorite: $title")
        }
    }
    
    /**
     * Increment usage count
     */
    fun incrementUsage(id: String) {
        val items = getAll().toMutableList()
        val index = items.indexOfFirst { it.id == id }
        
        if (index != -1) {
            items[index] = items[index].copy(usageCount = items[index].usageCount + 1)
            save(items)
        }
    }
    
    /**
     * Get by category
     */
    fun getByCategory(category: String): List<FavoriteItem> {
        return getAll().filter { it.category == category }
    }
    
    /**
     * Get by tag
     */
    fun getByTag(tag: String): List<FavoriteItem> {
        return getAll().filter { tag in it.tags }
    }
    
    /**
     * Search favorites
     */
    fun search(query: String): List<FavoriteItem> {
        if (query.isBlank()) return getAll()
        return getAll().filter { it.matches(query) }
    }
    
    /**
     * Get most used favorites
     */
    fun getMostUsed(limit: Int = 5): List<FavoriteItem> {
        return getAll()
            .sortedByDescending { it.usageCount }
            .take(limit)
    }
    
    /**
     * Get all categories
     */
    fun getCategories(): List<String> {
        return getAll()
            .map { it.category }
            .distinct()
            .sorted()
    }
    
    /**
     * Get all tags
     */
    fun getTags(): List<String> {
        return getAll()
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }
    
    /**
     * Delete favorite
     */
    fun delete(id: String) {
        val items = getAll().toMutableList()
        items.removeAll { it.id == id }
        save(items)
        Log.d(TAG, "Deleted favorite: $id")
    }
    
    /**
     * Clear all favorites
     */
    fun clear() {
        prefs.edit().remove(KEY_FAVORITES).apply()
        Log.d(TAG, "Favorites cleared")
    }
    
    /**
     * Export favorites as JSON
     */
    fun export(): String {
        return gson.toJson(getAll())
    }
    
    /**
     * Import favorites from JSON
     */
    fun import(json: String): Int {
        return try {
            val type = object : TypeToken<List<FavoriteItem>>() {}.type
            val importedItems: List<FavoriteItem> = gson.fromJson(json, type)
            
            val existingItems = getAll().toMutableList()
            var addedCount = 0
            
            importedItems.forEach { imported ->
                if (existingItems.none { it.title == imported.title }) {
                    existingItems.add(imported)
                    addedCount++
                }
            }
            
            save(existingItems)
            Log.d(TAG, "Imported $addedCount favorites")
            addedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error importing favorites", e)
            0
        }
    }
    
    private fun save(items: List<FavoriteItem>) {
        try {
            val json = gson.toJson(items)
            prefs.edit().putString(KEY_FAVORITES, json).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorites", e)
        }
    }
}
