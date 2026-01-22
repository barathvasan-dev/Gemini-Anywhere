package com.geminianywhere.app.api

import android.util.Log
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    @Headers("Content-Type: application/json")
    suspend fun generateContent(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Double = 0.7,
    val topK: Int = 40,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 1024
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?
)

class GeminiApiClient {
    companion object {
        private const val TAG = "GeminiApiClient"
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(GeminiApiService::class.java)

    suspend fun generateResponse(
        apiKey: String,
        prompt: String,
        context: String,
        model: String = "gemini-2.0-flash-exp",
        maxRetries: Int = 2
    ): String {
        Log.d(TAG, "⚡ API Request started - Context: $context")
        val startTime = System.currentTimeMillis()
        
        val contextualPrompt = buildContextualPrompt(prompt, context)
        
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(contextualPrompt))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7,
                maxOutputTokens = 300
            )
        )

        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val response = service.generateContent(model, apiKey, request)
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ API Response received in ${elapsed}ms")
                
                val result = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?: "No response generated"
                    
                return sanitizeMarkdown(result)
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "❌ API Error (attempt ${attempt + 1}/$maxRetries): ${e.message}")
                val errorMessage = e.message?.lowercase() ?: ""
                
                // Check if error is related to model being busy or rate limit
                if ((errorMessage.contains("busy") || 
                     errorMessage.contains("overloaded") ||
                     errorMessage.contains("rate limit") ||
                     errorMessage.contains("429")) && 
                    attempt < maxRetries - 1) {
                    // Fast retry: 500ms only
                    delay(500L)
                } else if (attempt == maxRetries - 1) {
                    // Last attempt failed, throw exception
                    throw e
                } else {
                    // Other errors, throw immediately
                    throw e
                }
            }
        }
        
        throw lastException ?: Exception("Failed after $maxRetries attempts")
    }

    private fun buildContextualPrompt(prompt: String, context: String): String {
        val contextPrefix = when (context) {
            "whatsapp" -> """You're helping compose a WhatsApp message. Be very brief, casual, and conversational.
                |Keep it under 2-3 sentences. Use natural, friendly language.
                |
                |Example:
                |Hey! Sure, I can do that. What time works for you?
                |""".trimMargin()
            
            "messaging" -> "You're helping compose a casual message. Be friendly and concise. "
            
            "email" -> """You're helping write a professional email. Be formal and well-structured.
                |
                |IMPORTANT: Format your response EXACTLY like this:
                |Subject: [concise subject line]
                |
                |[email body with proper greeting and closing]
                |
                |Example:
                |Subject: Meeting Follow-up
                |
                |Dear John,
                |
                |Thank you for taking the time to meet with me today. I wanted to follow up on our discussion about the project timeline.
                |
                |Best regards
                |""".trimMargin()
            
            "linkedin" -> """You're helping create a LinkedIn post. Use the Hook-Body-CTA structure.
                |
                |IMPORTANT: Format your response EXACTLY like this:
                |[Attention-grabbing hook - 1 sentence]
                |
                |[Body - 2-3 paragraphs with value/insights]
                |
                |[Call-to-action - question or engagement prompt]
                |
                |Example:
                |Just closed the biggest deal of my career, and here's what I learned.
                |
                |When I started this journey 3 months ago, I had no idea it would teach me so much about persistence and relationship building. The key wasn't just about the product, it was about understanding the client's real pain points.
                |
                |What's been your biggest career lesson this year? Share in the comments.
                |""".trimMargin()
            
            "social" -> "You're helping create a social media post. Be engaging and creative. "
            "professional" -> "You're helping with professional communication. Be clear and respectful. "
            else -> "You're helping with text composition. Be helpful and clear. "
        }
        
        val plainTextRule = """\n\nCRITICAL OUTPUT RULES:
            |Return output as plain text only.
            |Do NOT use markdown, emojis, bullets, headings, or special formatting.
            |Do NOT use *, **, #, -, or backticks.
            |Provide only the text to be inserted, without any explanation or meta-commentary.""".trimMargin()
        
        return "$contextPrefix User request: $prompt$plainTextRule"
    }
    
    /**
     * Sanitize AI response by removing markdown formatting symbols
     * while preserving actual content (math, passwords, etc.)
     */
    fun sanitizeMarkdown(text: String): String {
        var cleaned = text
        
        // Remove bold (**text** or __text__)
        cleaned = cleaned.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        cleaned = cleaned.replace(Regex("__(.+?)__"), "$1")
        
        // Remove italic (*text* or _text_) - careful not to remove standalone asterisks
        cleaned = cleaned.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "$1")
        cleaned = cleaned.replace(Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)"), "$1")
        
        // Remove inline code (`text`)
        cleaned = cleaned.replace(Regex("`(.+?)`"), "$1")
        
        // Remove heading markers at start of lines (# ## ### etc.)
        cleaned = cleaned.replace(Regex("(?m)^#{1,6}\\s+"), "")
        
        // Remove bullet points at start of lines (-, *, •)
        cleaned = cleaned.replace(Regex("(?m)^[\\-\\*•]\\s+"), "")
        
        // Remove numbered lists (1. 2. etc.)
        cleaned = cleaned.replace(Regex("(?m)^\\d+\\.\\s+"), "")
        
        // Remove blockquote markers
        cleaned = cleaned.replace(Regex("(?m)^>\\s+"), "")
        
        // Remove code block markers (```)
        cleaned = cleaned.replace(Regex("```[a-z]*\\n"), "")
        cleaned = cleaned.replace("```", "")
        
        return cleaned.trim()
    }
}
