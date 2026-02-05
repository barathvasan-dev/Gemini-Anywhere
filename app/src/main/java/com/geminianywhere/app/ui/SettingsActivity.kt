package com.geminianywhere.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import com.geminianywhere.app.databinding.ActivitySettingsBinding
import com.geminianywhere.app.utils.PreferenceManager
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        // Load trigger settings
        binding.switchEnableTrigger.isChecked = prefManager.isTriggerEnabled()
        binding.etCustomTrigger.setText(prefManager.getCustomTrigger())
        binding.switchShowFloating.isChecked = prefManager.isFloatingButtonEnabled()

        // Load button settings
        when (prefManager.getButtonPosition()) {
            "left" -> binding.rbLeft.isChecked = true
            "right" -> binding.rbRight.isChecked = true
            "free" -> binding.rbFreeMove.isChecked = true
        }

        when (prefManager.getButtonSize()) {
            48 -> binding.rbSmall.isChecked = true
            56 -> binding.rbMedium.isChecked = true
            64 -> binding.rbLarge.isChecked = true
        }

        val opacity = prefManager.getButtonOpacity()
        binding.sliderOpacity.value = opacity.toFloat()
        binding.tvOpacityValue.text = "${opacity}%"

        binding.switchAutoHide.isChecked = prefManager.isAutoHideEnabled()
        
        // Load language settings
        val languages = arrayOf(
            "English", "Spanish", "French", "German", "Italian", "Portuguese",
            "Chinese", "Japanese", "Korean", "Arabic", "Russian", "Hindi",
            "Dutch", "Swedish", "Turkish", "Polish", "Indonesian", "Thai"
        )
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languages)
        binding.actvLanguage.setAdapter(languageAdapter)
        binding.actvLanguage.setText(prefManager.getPreferredLanguage(), false)
        
        binding.switchAutoTranslate.isChecked = prefManager.isAutoTranslateEnabled()
    }

    private fun setupListeners() {
        // Commands management
        binding.cardCommands.setOnClickListener {
            startActivity(android.content.Intent(this, CommandsActivity::class.java))
        }
        
        // Trigger settings
        binding.switchEnableTrigger.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setTriggerEnabled(isChecked)
            showSuccess("Trigger ${if (isChecked) "enabled" else "disabled"}")
        }

        // Debounce handler for custom trigger
        val triggerHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var triggerRunnable: Runnable? = null
        
        binding.etCustomTrigger.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Cancel previous update
                triggerRunnable?.let { triggerHandler.removeCallbacks(it) }
                
                // Schedule new update after 500ms delay
                triggerRunnable = Runnable {
                    val trigger = s.toString().trim()
                    if (trigger.isNotEmpty()) {
                        prefManager.setCustomTrigger(trigger)
                        showSuccess("Trigger word updated: $trigger")
                    }
                }
                triggerHandler.postDelayed(triggerRunnable!!, 500)
            }
        })

        binding.switchShowFloating.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setFloatingButtonEnabled(isChecked)
            showSuccess("Floating button ${if (isChecked) "enabled" else "disabled"}")
        }

        // Button position
        binding.rgPosition.setOnCheckedChangeListener { _, checkedId ->
            val position = when (checkedId) {
                binding.rbLeft.id -> "left"
                binding.rbRight.id -> "right"
                binding.rbFreeMove.id -> "free"
                else -> "right"
            }
            prefManager.setButtonPosition(position)
            showSuccess("Position: ${position.replaceFirstChar { it.uppercase() }}")
        }

        // Button size
        binding.rgSize.setOnCheckedChangeListener { _, checkedId ->
            val size = when (checkedId) {
                binding.rbSmall.id -> 48
                binding.rbMedium.id -> 56
                binding.rbLarge.id -> 64
                else -> 56
            }
            prefManager.setButtonSize(size)
            showSuccess("Size: ${size}dp")
        }

        // Button opacity
        binding.sliderOpacity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val opacity = value.toInt()
                binding.tvOpacityValue.text = "${opacity}%"
                prefManager.setButtonOpacity(opacity)
            }
        }

        // Auto-hide: Automatically hides floating button when user starts typing after trigger
        // Note: Currently stored as preference but implementation pending in FloatingOverlayService
        binding.switchAutoHide.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setAutoHideEnabled(isChecked)
            showSuccess("Auto-hide ${if (isChecked) "enabled" else "disabled"} (Feature in development)")
        }
        
        // Language settings
        binding.actvLanguage.setOnItemClickListener { _, _, position, _ ->
            val languages = arrayOf(
                "English", "Spanish", "French", "German", "Italian", "Portuguese",
                "Chinese", "Japanese", "Korean", "Arabic", "Russian", "Hindi",
                "Dutch", "Swedish", "Turkish", "Polish", "Indonesian", "Thai"
            )
            val selectedLanguage = languages[position]
            prefManager.setPreferredLanguage(selectedLanguage)
            showSuccess("Language set to $selectedLanguage")
        }
        
        binding.switchAutoTranslate.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setAutoTranslateEnabled(isChecked)
            showSuccess("Auto-translate ${if (isChecked) "enabled" else "disabled"}")
        }
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, "✓ $message", Snackbar.LENGTH_SHORT).show()
    }
}
