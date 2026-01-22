package com.geminianywhere.app.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.geminianywhere.app.R
import com.geminianywhere.app.databinding.ActivityMainBinding
import com.geminianywhere.app.service.GeminiAccessibilityService
import com.geminianywhere.app.utils.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefManager: PreferenceManager
    
    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 200
    }
    
    private val geminiModels = listOf(
        "gemini-3-pro-preview" to "Gemini 3 Pro Preview",
        "gemini-3-flash-preview" to "Gemini 3 Flash Preview",
        "gemini-2.5-pro" to "Gemini 2.5 Pro",
        "gemini-2.5-flash" to "Gemini 2.5 Flash",
        "gemini-2.5-flash-lite" to "Gemini 2.5 Flash Lite",
        "gemini-flash-latest" to "Gemini Flash Latest",
        "gemini-flash-lite-latest" to "Gemini Flash Lite Latest",
        "gemini-2.0-flash" to "Gemini 2.0 Flash",
        "gemini-2.0-flash-lite" to "Gemini 2.0 Flash Lite",
        "gemini-3-pro-image-preview" to "Gemini 3 Pro Image",
        "gemini-2.5-flash-image" to "Gemini 2.5 Flash Image",
        "gemini-robotics-er-1.5-preview" to "Gemini Robotics-ER 1.5",
        "gemini-2.5-pro-preview-tts" to "Gemini 2.5 Pro TTS",
        "gemini-2.5-flash-preview-tts" to "Gemini 2.5 Flash TTS"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        setupUI()
        checkPermissions()
        animateCardEntrance()
        
        if (prefManager.isFirstLaunch()) {
            showWelcomeDialog()
        }
    }

    private fun setupUI() {
        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // API Key Section
        binding.btnSetApiKey.setOnClickListener {
            showApiKeyDialog()
        }

        // Check if API key is set
        updateApiKeyStatus()
        
        // Model Selection Spinner
        setupModelSpinner()

        // Accessibility Service
        binding.cardAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        // Overlay Permission
        binding.cardOverlay.setOnClickListener {
            requestOverlayPermission()
        }
    }

    private fun setupModelSpinner() {
        val modelNames = geminiModels.map { it.second }
        val adapter = ArrayAdapter(this, R.layout.spinner_item, modelNames)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        binding.spinnerModel.adapter = adapter
        
        // Set custom background
        binding.spinnerModel.setBackgroundResource(R.drawable.spinner_background)
        
        // Set current selection
        val currentModel = prefManager.getSelectedModel()
        val currentIndex = geminiModels.indexOfFirst { it.first == currentModel }
        if (currentIndex >= 0) {
            binding.spinnerModel.setSelection(currentIndex)
        }
        
        // Handle selection changes
        binding.spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var isFirstTime = true
            
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isFirstTime) {
                    val selectedModel = geminiModels[position].first
                    val selectedModelName = geminiModels[position].second
                    prefManager.setSelectedModel(selectedModel)
                    
                    // Animate selection
                    view?.let { animateView(it) }
                    
                    // Show confirmation message
                    showSnackbar("✓ Model selected: $selectedModelName")
                }
                isFirstTime = false
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showApiKeyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_api_key, null)
        val input = dialogView.findViewById<TextInputEditText>(R.id.etApiKey)
        val linkText = dialogView.findViewById<android.widget.TextView>(R.id.tvGetApiKey)
        
        input.setText(prefManager.getApiKey())
        
        linkText.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
            startActivity(intent)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val apiKey = input.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    prefManager.setApiKey(apiKey)
                    updateApiKeyStatus()
                    
                    // Animate API key status update
                    animateView(binding.cardApiKey)
                    
                    // Show success message
                    showSnackbar("✓ API Key saved successfully!")
                } else {
                    showSnackbar("Please enter a valid API key", isSuccess = false)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
        
        // Animate dialog entrance
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
    }

    private fun updateApiKeyStatus() {
        val hasApiKey = prefManager.getApiKey().isNotEmpty()
        binding.tvApiKeyStatus.text = if (hasApiKey) {
            "✓ Set"
        } else {
            "Not Set"
        }
        binding.tvApiKeyStatus.setTextColor(
            if (hasApiKey) getColor(R.color.success_green)
            else getColor(R.color.error_red)
        )
        
        // Animate status change
        if (hasApiKey) {
            animateView(binding.tvApiKeyStatus)
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        if (!isOverlayPermissionGranted()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/${GeminiAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun checkPermissions() {
        val wasAccessibilityEnabled = binding.tvAccessibilityStatus.text == "✓ Enabled"
        val wasOverlayGranted = binding.tvOverlayStatus.text == "✓ Granted"
        
        val accessibilityEnabled = isAccessibilityEnabled()
        val overlayGranted = isOverlayPermissionGranted()
        
        // Check and request microphone permission for voice input
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_RECORD_AUDIO
            )
        }

        binding.tvAccessibilityStatus.text = if (accessibilityEnabled) {
            "✓ Enabled"
        } else {
            "Disabled"
        }
        binding.tvAccessibilityStatus.setTextColor(
            if (accessibilityEnabled) getColor(R.color.success_green)
            else getColor(R.color.error_red)
        )
        
        // Show success message when accessibility is newly enabled
        if (accessibilityEnabled && !wasAccessibilityEnabled) {
            animateView(binding.cardAccessibility)
            showSnackbar("✓ Accessibility Service enabled!")
        }

        binding.tvOverlayStatus.text = if (overlayGranted) {
            "✓ Granted"
        } else {
            "Not Granted"
        }
        binding.tvOverlayStatus.setTextColor(
            if (overlayGranted) getColor(R.color.success_green)
            else getColor(R.color.error_red)
        )
        
        // Show success message when overlay permission is newly granted
        if (overlayGranted && !wasOverlayGranted) {
            animateView(binding.cardOverlay)
            showSnackbar("✓ Display Over Apps permission granted!")
        }

        // Show/hide status indicator with animation
        val allGranted = accessibilityEnabled && overlayGranted && prefManager.getApiKey().isNotEmpty()
        val wasAllGranted = binding.cardStatus.visibility == View.VISIBLE
        
        if (allGranted != wasAllGranted) {
            if (allGranted) {
                binding.cardStatus.visibility = View.VISIBLE
                val animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
                binding.cardStatus.startAnimation(animation)
                showSnackbar("🎉 All set! You're ready to use Gemini Anywhere")
            } else {
                binding.cardStatus.visibility = View.GONE
            }
        }
    }

    private fun showWelcomeDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("One AI. Every textbox.")
            .setMessage(
                "Type @gemini in any app to activate AI assistance.\n\n" +
                "Setup:\n" +
                "1. Set API key\n" +
                "2. Enable Accessibility\n" +
                "3. Allow Overlay"
            )
            .setPositiveButton("Get Started") { _, _ ->
                prefManager.setFirstLaunchComplete()
            }
            .setCancelable(false)
            .show()
    }



    override fun onResume() {
        super.onResume()
        checkPermissions()
        updateApiKeyStatus()
    }
    
    // Animation and UI helper methods
    private fun animateCardEntrance() {
        val cards = listOf(
            binding.cardApiKey,
            binding.cardModel,
            binding.cardAccessibility,
            binding.cardOverlay
        )
        
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 60f
            card.scaleX = 0.95f
            card.scaleY = 0.95f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay((index * 100).toLong())
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()
        }
    }
    
    private fun animateView(view: View) {
        // More professional pulse animation
        view.animate()
            .scaleX(1.03f)
            .scaleY(1.03f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .setInterpolator(OvershootInterpolator(1.5f))
                    .start()
            }
            .start()
    }
    
    private fun showSnackbar(message: String, isSuccess: Boolean = true) {
        val colorRes = if (isSuccess) R.color.success_green else R.color.error_red
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(this, colorRes))
            .setTextColor(ContextCompat.getColor(this, android.R.color.white))
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .apply { view.elevation = 12f }
            .show()
    }
}
