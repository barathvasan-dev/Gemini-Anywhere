package com.geminianywhere.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferenceManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)
    
    // Encrypted SharedPreferences for sensitive data (API keys)
    private val encryptedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                "gemini_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating encrypted preferences, falling back to regular", e)
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences("gemini_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }
    
    companion object {
        private const val TAG = "PreferenceManager"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_KEY_MIGRATED = "api_key_migrated"
        private const val KEY_CURRENT_CONTEXT = "current_context"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEY_MAX_RETRIES = "max_retries"
        
        // Trigger settings
        private const val KEY_TRIGGER_ENABLED = "trigger_enabled"
        private const val KEY_CUSTOM_TRIGGER = "custom_trigger"
        private const val KEY_FLOATING_BUTTON_ENABLED = "floating_button_enabled"
        
        // Floating button settings
        private const val KEY_BUTTON_POSITION = "button_position"
        private const val KEY_BUTTON_SIZE = "button_size"
        private const val KEY_BUTTON_OPACITY = "button_opacity"
        private const val KEY_AUTO_HIDE = "auto_hide_enabled"
        
        // Custom commands
        private const val KEY_COMMANDS = "custom_commands"
        
        // Language settings
        private const val KEY_PREFERRED_LANGUAGE = "preferred_language"
        private const val KEY_AUTO_TRANSLATE = "auto_translate_enabled"
        
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_TRIGGER = "@gemini"
        
        // Default commands
        val DEFAULT_COMMANDS = mapOf(
            "/reply" to "You are an assistant replying to the following message. Reply in a polite and professional tone.\n\nMessage:\n{text}\n\nYour reply:",
            "/rewrite" to "Rewrite the following text to be more clear, professional, and well-structured:\n\n{text}",
            "/summarize" to "Summarize the following text in a concise way, highlighting the key points:\n\n{text}",
            "/fix" to "Fix any grammar, spelling, or punctuation errors in the following text:\n\n{text}",
            "/expand" to "Expand the following text with more details and context:\n\n{text}",
            "/shorten" to "Make the following text more concise and to the point:\n\n{text}",
            "/professional" to "Rewrite the following text in a professional business tone:\n\n{text}",
            "/casual" to "Rewrite the following text in a friendly, casual tone:\n\n{text}"
        )
    }
    
    fun setApiKey(apiKey: String) {
        try {
            // Store in encrypted preferences
            encryptedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
            Log.d(TAG, "API key stored securely (encrypted)")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing API key securely", e)
            // Fallback to regular storage if encryption fails
            sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
        }
    }
    
    fun getApiKey(): String {
        try {
            // Check if we need to migrate from old unencrypted storage
            val hasMigrated = sharedPreferences.getBoolean(KEY_API_KEY_MIGRATED, false)
            if (!hasMigrated) {
                migrateApiKeyToEncrypted()
            }
            
            // Get from encrypted preferences
            val encryptedKey = encryptedPreferences.getString(KEY_API_KEY, "")
            if (!encryptedKey.isNullOrEmpty()) {
                return encryptedKey
            }
            
            // Fallback: check regular preferences (shouldn't happen after migration)
            return sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving API key", e)
            // Fallback to regular storage
            return sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        }
    }
    
    /**
     * Migrate API key from plain-text SharedPreferences to EncryptedSharedPreferences
     * This ensures backward compatibility for existing users
     */
    private fun migrateApiKeyToEncrypted() {
        try {
            val oldApiKey = sharedPreferences.getString(KEY_API_KEY, "")
            if (!oldApiKey.isNullOrEmpty()) {
                Log.d(TAG, "Migrating API key to encrypted storage")
                
                // Store in encrypted preferences
                encryptedPreferences.edit().putString(KEY_API_KEY, oldApiKey).apply()
                
                // Remove from plain-text storage
                sharedPreferences.edit().remove(KEY_API_KEY).apply()
                
                Log.d(TAG, "API key successfully migrated and removed from plain-text storage")
            }
            
            // Mark migration as complete
            sharedPreferences.edit().putBoolean(KEY_API_KEY_MIGRATED, true).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error during API key migration", e)
            // Don't mark as migrated if it failed, will retry next time
        }
    }
    
    fun setCurrentContext(context: String) {
        sharedPreferences.edit().putString(KEY_CURRENT_CONTEXT, context).apply()
    }
    
    fun getCurrentContext(): String {
        return sharedPreferences.getString(KEY_CURRENT_CONTEXT, "general") ?: "general"
    }
    
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchComplete() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
    
    fun setSelectedModel(model: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_MODEL, model).apply()
    }
    
    fun getSelectedModel(): String {
        return sharedPreferences.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }
    
    fun setMaxRetries(retries: Int) {
        sharedPreferences.edit().putInt(KEY_MAX_RETRIES, retries).apply()
    }
    
    fun getMaxRetries(): Int {
        return sharedPreferences.getInt(KEY_MAX_RETRIES, DEFAULT_MAX_RETRIES)
    }
    
    // Trigger Settings
    fun setTriggerEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_TRIGGER_ENABLED, enabled).apply()
    }
    
    fun isTriggerEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_TRIGGER_ENABLED, true)
    }
    
    fun setCustomTrigger(trigger: String) {
        sharedPreferences.edit().putString(KEY_CUSTOM_TRIGGER, trigger).apply()
    }
    
    fun getCustomTrigger(): String {
        return sharedPreferences.getString(KEY_CUSTOM_TRIGGER, DEFAULT_TRIGGER) ?: DEFAULT_TRIGGER
    }
    
    fun setFloatingButtonEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FLOATING_BUTTON_ENABLED, enabled).apply()
    }
    
    fun isFloatingButtonEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_FLOATING_BUTTON_ENABLED, true)
    }
    
    // Floating Button Settings
    fun setButtonPosition(position: String) {
        sharedPreferences.edit().putString(KEY_BUTTON_POSITION, position).apply()
    }
    
    fun getButtonPosition(): String {
        return sharedPreferences.getString(KEY_BUTTON_POSITION, "right") ?: "right"
    }
    
    fun setButtonSize(size: Int) {
        sharedPreferences.edit().putInt(KEY_BUTTON_SIZE, size).apply()
    }
    
    fun getButtonSize(): Int {
        return sharedPreferences.getInt(KEY_BUTTON_SIZE, 56)
    }
    
    fun setButtonOpacity(opacity: Int) {
        sharedPreferences.edit().putInt(KEY_BUTTON_OPACITY, opacity).apply()
    }
    
    fun getButtonOpacity(): Int {
        return sharedPreferences.getInt(KEY_BUTTON_OPACITY, 100)
    }
    
    fun setAutoHideEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_HIDE, enabled).apply()
    }
    
    fun isAutoHideEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_HIDE, false)
    }
    
    // Custom Commands
    fun getCommands(): Map<String, String> {
        val json = sharedPreferences.getString(KEY_COMMANDS, null)
        return if (json != null) {
            try {
                // Parse JSON to Map
                val map = mutableMapOf<String, String>()
                json.split("|||").forEach { entry ->
                    val parts = entry.split(":::")
                    if (parts.size == 2) {
                        map[parts[0]] = parts[1]
                    }
                }
                map
            } catch (e: Exception) {
                DEFAULT_COMMANDS
            }
        } else {
            DEFAULT_COMMANDS
        }
    }
    
    fun saveCommands(commands: Map<String, String>) {
        // Convert Map to simple string format
        val json = commands.entries.joinToString("|||") { "${it.key}:::${it.value}" }
        sharedPreferences.edit().putString(KEY_COMMANDS, json).apply()
    }
    
    fun getCommandPrompt(command: String): String? {
        return getCommands()[command]
    }
    
    // Language Settings
    fun setPreferredLanguage(language: String) {
        sharedPreferences.edit().putString(KEY_PREFERRED_LANGUAGE, language).apply()
    }
    
    fun getPreferredLanguage(): String {
        return sharedPreferences.getString(KEY_PREFERRED_LANGUAGE, "English") ?: "English"
    }
    
    fun setAutoTranslateEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_TRANSLATE, enabled).apply()
    }
    
    fun isAutoTranslateEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_TRANSLATE, false)
    }
}
