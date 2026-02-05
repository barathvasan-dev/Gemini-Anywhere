package com.geminianywhere.app.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
        private const val TAG = "MainActivity"
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
        
        // First launch dialog removed - instructions visible on main screen
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

        // Accessibility Service - Always allow opening settings
        binding.cardAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
        
        // Accessibility Info Icon
        binding.iconAccessibilityInfo.setOnClickListener {
            showInfoDialog(
                "Accessibility Service",
                "Required to detect when you type @gemini in any text field across all apps. This allows the AI assistant to be activated contextually.\n\nYour privacy is protected - text is only analyzed when the trigger word is detected."
            )
        }

        // Overlay Permission - Always allow opening settings
        binding.cardOverlay.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        
        // Overlay Info Icon
        binding.iconOverlayInfo.setOnClickListener {
            showInfoDialog(
                "Display Over Apps",
                "Required to show the floating AI assistant button on top of other apps. This allows quick access to Gemini from anywhere on your device."
            )
        }
        
        // Microphone Permission Card
        binding.cardMicrophone.setOnClickListener {
            if (!hasMicrophonePermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    PERMISSION_REQUEST_RECORD_AUDIO
                )
            } else {
                // Open app settings directly
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
        
        // Microphone Info Icon
        binding.iconMicrophoneInfo.setOnClickListener {
            showInfoDialog(
                "Microphone Access",
                "Required for voice input feature. Your voice is processed on-device for speech recognition.\n\nYou can enable or disable this permission at any time from your device settings."
            )
        }
        
        // History Button
        binding.btnHistory.setOnClickListener {
            Log.d(TAG, "History button clicked!")
            try {
                val intent = Intent(this, HistoryActivity::class.java)
                Log.d(TAG, "Starting HistoryActivity...")
                startActivity(intent)
                Log.d(TAG, "HistoryActivity started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting HistoryActivity", e)
                showSnackbar("Error opening History: ${e.message}", isSuccess = false)
            }
        }
        
        // Long press to add test data
        binding.btnHistory.setOnLongClickListener {
            addTestData()
            true
        }
        
        // Favorites Button
        binding.btnFavorites.setOnClickListener {
            Log.d(TAG, "Favorites button clicked!")
            try {
                val intent = Intent(this, FavoritesActivity::class.java)
                Log.d(TAG, "Starting FavoritesActivity...")
                startActivity(intent)
                Log.d(TAG, "FavoritesActivity started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting FavoritesActivity", e)
                showSnackbar("Error opening Favorites: ${e.message}", isSuccess = false)
            }
        }
    }
    
    private fun addTestData() {
        try {
            // Import data classes
            val history = com.geminianywhere.app.data.CommandHistory(this)
            val favorites = com.geminianywhere.app.data.FavoritePrompts(this)
            
            // Add test history
            history.add(
                prompt = "What is the capital of France?",
                response = "The capital of France is Paris. It is located in the north-central part of the country on the river Seine.",
                context = "general",
                model = "gemini-2.0-flash"
            )
            
            history.add(
                prompt = "Write a Python function to calculate fibonacci numbers",
                response = "```python\ndef fibonacci(n):\n    if n <= 1:\n        return n\n    return fibonacci(n-1) + fibonacci(n-2)\n```",
                context = "coding",
                model = "gemini-2.5-pro"
            )
            
            history.add(
                prompt = "Explain quantum computing in simple terms",
                response = "Quantum computing uses quantum bits (qubits) that can exist in multiple states simultaneously, allowing for parallel processing of information. This makes them potentially much faster than classical computers for certain types of problems.",
                context = "education",
                model = "gemini-2.0-flash"
            )
            
            // Add test favorites
            favorites.add(
                title = "Code Review Template",
                prompt = "Review the following code and provide feedback on: 1) Code quality 2) Best practices 3) Potential bugs 4) Performance optimizations",
                category = "Work",
                tags = listOf("code", "review", "development")
            )
            
            favorites.add(
                title = "Email Formatter",
                prompt = "Format this as a professional email with proper greeting, body paragraphs, and closing",
                category = "Work",
                tags = listOf("email", "professional", "communication")
            )
            
            favorites.add(
                title = "Story Generator",
                prompt = "Write a short creative story about:",
                category = "Personal",
                tags = listOf("creative", "writing", "fun")
            )
            
            showSnackbar("✓ Test data added! Open History/Favorites to see", isSuccess = true)
            Log.d(TAG, "Test data added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding test data", e)
            showSnackbar("Error adding test data: ${e.message}", isSuccess = false)
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
    
    private fun showInfoDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }
    
    private fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
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
        // This method is called on initial load - don't auto-request microphone
        updatePermissionStatuses()
    }
    
    private fun updatePermissionStatuses() {
        val wasAccessibilityEnabled = binding.tvAccessibilityStatus.text == "✓ Enabled"
        val wasOverlayGranted = binding.tvOverlayStatus.text == "✓ Enabled"
        
        val accessibilityEnabled = isAccessibilityEnabled()
        val overlayGranted = isOverlayPermissionGranted()
        val microphoneGranted = hasMicrophonePermission()

        // Update Accessibility Status
        binding.tvAccessibilityStatus.text = if (accessibilityEnabled) {
            "✓ Enabled"
        } else {
            "Not Enabled"
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

        // Update Overlay Status
        binding.tvOverlayStatus.text = if (overlayGranted) {
            "✓ Enabled"
        } else {
            "Not Enabled"
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
        
        // Update Microphone Status
        binding.tvMicrophoneStatus.text = if (microphoneGranted) {
            "✓ Enabled"
        } else {
            "Not Enabled"
        }
        binding.tvMicrophoneStatus.setTextColor(
            if (microphoneGranted) getColor(R.color.success_green)
            else getColor(R.color.error_red)
        )

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

    // Welcome dialog removed - instructions visible on main screen



    override fun onResume() {
        super.onResume()
        updatePermissionStatuses()
        updateApiKeyStatus()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackbar("✓ Microphone permission granted!")
                updatePermissionStatuses()
            } else {
                showSnackbar("Microphone permission denied", isSuccess = false)
            }
        }
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
