package com.geminianywhere.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.geminianywhere.app.R
import com.geminianywhere.app.api.GeminiApiClient
import com.geminianywhere.app.data.CommandHistory
import com.geminianywhere.app.databinding.FloatingButtonLayoutBinding
import com.geminianywhere.app.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingOverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.geminianywhere.app.ACTION_SHOW"
        const val ACTION_HIDE = "com.geminianywhere.app.ACTION_HIDE"
        const val ACTION_VOICE_INPUT = "com.geminianywhere.app.ACTION_VOICE_INPUT"
        private const val TAG = "FloatingOverlay"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "gemini_overlay_channel"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var voiceView: View? = null
    private lateinit var binding: FloatingButtonLayoutBinding
    private lateinit var apiClient: GeminiApiClient
    private lateinit var prefManager: PreferenceManager
    
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private var currentPrompt: String = ""
    private var currentFullText: String = ""
    private var currentContext: String = "general"
    private var isCommand: Boolean = false
    private var commandName: String = ""
    
    private var voiceHandler: VoiceInputHandler? = null
    private var isRecording = false
    private var currentTranscription: String = ""
    
    // Voice UI elements
    private var micContainer: View? = null
    private var micButton: View? = null
    private var micIcon: android.widget.ImageView? = null
    private var statusText: android.widget.TextView? = null
    private var hintText: android.widget.TextView? = null
    private var waveformContainer: android.view.ViewGroup? = null
    private var pulseRing: View? = null
    private val waveViews = mutableListOf<View>()
    private var transcriptionScroll: View? = null
    private var transcriptionText: android.widget.EditText? = null
    private var actionButtons: View? = null
    private var btnClose: android.widget.ImageButton? = null
    private var btnCancel: com.google.android.material.button.MaterialButton? = null
    private var btnAddMore: com.google.android.material.button.MaterialButton? = null
    private var btnSend: com.google.android.material.button.MaterialButton? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== FloatingOverlayService onCreate ===")
        
        // Create notification channel for foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gemini Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Gemini AI floating button"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "✓ Notification channel created")
        }
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        apiClient = GeminiApiClient()
        prefManager = PreferenceManager(this)
        Log.d(TAG, "✓ Service initialized")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        Log.d(TAG, "=== onStartCommand received ===")
        Log.d(TAG, "Intent action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_SHOW -> {
                val fullText = intent.getStringExtra("full_text") ?: ""
                val prompt = intent.getStringExtra("prompt") ?: ""
                currentPrompt = prompt
                currentFullText = fullText
                currentContext = intent.getStringExtra("context") ?: "general"
                isCommand = intent.getBooleanExtra("is_command", false)
                commandName = intent.getStringExtra("command") ?: ""
                
                Log.d(TAG, "ACTION_SHOW: prompt='$prompt', isCommand=$isCommand, command='$commandName'")
                showFloatingButton()
            }
            ACTION_HIDE -> {
                Log.d(TAG, "ACTION_HIDE")
                hideFloatingButton()
            }
            ACTION_VOICE_INPUT -> {
                Log.d(TAG, "ACTION_VOICE_INPUT")
                startVoiceRecording()
            }
            else -> {
                Log.w(TAG, "Unknown action or null intent")
            }
        }
        
        return START_NOT_STICKY
    }

    private fun showFloatingButton() {
        try {
            Log.d(TAG, "=== showFloatingButton called ===")
            
            // Check if floating button is enabled in settings
            if (!prefManager.isFloatingButtonEnabled()) {
                Log.d(TAG, "Floating button disabled in settings")
                return
            }
            
            // Start foreground service on Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Gemini AI")
                    .setContentText("Processing your request...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build()
                    
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Android 14+ requires service type
                    startForeground(
                        NOTIFICATION_ID, 
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                Log.d(TAG, "✓ Foreground service started")
            }
            
            if (floatingView != null) {
                Log.d(TAG, "Hiding existing view")
                hideFloatingButton()
            }

            Log.d(TAG, "Inflating layout")
            binding = FloatingButtonLayoutBinding.inflate(LayoutInflater.from(this))
            floatingView = binding.root

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            Log.d(TAG, "Layout type: $layoutType")

            // Get button size from settings
            val buttonSize = prefManager.getButtonSize()
            val sizeInPx = (buttonSize * resources.displayMetrics.density).toInt()
            
            val params = WindowManager.LayoutParams(
                sizeInPx,
                sizeInPx,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            // Position based on settings
            val position = prefManager.getButtonPosition()
            when (position) {
                "left" -> {
                    params.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    params.x = 16
                }
                "right" -> {
                    params.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    params.x = 16
                }
                "free" -> {
                    params.gravity = Gravity.TOP or Gravity.START
                    params.x = 100
                    params.y = 300
                }
            }

            Log.d(TAG, "Adding view to WindowManager - Size: ${buttonSize}dp, Position: $position")
            windowManager?.addView(floatingView, params)
            Log.d(TAG, "✓ View added successfully!")
            
            // Apply opacity from settings
            val opacity = prefManager.getButtonOpacity() / 100f
            floatingView?.alpha = 0f
            floatingView?.animate()?.alpha(opacity)?.setDuration(200)?.start()
            Log.d(TAG, "✓ Animation started with opacity: $opacity")

            setupButtonListeners(params)
            
        } catch (e: Exception) {
            Log.e(TAG, "ERROR showing button: ${e.message}", e)
            Toast.makeText(this, "Overlay error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtonListeners(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        
        val position = prefManager.getButtonPosition()
        val allowDragging = position == "free"
        
        binding.fabGemini.setOnTouchListener { view, event ->
            Log.d(TAG, "Touch event: ${event.action}")
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    Log.d(TAG, "ACTION_DOWN detected")
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if (allowDragging) {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        // Check if user is dragging (moved more than 10px)
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isDragging = true
                            
                            when (params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                                Gravity.START -> params.x = initialX + deltaX.toInt()
                                Gravity.END -> params.x = initialX - deltaX.toInt()
                                else -> params.x = initialX + deltaX.toInt()
                            }
                            params.y = initialY + deltaY.toInt()
                            windowManager?.updateViewLayout(floatingView, params)
                        }
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "ACTION_UP - isDragging: $isDragging")
                    if (!isDragging) {
                        // It was a click, not a drag
                        Log.d(TAG, "Click detected! Calling handleSendClick()")
                        view.performClick()
                        handleSendClick()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Handles the floating button click event.
     * 
     * Retrieves the latest prompt from the accessibility service,
     * validates the content, calls the Gemini API, and replaces
     * the text in the active field with the AI-generated response.
     * 
     * Includes error handling for missing API keys and empty prompts.
     */
    private fun handleSendClick() {
        Log.d(TAG, "handleSendClick() - START")
        
        // Get fresh prompt from accessibility service (in case user kept typing)
        val serviceInstance = GeminiAccessibilityService.instance
        Log.d(TAG, "Accessibility service instance: ${if (serviceInstance != null) "AVAILABLE" else "NULL"}")
        
        var freshPrompt = serviceInstance?.getCurrentPrompt() ?: currentPrompt
        Log.d(TAG, "Fresh prompt from service: '$freshPrompt', Original prompt: '$currentPrompt', isCommand: $isCommand")
        
        if (freshPrompt == GeminiAccessibilityService.MARKER_EMPTY_CONTENT || (isCommand && freshPrompt.isEmpty())) {
            Log.w(TAG, "No content for command!")
            Toast.makeText(this, "Paste or type text after $commandName", Toast.LENGTH_LONG).show()
            hideFloatingButton()
            return
        }
        
        if (freshPrompt.isEmpty() && !isCommand) {
            Log.w(TAG, "Prompt is empty! Showing toast.")
            Toast.makeText(this, "No prompt detected. Type after trigger", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentPrompt = freshPrompt
        Log.d(TAG, "=== Sending to Gemini ===")
        Log.d(TAG, "Prompt: '$currentPrompt'")
        Log.d(TAG, "Is Command: $isCommand")
        
        // Show loading state
        binding.ivSend.visibility = View.GONE
        binding.fabGemini.alpha = 0.3f
        binding.loadingIndicator.visibility = View.VISIBLE
        
        // Make API call
        serviceScope.launch {
            try {
                val apiKey = prefManager.getApiKey()
                if (apiKey.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@FloatingOverlayService,
                            "Please set API key",
                            Toast.LENGTH_LONG
                        ).show()
                        resetButtonState()
                    }
                    return@launch
                }
                
                Log.d(TAG, "API Key: ${apiKey.take(10)}...")
                val selectedModel = prefManager.getSelectedModel()
                val maxRetries = prefManager.getMaxRetries()
                
                // Add language instruction if preferred language is set
                val preferredLanguage = prefManager.getPreferredLanguage()
                val autoTranslate = prefManager.isAutoTranslateEnabled()
                
                // Build final prompt
                val languagePrompt = buildString {
                    append(currentPrompt)
                    
                    // Add language instruction if needed
                    if (autoTranslate && preferredLanguage != "English") {
                        append("\n\n[Important: Please respond in $preferredLanguage language]")
                    }
                }
                
                Log.d(TAG, "Calling Gemini API with model: $selectedModel, Language: $preferredLanguage")
                val response = apiClient.generateResponse(
                    apiKey = apiKey,
                    prompt = languagePrompt,
                    context = currentContext,
                    model = selectedModel,
                    maxRetries = maxRetries
                )
                
                // Sanitize markdown formatting from response
                val cleanedResponse = apiClient.sanitizeMarkdown(response)
                
                Log.d(TAG, "Got response: ${cleanedResponse.take(50)}...")
                
                // Save to history
                try {
                    val history = CommandHistory(this@FloatingOverlayService)
                    history.add(
                        prompt = currentPrompt,
                        response = cleanedResponse,
                        context = currentContext
                    )
                    Log.d(TAG, "✓ Saved to history")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving to history", e)
                }
                
                withContext(Dispatchers.Main) {
                    // Replace text in the field
                    GeminiAccessibilityService.instance?.replaceTextInCurrentField(cleanedResponse)
                    
                    Toast.makeText(
                        this@FloatingOverlayService,
                        "✓ Text replaced",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Hide with fade out after successful replacement
                    floatingView?.animate()?.alpha(0f)?.setDuration(200)?.withEndAction {
                        hideFloatingButton()
                    }?.start()
                }
                    
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Gemini API", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FloatingOverlayService,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    resetButtonState()
                }
            }
        }
    }

    private fun resetButtonState() {
        try {
            binding.ivSend.visibility = View.VISIBLE
            binding.fabGemini.visibility = View.VISIBLE
            binding.loadingIndicator.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting button state", e)
        }
    }

    private fun hideFloatingButton() {
        val view = floatingView ?: return
        view.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating view", e)
            } finally {
                floatingView = null
            }
        }?.start()
    }
    
    /**
     * Initializes and displays the voice recording overlay UI.
     * 
     * Creates a fullscreen overlay with:
     * - Microphone button for recording control
     * - Waveform animation during recording
     * - Transcription preview with editing capability
     * - Action buttons for replay, cancel, and send
     * 
     * The overlay uses Material Design theming and handles
     * keyboard input for transcription editing.
     */
    private fun startVoiceRecording() {
        try {
            Log.d(TAG, "Showing voice recording UI...")
            
            // Safely remove existing voice view if attached
            if (voiceView != null && voiceView?.parent != null) {
                try {
                    windowManager?.removeView(voiceView)
                } catch (e: Exception) {
                    Log.w(TAG, "Voice view was not attached to window: ${e.message}")
                }
                voiceView = null
            }
            
            // Inflate voice recording overlay with Material theme context
            val themedContext = android.view.ContextThemeWrapper(
                this,
                com.google.android.material.R.style.Theme_MaterialComponents_Light
            )
            voiceView = LayoutInflater.from(themedContext).inflate(
                R.layout.voice_input_overlay,
                null
            )
            
            // Setup window params for voice overlay (fullscreen with dim background)
            val voiceParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            voiceParams.gravity = Gravity.CENTER
            voiceParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                                       WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            
            // Get UI elements
            micContainer = voiceView?.findViewById(R.id.micContainer)
            micButton = voiceView?.findViewById(R.id.micButton)
            micIcon = voiceView?.findViewById(R.id.micIcon)
            statusText = voiceView?.findViewById(R.id.statusText)
            hintText = voiceView?.findViewById(R.id.hintText)
            waveformContainer = voiceView?.findViewById(R.id.waveformContainer)
            pulseRing = voiceView?.findViewById(R.id.pulseRing)
            transcriptionScroll = voiceView?.findViewById(R.id.transcriptionScroll)
            transcriptionText = voiceView?.findViewById(R.id.transcriptionText)
            actionButtons = voiceView?.findViewById(R.id.actionButtons)
            btnClose = voiceView?.findViewById(R.id.btnClose)
            btnCancel = voiceView?.findViewById(R.id.btnCancel)
            btnAddMore = voiceView?.findViewById(R.id.btnAddMore)
            btnSend = voiceView?.findViewById(R.id.btnSend)
            
            // Collect waveform bars
            waveViews.clear()
            voiceView?.findViewById<View>(R.id.wave1)?.let { waveViews.add(it) }
            voiceView?.findViewById<View>(R.id.wave2)?.let { waveViews.add(it) }
            voiceView?.findViewById<View>(R.id.wave3)?.let { waveViews.add(it) }
            voiceView?.findViewById<View>(R.id.wave4)?.let { waveViews.add(it) }
            voiceView?.findViewById<View>(R.id.wave5)?.let { waveViews.add(it) }
            voiceView?.findViewById<View>(R.id.wave6)?.let { waveViews.add(it) }
            voiceView?.findViewById<View>(R.id.wave7)?.let { waveViews.add(it) }
            
            // Initial state - ready to record
            showReadyState()
            
            // Close button - force quit
            btnClose?.setOnClickListener {
                if (isRecording) {
                    voiceHandler?.stopVoiceInput()
                }
                hideVoiceOverlay()
            }
            
            // Mic button click - start/stop recording
            micButton?.setOnClickListener {
                if (!isRecording) {
                    startRecording()
                } else {
                    stopRecording()
                }
            }
            
            // Cancel button
            btnCancel?.setOnClickListener {
                hideVoiceOverlay()
            }
            
            // Add More button - record additional voice to append to existing transcription
            btnAddMore?.setOnClickListener {
                if (!isRecording) {
                    startAddMoreRecording()
                } else {
                    stopAddMoreRecording()
                }
            }
            
            // Send button - process with Gemini
            btnSend?.setOnClickListener {
                val editedText = transcriptionText?.text?.toString() ?: ""
                Log.d(TAG, "Send button clicked, text length: ${editedText.length}")
                Log.d(TAG, "Text to process: ${editedText.take(100)}")
                if (editedText.isNotEmpty()) {
                    sendToGemini(editedText)
                } else {
                    Log.w(TAG, "Send clicked but text is empty!")
                }
            }
            
            // Add to window
            windowManager?.addView(voiceView, voiceParams)
            Log.d(TAG, "Voice overlay displayed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing voice UI", e)
            Toast.makeText(this, "Voice input failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showReadyState() {
        statusText?.text = "Tap to start speaking"
        hintText?.text = "Speak clearly for best results"
        hintText?.visibility = View.VISIBLE
        micIcon?.setImageResource(android.R.drawable.ic_btn_speak_now)
        micButton?.findViewById<com.google.android.material.card.MaterialCardView>(R.id.micButton)
            ?.setCardBackgroundColor(getColor(R.color.primary))
        micButton?.visibility = View.VISIBLE
        waveformContainer?.visibility = View.GONE
        pulseRing?.visibility = View.GONE
        stopWaveAnimation()
        transcriptionScroll?.visibility = View.GONE
        actionButtons?.visibility = View.GONE
        btnAddMore?.visibility = View.GONE
        btnClose?.visibility = View.VISIBLE
        isRecording = false
    }
    
    private fun showRecordingState() {
        statusText?.text = "Listening..."
        hintText?.text = "Tap mic to stop"
        micIcon?.setImageResource(android.R.drawable.ic_btn_speak_now)
        micButton?.findViewById<com.google.android.material.card.MaterialCardView>(R.id.micButton)
            ?.setCardBackgroundColor(getColor(R.color.error_red))
        micContainer?.visibility = View.VISIBLE
        micButton?.visibility = View.VISIBLE
        waveformContainer?.visibility = View.VISIBLE
        pulseRing?.visibility = View.VISIBLE
        startWaveAnimation()
        startPulseAnimation()
        transcriptionScroll?.visibility = View.GONE
        actionButtons?.visibility = View.GONE
        btnAddMore?.visibility = View.GONE
        btnClose?.visibility = View.VISIBLE
        isRecording = true
    }
    
    private fun showPreviewState(transcription: String) {
        statusText?.text = "Edit your message"
        hintText?.visibility = View.GONE
        transcriptionText?.setText(transcription)
        micContainer?.visibility = View.GONE
        waveformContainer?.visibility = View.GONE
        pulseRing?.visibility = View.GONE
        stopWaveAnimation()
        btnAddMore?.visibility = View.VISIBLE
        btnAddMore?.text = "🎤 Tap to Record More"
        transcriptionScroll?.visibility = View.VISIBLE
        actionButtons?.visibility = View.VISIBLE
        micButton?.visibility = View.GONE
        btnClose?.visibility = View.VISIBLE
        isRecording = false
        
        // Request focus and show keyboard for editing
        transcriptionText?.postDelayed({
            transcriptionText?.requestFocus()
            transcriptionText?.setSelection(transcriptionText?.text?.length ?: 0)
            // Request keyboard to show
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(transcriptionText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }
    
    private fun startRecording() {
        showRecordingState()
        
        voiceHandler = VoiceInputHandler(this)
        voiceHandler?.startVoiceInput(object : VoiceInputHandler.VoiceCallback {
            override fun onListening() {
                Log.d(TAG, "Voice: Recording started")
            }
            
            override fun onTranscription(text: String) {
                Log.d(TAG, "Voice: Transcribed - $text")
                serviceScope.launch(Dispatchers.Main) {
                    currentTranscription = text
                    showPreviewState(text)
                }
            }
            
            override fun onError(message: String) {
                Log.e(TAG, "Voice: Error - $message")
                serviceScope.launch(Dispatchers.Main) {
                    hideVoiceOverlay()
                    Toast.makeText(this@FloatingOverlayService, message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }
    
    private fun stopRecording() {
        Log.d(TAG, "Voice: Stopping recording...")
        isRecording = false
        statusText?.text = "Converting speech..."
        micButton?.findViewById<com.google.android.material.card.MaterialCardView>(R.id.micButton)
            ?.setCardBackgroundColor(getColor(R.color.primary))
        voiceHandler?.stopVoiceInput()
        // Status will be updated by onTranscription callback
    }
    
    /**
     * Start recording additional voice input to append to existing transcription
     */
    private fun startAddMoreRecording() {
        Log.d(TAG, "Starting Add More recording...")
        isRecording = true
        
        // Update button to show "Tap to Stop"
        btnAddMore?.text = "⏹️ Tap to Stop Recording"
        btnAddMore?.setStrokeColorResource(R.color.error_red)
        btnAddMore?.setTextColor(getColor(R.color.error_red))
        
        // Show recording indicators
        statusText?.text = "Recording more..."
        hintText?.visibility = View.VISIBLE
        hintText?.text = "Speak to add to your message"
        waveformContainer?.visibility = View.VISIBLE
        startWaveAnimation()
        
        // Start voice input
        voiceHandler = VoiceInputHandler(this)
        voiceHandler?.startVoiceInput(object : VoiceInputHandler.VoiceCallback {
            override fun onListening() {
                Log.d(TAG, "Add More: Recording started")
            }
            
            override fun onTranscription(text: String) {
                Log.d(TAG, "Add More: Transcribed - $text")
                serviceScope.launch(Dispatchers.Main) {
                    // Append new transcription to existing text with a space
                    val existingText = transcriptionText?.text?.toString() ?: ""
                    val updatedText = if (existingText.isNotEmpty()) {
                        "$existingText $text"
                    } else {
                        text
                    }
                    currentTranscription = updatedText
                    showPreviewState(updatedText)
                    Toast.makeText(this@FloatingOverlayService, "✓ Added to message", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onError(message: String) {
                Log.e(TAG, "Add More: Error - $message")
                serviceScope.launch(Dispatchers.Main) {
                    showPreviewState(transcriptionText?.text?.toString() ?: "")
                    Toast.makeText(this@FloatingOverlayService, message, Toast.LENGTH_LONG).show()
                }
            }
        })
    }
    
    /**
     * Stop the Add More recording
     */
    private fun stopAddMoreRecording() {
        Log.d(TAG, "Stopping Add More recording...")
        isRecording = false
        statusText?.text = "Converting speech..."
        btnAddMore?.text = "\ud83c\udfa4 Tap to Record More"
        btnAddMore?.setStrokeColorResource(R.color.primary)
        btnAddMore?.setTextColor(getColor(R.color.primary))
        waveformContainer?.visibility = View.GONE
        stopWaveAnimation()
        voiceHandler?.stopVoiceInput()
        // Status will be updated by onTranscription callback
    }
    
    private fun sendToGemini(transcription: String) {
        Log.d(TAG, "=== sendToGemini called ===")
        Log.d(TAG, "Transcription: ${transcription.take(100)}")
        statusText?.text = "Processing with AI..."
        actionButtons?.visibility = View.GONE
        
        Log.d(TAG, "Calling voiceHandler.processVoiceInput()...")
        voiceHandler?.processVoiceInput(transcription) { result ->
            Log.d(TAG, "=== Callback received from processVoiceInput ===")
            Log.d(TAG, "Result: ${result.take(100)}")
            serviceScope.launch(Dispatchers.Main) {
                if (result.startsWith("Error:")) {
                    Log.e(TAG, "Error result: $result")
                    Toast.makeText(this@FloatingOverlayService, result, Toast.LENGTH_LONG).show()
                    showPreviewState(transcription) // Back to preview on error
                } else {
                    Log.d(TAG, "Success! Hiding overlay and replacing text")
                    // Success - hide overlay first, then replace text
                    hideVoiceOverlay()
                    
                    // Replace @gemini /voice with result and focus
                    Log.d(TAG, "Calling replaceVoiceTrigger...")
                    GeminiAccessibilityService.instance?.replaceVoiceTrigger(result)
                    
                    Toast.makeText(this@FloatingOverlayService, "✓ Voice input completed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        Log.d(TAG, "processVoiceInput() call initiated")
    }
    
    private fun hideVoiceOverlay() {
        try {
            // Hide keyboard before removing view
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            transcriptionText?.windowToken?.let { token ->
                imm?.hideSoftInputFromWindow(token, 0)
            }
            
            // Remove view from window manager
            voiceView?.let { view ->
                try {
                    windowManager?.removeView(view)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing voice view from window", e)
                }
            }
            
            // Clean up resources
            voiceView = null
            isRecording = false
            stopWaveAnimation()
            voiceHandler?.cleanup()
            voiceHandler = null
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding voice overlay", e)
        }
    }
    
    // Waveform animation functions
    private fun startWaveAnimation() {
        waveViews.forEachIndexed { index, view ->
            val animator = android.animation.ObjectAnimator.ofFloat(
                view, 
                "scaleY", 
                0.3f, 1.0f, 0.5f, 1.2f, 0.4f, 1.0f
            ).apply {
                duration = 1200L
                startDelay = (index * 150).toLong()
                repeatCount = android.animation.ObjectAnimator.INFINITE
                repeatMode = android.animation.ObjectAnimator.REVERSE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            }
            animator.start()
            view.tag = animator
        }
    }
    
    private fun stopWaveAnimation() {
        waveViews.forEach { view ->
            (view.tag as? android.animation.ObjectAnimator)?.cancel()
            view.scaleY = 1.0f
        }
    }
    
    private fun startPulseAnimation() {
        pulseRing?.let { ring ->
            val scaleAnimator = android.animation.AnimatorSet().apply {
                playTogether(
                    android.animation.ObjectAnimator.ofFloat(ring, "scaleX", 1.0f, 1.15f).apply {
                        duration = 1000L
                        repeatCount = android.animation.ObjectAnimator.INFINITE
                        repeatMode = android.animation.ObjectAnimator.REVERSE
                    },
                    android.animation.ObjectAnimator.ofFloat(ring, "scaleY", 1.0f, 1.15f).apply {
                        duration = 1000L
                        repeatCount = android.animation.ObjectAnimator.INFINITE
                        repeatMode = android.animation.ObjectAnimator.REVERSE
                    },
                    android.animation.ObjectAnimator.ofFloat(ring, "alpha", 0f, 0.4f).apply {
                        duration = 1000L
                        repeatCount = android.animation.ObjectAnimator.INFINITE
                        repeatMode = android.animation.ObjectAnimator.REVERSE
                    }
                )
            }
            scaleAnimator.start()
            ring.tag = scaleAnimator
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== FloatingOverlayService onDestroy ===")
        
        // Clean up coroutine jobs
        serviceJob.cancel()
        
        // Clean up views and overlays
        hideFloatingButton()
        hideVoiceOverlay()
        
        // Clean up voice handler
        voiceHandler?.cleanup()
        voiceHandler = null
        
        // Stop foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
