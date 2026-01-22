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
    private var micButton: View? = null
    private var micIcon: android.widget.ImageView? = null
    private var statusText: android.widget.TextView? = null
    private var transcriptionScroll: View? = null
    private var transcriptionText: android.widget.EditText? = null
    private var actionButtons: View? = null
    private var btnClose: android.widget.ImageButton? = null
    private var btnCancel: com.google.android.material.button.MaterialButton? = null
    private var btnReplay: com.google.android.material.button.MaterialButton? = null
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
    
    private fun handleSendClick() {
        Log.d(TAG, "handleSendClick() - START")
        
        // Get fresh prompt from accessibility service (in case user kept typing)
        val serviceInstance = GeminiAccessibilityService.instance
        Log.d(TAG, "Accessibility service instance: ${if (serviceInstance != null) "AVAILABLE" else "NULL"}")
        
        var freshPrompt = serviceInstance?.getCurrentPrompt() ?: currentPrompt
        Log.d(TAG, "Fresh prompt from service: '$freshPrompt', Original prompt: '$currentPrompt', isCommand: $isCommand")
        
        if (freshPrompt == "EMPTY_CONTENT" || (isCommand && freshPrompt.isEmpty())) {
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
                
                Log.d(TAG, "Calling Gemini API with model: $selectedModel")
                val response = apiClient.generateResponse(
                    apiKey = apiKey,
                    prompt = currentPrompt,
                    context = currentContext,
                    model = selectedModel,
                    maxRetries = maxRetries
                )
                
                // Sanitize markdown formatting from response
                val cleanedResponse = apiClient.sanitizeMarkdown(response)
                
                Log.d(TAG, "Got response: ${cleanedResponse.take(50)}...")
                
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
        binding.ivSend.visibility = View.VISIBLE
        binding.fabGemini.visibility = View.VISIBLE
        binding.loadingIndicator.visibility = View.GONE
    }

    private fun hideFloatingButton() {
        floatingView?.let {
            it.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
                windowManager?.removeView(it)
                floatingView = null
            }?.start()
        }
    }
    
    private fun startVoiceRecording() {
        try {
            Log.d(TAG, "Showing voice recording UI...")
            
            if (voiceView != null) {
                windowManager?.removeView(voiceView)
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            voiceParams.gravity = Gravity.CENTER
            
            // Get UI elements
            micButton = voiceView?.findViewById(R.id.micButton)
            micIcon = voiceView?.findViewById(R.id.micIcon)
            statusText = voiceView?.findViewById(R.id.statusText)
            transcriptionScroll = voiceView?.findViewById(R.id.transcriptionScroll)
            transcriptionText = voiceView?.findViewById(R.id.transcriptionText)
            actionButtons = voiceView?.findViewById(R.id.actionButtons)
            btnClose = voiceView?.findViewById(R.id.btnClose)
            btnCancel = voiceView?.findViewById(R.id.btnCancel)
            btnReplay = voiceView?.findViewById(R.id.btnReplay)
            btnSend = voiceView?.findViewById(R.id.btnSend)
            
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
            
            // Replay button - restart recording
            btnReplay?.setOnClickListener {
                showReadyState()
                currentTranscription = ""
            }
            
            // Send button - process with Gemini
            btnSend?.setOnClickListener {
                val editedText = transcriptionText?.text?.toString() ?: ""
                if (editedText.isNotEmpty()) {
                    sendToGemini(editedText)
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
        statusText?.text = "Tap to Start Recording"
        micIcon?.setImageResource(android.R.drawable.ic_btn_speak_now)
        micButton?.findViewById<com.google.android.material.card.MaterialCardView>(R.id.micButton)
            ?.setCardBackgroundColor(getColor(R.color.primary))
        transcriptionScroll?.visibility = View.GONE
        actionButtons?.visibility = View.GONE
        isRecording = false
    }
    
    private fun showRecordingState() {
        statusText?.text = "Listening... (Tap to Stop)"
        micIcon?.setImageResource(android.R.drawable.ic_btn_speak_now)
        micButton?.findViewById<com.google.android.material.card.MaterialCardView>(R.id.micButton)
            ?.setCardBackgroundColor(getColor(R.color.error_red))
        transcriptionScroll?.visibility = View.GONE
        actionButtons?.visibility = View.GONE
        btnClose?.visibility = View.VISIBLE
        isRecording = true
    }
    
    private fun showPreviewState(transcription: String) {
        statusText?.text = "Edit Your Message"
        transcriptionText?.setText(transcription)
        transcriptionScroll?.visibility = View.VISIBLE
        actionButtons?.visibility = View.VISIBLE
        micButton?.visibility = View.GONE
        btnClose?.visibility = View.VISIBLE
        isRecording = false
        
        // Focus and show keyboard for editing
        transcriptionText?.requestFocus()
        transcriptionText?.setSelection(transcriptionText?.text?.length ?: 0)
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
    
    private fun sendToGemini(transcription: String) {
        statusText?.text = "Processing with AI..."
        actionButtons?.visibility = View.GONE
        
        voiceHandler?.processVoiceInput(transcription) { result ->
            serviceScope.launch(Dispatchers.Main) {
                if (result.startsWith("Error:")) {
                    Toast.makeText(this@FloatingOverlayService, result, Toast.LENGTH_LONG).show()
                    showPreviewState(transcription) // Back to preview on error
                } else {
                    // Success - replace @gemini /voice with result
                    GeminiAccessibilityService.instance?.replaceVoiceTrigger(result)
                    hideVoiceOverlay()
                    Toast.makeText(this@FloatingOverlayService, "✓ Voice input completed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun hideVoiceOverlay() {
        try {
            voiceView?.let {
                windowManager?.removeView(it)
                voiceView = null
            }
            isRecording = false
            voiceHandler?.cleanup()
            voiceHandler = null
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding voice overlay", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== FloatingOverlayService onDestroy ===")
        serviceJob.cancel()
        hideFloatingButton()
        hideVoiceOverlay()
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
