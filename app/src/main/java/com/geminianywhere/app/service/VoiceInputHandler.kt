package com.geminianywhere.app.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.geminianywhere.app.api.GeminiApiClient
import com.geminianywhere.app.utils.PreferenceManager

class VoiceInputHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceInputHandler"
        private const val STOP_TIMEOUT = 2000L
        private const val MIN_SPEECH_LENGTH = 3
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val apiClient = GeminiApiClient()
    private val prefManager = PreferenceManager(context)
    private var lastPartialResult: String? = null
    private var currentCallback: VoiceCallback? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    
    interface VoiceCallback {
        fun onListening()
        fun onTranscription(text: String)
        fun onError(message: String)
    }
    
    fun startVoiceInput(callback: VoiceCallback) {
        try {
            currentCallback = callback
            lastPartialResult = null
            cancelTimeout()
            
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                callback.onError("Speech recognition not available")
                return
            }
            
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                    callback.onListening()
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Speech started")
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed - could update UI
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {}
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "Speech ended")
                }
                
                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Need microphone permission"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Recognition error"
                    }
                    Log.e(TAG, "Speech recognition error: $errorMessage")
                    callback.onError(errorMessage)
                    cleanup()
                }
                
                override fun onResults(results: Bundle?) {
                    cancelTimeout()  // Results arrived, cancel timeout
                    
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches.isNullOrEmpty()) {
                        // Try to use partial result before giving up
                        val partial = lastPartialResult
                        if (!partial.isNullOrBlank() && partial.length >= MIN_SPEECH_LENGTH) {
                            Log.d(TAG, "No final results, using partial: $partial")
                            callback.onTranscription(partial)
                        } else {
                            callback.onError("Couldn't hear you, try again")
                        }
                        cleanup()
                        return
                    }
                    
                    val transcription = matches[0]
                    Log.d(TAG, "Transcription: $transcription")
                    
                    if (transcription.isBlank() || transcription.length < 3) {
                        // Try partial result if main result too short
                        val partial = lastPartialResult
                        if (!partial.isNullOrBlank() && partial.length >= transcription.length) {
                            Log.d(TAG, "Short result, using partial: $partial")
                            callback.onTranscription(partial)
                        } else {
                            callback.onError("Speech too short, try again")
                        }
                        cleanup()
                        return
                    }
                    
                    // Return transcription to UI for preview
                    callback.onTranscription(transcription)
                    cleanup()
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        lastPartialResult = matches[0]
                        Log.d(TAG, "Partial result: $lastPartialResult")
                    }
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            
            speechRecognizer?.startListening(recognizerIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice input", e)
            callback.onError("Voice input failed: ${e.message}")
            cleanup()
        }
    }
    
    /**
     * Process voice transcription with Gemini API
     * Public method for external calls
     */
    fun processVoiceInput(transcription: String, callback: (String) -> Unit) {
        Log.d(TAG, "=== processVoiceInput called ===")
        Log.d(TAG, "Input transcription: $transcription")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Build voice-specific prompt
                val voicePrompt = buildVoicePrompt(transcription)
                Log.d(TAG, "Built prompt: ${voicePrompt.take(100)}")
                
                val apiKey = prefManager.getApiKey()
                if (apiKey.isEmpty()) {
                    Log.e(TAG, "API key is empty!")
                    withContext(Dispatchers.Main) {
                        callback("Error: API key not configured")
                    }
                    return@launch
                }
                Log.d(TAG, "API key found: ${apiKey.take(10)}...")
                
                val selectedModel = prefManager.getSelectedModel()
                val maxRetries = prefManager.getMaxRetries()
                Log.d(TAG, "Model: $selectedModel, Max retries: $maxRetries")
                
                // Call Gemini API
                Log.d(TAG, "Calling Gemini API...")
                val response = apiClient.generateResponse(
                    apiKey = apiKey,
                    prompt = voicePrompt,
                    context = "general",  // No app-specific context for voice
                    model = selectedModel,
                    maxRetries = maxRetries
                )
                Log.d(TAG, "API response received: ${response.take(100)}")
                
                // Sanitize output
                val cleanedResponse = apiClient.sanitizeMarkdown(response)
                Log.d(TAG, "Cleaned response: ${cleanedResponse.take(100)}")
                
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Calling callback with cleaned response")
                    callback(cleanedResponse)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing voice input", e)
                withContext(Dispatchers.Main) {
                    callback("Error: AI processing failed - ${e.message}")
                }
            }
        }
    }
    private fun buildVoicePrompt(transcription: String): String {
        return """You are an inline AI assistant responding to voice commands.
            |
            |User voice instruction:
            |"$transcription"
            |
            |CRITICAL OUTPUT RULES:
            |Return output as plain text only.
            |Do NOT use markdown, emojis, bullets, headings, or special formatting.
            |Do NOT use *, **, #, -, or backticks.
            |Provide only the text to be inserted, without any explanation or meta-commentary.
            |Keep it natural and conversational.""".trimMargin()
    }

    fun stopVoiceInput() {
        Log.d(TAG, "Stopping voice input...")
        
        // Start timeout - if onResults doesn't fire, use partial result
        scheduleTimeout()
        
        try {
            // This should trigger onResults, but sometimes doesn't
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognizer", e)
            forceComplete()
        }
    }
    
    private fun scheduleTimeout() {
        cancelTimeout()
        timeoutRunnable = Runnable {
            Log.w(TAG, "Timeout waiting for results, using partial result")
            forceComplete()
        }
        timeoutHandler.postDelayed(timeoutRunnable!!, STOP_TIMEOUT)
    }
    
    private fun cancelTimeout() {
        timeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }
    
    private fun forceComplete() {
        cancelTimeout()
        
        val result = lastPartialResult
        if (!result.isNullOrBlank() && result.length >= MIN_SPEECH_LENGTH) {
            Log.d(TAG, "Using partial result: $result")
            currentCallback?.onTranscription(result)
        } else {
            Log.w(TAG, "No speech captured")
            currentCallback?.onError("No speech detected, try again")
        }
        
        cleanup()
    }

    fun cleanup() {
        try {
            cancelTimeout()
            speechRecognizer?.destroy()
            speechRecognizer = null
            currentCallback = null
            lastPartialResult = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up speech recognizer", e)
        }
    }
}
