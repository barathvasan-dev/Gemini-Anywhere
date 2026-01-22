package com.geminianywhere.app.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.geminianywhere.app.api.GeminiApiClient
import com.geminianywhere.app.utils.PreferenceManager

class VoiceInputHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceInputHandler"
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private val apiClient = GeminiApiClient()
    private val prefManager = PreferenceManager(context)
    
    interface VoiceCallback {
        fun onListening()
        fun onTranscription(text: String)
        fun onError(message: String)
    }
    
    fun startVoiceInput(callback: VoiceCallback) {
        try {
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
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches.isNullOrEmpty()) {
                        callback.onError("Couldn't hear you, try again")
                        cleanup()
                        return
                    }
                    
                    val transcription = matches[0]
                    Log.d(TAG, "Transcription: $transcription")
                    
                    if (transcription.isBlank() || transcription.length < 3) {
                        callback.onError("Speech too short, try again")
                        cleanup()
                        return
                    }
                    
                    // Return transcription to UI for preview
                    callback.onTranscription(transcription)
                    cleanup()
                }
                
                override fun onPartialResults(partialResults: Bundle?) {}
                
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Build voice-specific prompt
                val voicePrompt = buildVoicePrompt(transcription)
                
                val apiKey = prefManager.getApiKey()
                if (apiKey.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback("Error: API key not configured")
                    }
                    return@launch
                }
                
                val selectedModel = prefManager.getSelectedModel()
                val maxRetries = prefManager.getMaxRetries()
                
                // Call Gemini API
                val response = apiClient.generateResponse(
                    apiKey = apiKey,
                    prompt = voicePrompt,
                    context = "general",  // No app-specific context for voice
                    model = selectedModel,
                    maxRetries = maxRetries
                )
                
                // Sanitize output
                val cleanedResponse = apiClient.sanitizeMarkdown(response)
                
                withContext(Dispatchers.Main) {
                    callback(cleanedResponse)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing voice input", e)
                withContext(Dispatchers.Main) {
                    callback("Error: AI processing failed")
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
        cleanup()
    }

    fun cleanup() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up speech recognizer", e)
        }
    }
}
