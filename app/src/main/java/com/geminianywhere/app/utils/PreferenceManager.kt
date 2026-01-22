package com.geminianywhere.app.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_CURRENT_CONTEXT = "current_context"
        
        // Context types
        const val CONTEXT_WHATSAPP = "whatsapp"
        const val CONTEXT_MESSAGING = "messaging"
        const val CONTEXT_EMAIL = "email"
        const val CONTEXT_LINKEDIN = "linkedin"
        const val CONTEXT_SOCIAL = "social"
        const val CONTEXT_PROFESSIONAL = "professional"
        const val CONTEXT_GENERAL = "general"
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
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
    }
    
    fun getApiKey(): String {
        return sharedPreferences.getString(KEY_API_KEY, "") ?: ""
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
}
