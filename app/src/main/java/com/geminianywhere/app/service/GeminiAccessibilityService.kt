package com.geminianywhere.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.geminianywhere.app.utils.PreferenceManager

class GeminiAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "GeminiAccessibility"
        var instance: GeminiAccessibilityService? = null
        
        // Special markers for command handling
        const val MARKER_VOICE_INPUT = "VOICE_INPUT"
        const val MARKER_EMPTY_CONTENT = "EMPTY_CONTENT"
    }

    fun getCurrentPrompt(): String {
        return try {
            val text = currentEditText?.text?.toString() ?: ""
            val triggerPattern = prefManager.getCustomTrigger()
            Log.d(TAG, "getCurrentPrompt() - Full text: '$text', Trigger: '$triggerPattern'")
            
            if (text.contains(triggerPattern, ignoreCase = false)) {
                val triggerIndex = text.indexOf(triggerPattern)
                val afterTrigger = text.substring(triggerIndex + triggerPattern.length).trim()
                
                // Check if it's a command (starts with /)
                if (afterTrigger.startsWith("/")) {
                    val commandEnd = afterTrigger.indexOf(" ")
                    val command = if (commandEnd > 0) {
                        afterTrigger.substring(0, commandEnd)
                    } else {
                        afterTrigger
                    }
                    
                    Log.d(TAG, "Command detected: '$command'")
                    
                    // Special handling for /voice command
                    if (command == "/voice") {
                        Log.d(TAG, "Voice command detected - triggering voice input")
                        return MARKER_VOICE_INPUT
                    }
                    
                    // Get command prompt template
                    val commandPrompt = prefManager.getCommandPrompt(command)
                    if (commandPrompt != null) {
                        // Get text after command
                        val userText = if (commandEnd > 0) {
                            afterTrigger.substring(commandEnd).trim()
                        } else {
                            ""
                        }
                        
                        if (userText.isEmpty()) {
                            Log.w(TAG, "No text provided after command $command")
                            return MARKER_EMPTY_CONTENT
                        }
                        
                        Log.d(TAG, "Using text after command: '${userText.take(50)}...'")
                        
                        // Replace {text} with user text
                        val finalPrompt = commandPrompt.replace("{text}", userText)
                        Log.d(TAG, "Final prompt: '${finalPrompt.take(100)}...'")
                        return finalPrompt
                    }
                }
                
                // Regular prompt (no command)
                val prompt = afterTrigger
                Log.d(TAG, "getCurrentPrompt() - Extracted prompt: '$prompt'")
                prompt
            } else {
                Log.d(TAG, "getCurrentPrompt() - No trigger found")
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current prompt", e)
            ""
        }
    }

    private var currentEditText: AccessibilityNodeInfo? = null
    private var lastText: String = ""
    private var currentPackageName: String = ""
    private lateinit var prefManager: PreferenceManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefManager = PreferenceManager(this)
        Log.d(TAG, "=== Gemini Accessibility Service Started ===")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Handle text change and window content change events for better Gmail compatibility
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED && 
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            return
        }
        
        // If focus changed to a different field, hide button
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            val source = event.source
            if (source != null && source.isEditable) {
                // New field focused - reset and hide button
                lastText = ""
                hideFloatingButton()
                source.recycle()
            }
            return
        }
        
        // Get the source node directly from the event
        val source = event.source ?: return
        
        try {
            // Only process editable text fields
            if (!source.isEditable) {
                source.recycle()
                return
            }
            
            checkNodeForTrigger(source)
            source.recycle()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling accessibility event", e)
            source.recycle()
        }
    }

    private fun checkNodeForTrigger(node: AccessibilityNodeInfo) {
        try {
            // Check if trigger is enabled in settings
            if (!prefManager.isTriggerEnabled()) {
                hideFloatingButton()
                return
            }
            
            currentEditText = node
            currentPackageName = node.packageName?.toString() ?: ""
            val currentText = node.text?.toString() ?: ""
            
            val triggerPattern = prefManager.getCustomTrigger()
            
            if (currentText.isEmpty()) {
                // Text cleared - hide button
                hideFloatingButton()
                lastText = ""
                return
            }
            
            Log.d(TAG, "Checking text: '${currentText.take(50)}...' for trigger: '$triggerPattern'")
            
            // Check if text contains the custom trigger
            val hasTrigger = currentText.contains(triggerPattern, ignoreCase = false)
            val hadTrigger = lastText.contains(triggerPattern, ignoreCase = false)
            
            if (hasTrigger && !hadTrigger) {
                Log.d(TAG, "✓ TRIGGER DETECTED!")
                showFloatingBubble(node, currentText)
            } else if (!hasTrigger && hadTrigger) {
                Log.d(TAG, "Trigger removed - hiding button")
                hideFloatingButton()
            } else if (hasTrigger && hadTrigger) {
                // Trigger still present - check if /voice command was just typed
                val triggerIndex = currentText.indexOf(triggerPattern)
                val afterTrigger = currentText.substring(triggerIndex + triggerPattern.length).trim()
                
                if (afterTrigger.startsWith("/voice")) {
                    Log.d(TAG, "Voice command detected during typing!")
                    val intent = Intent(this, FloatingOverlayService::class.java).apply {
                        action = FloatingOverlayService.ACTION_VOICE_INPUT
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    // Hide the regular button if it was showing
                    hideFloatingButton()
                    lastText = currentText
                    return
                }
            } else if (!hasTrigger && !hadTrigger) {
                // No trigger present at all - ensure button is hidden
                hideFloatingButton()
            }
            
            lastText = currentText
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking node", e)
        }
    }

    private fun showFloatingBubble(node: AccessibilityNodeInfo, text: String) {
        Log.d(TAG, "=== Showing Floating Bubble ===")
        
        try {
            // Check overlay permission
            if (!Settings.canDrawOverlays(this)) {
                Log.e(TAG, "ERROR: No overlay permission!")
                return
            }
            Log.d(TAG, "✓ Overlay permission OK")
            
            val triggerPattern = prefManager.getCustomTrigger()
            
            // Extract prompt after trigger
            val triggerIndex = text.indexOf(triggerPattern)
            if (triggerIndex == -1) return
            
            val afterTrigger = text.substring(triggerIndex + triggerPattern.length).trim()
            
            // Check if it's a command
            val isCommand = afterTrigger.startsWith("/")
            val command = if (isCommand) {
                val spaceIdx = afterTrigger.indexOf(" ")
                if (spaceIdx > 0) afterTrigger.substring(0, spaceIdx) else afterTrigger
            } else {
                ""
            }
            
            Log.d(TAG, "After trigger: '$afterTrigger', Is command: $isCommand, Command: '$command'")
            
            // Special handling for /voice command
            if (command == "/voice") {
                Log.d(TAG, "Voice command detected - launching voice input")
                val intent = Intent(this, FloatingOverlayService::class.java).apply {
                    action = FloatingOverlayService.ACTION_VOICE_INPUT
                }
                startService(intent)
                return
            }
            
            // Show floating button
            val intent = Intent(this, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_SHOW
                putExtra("full_text", text)
                putExtra("prompt", afterTrigger)
                putExtra("context", prefManager.getCurrentContext())
                putExtra("is_command", isCommand)
                putExtra("command", command)
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(TAG, "Starting foreground service (Android O+)")
                    startForegroundService(intent)
                } else {
                    Log.d(TAG, "Starting service (pre-Android O)")
                    startService(intent)
                }
                Log.d(TAG, "✓ Service start command sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "ERROR starting service: ${e.message}", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing button", e)
        }
    }

    private fun hideFloatingButton() {
        try {
            val intent = Intent(this, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_HIDE
            }
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding button", e)
        }
    }

    private fun isGmailCompose(): Boolean {
        return currentPackageName.contains("gmail", ignoreCase = true)
    }

    fun replaceTextInCurrentField(newText: String) {
        try {
            Log.d(TAG, "replaceTextInCurrentField called with text length: ${newText.length}")
            Log.d(TAG, "First 100 chars: ${newText.take(100)}")
            Log.d(TAG, "Last 50 chars: ${newText.takeLast(50)}")
            
            // Check if this is Gmail compose window
            if (isGmailCompose()) {
                replaceTextInGmail(newText)
            } else {
                // Standard replacement for other apps
                currentEditText?.let { node ->
                    setTextInNode(node, newText)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing text", e)
        }
    }
    
    /**
     * Replace only the @gemini /voice trigger with AI output, preserving other text
     */
    fun replaceVoiceTrigger(aiOutput: String) {
        try {
            val editText = currentEditText ?: run {
                Log.e(TAG, "replaceVoiceTrigger - No current edit text!")
                return
            }
            
            if (!editText.isEditable) {
                Log.e(TAG, "replaceVoiceTrigger - Field not editable!")
                return
            }
            
            val currentText = editText.text?.toString() ?: ""
            val triggerPattern = prefManager.getCustomTrigger()
            
            Log.d(TAG, "replaceVoiceTrigger() - Current text: '$currentText'")
            Log.d(TAG, "replaceVoiceTrigger() - Text bytes: ${currentText.toByteArray().joinToString(",") { it.toString() }}")
            
            if (currentText.contains(triggerPattern)) {
                val triggerIndex = currentText.indexOf(triggerPattern)
                
                // Find where trigger ends
                val triggerEnd = triggerIndex + triggerPattern.length
                
                // Look for /voice starting from after the trigger
                val remainingText = currentText.substring(triggerEnd)
                val voiceIndex = remainingText.indexOf("/voice")
                
                val commandEnd = if (voiceIndex >= 0) {
                    // Found /voice, skip past it and any trailing whitespace
                    triggerEnd + voiceIndex + "/voice".length
                } else {
                    triggerEnd
                }
                
                // Build final text: everything before trigger + AI output only
                val beforeTrigger = currentText.substring(0, triggerIndex)
                val newText = beforeTrigger + aiOutput
                
                Log.d(TAG, "Before: '$beforeTrigger'")
                Log.d(TAG, "Output: '$aiOutput'")
                Log.d(TAG, "Final: '$newText'")
                setTextInNode(editText, newText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing voice trigger", e)
        }
    }

    private fun setTextInNode(node: AccessibilityNodeInfo, text: String) {
        try {
            Log.d(TAG, "setTextInNode: Setting ${text.length} characters")
            Log.d(TAG, "First 100 chars: ${text.take(100)}")
            
            // Try ACTION_SET_TEXT first
            val pasteArgs = android.os.Bundle()
            pasteArgs.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, pasteArgs)
            Log.d(TAG, "ACTION_SET_TEXT result: $success")
            
            if (!success) {
                // Fallback: Try focus first, then set text
                Log.w(TAG, "ACTION_SET_TEXT failed, trying with focus first...")
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, pasteArgs)
                }, 50)
            }
            
            // Set cursor to end of text
            val textLength = text.length
            val cursorArgs = android.os.Bundle()
            cursorArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, textLength)
            cursorArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, textLength)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursorArgs)
            
            // Focus the field to show keyboard
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            
            Log.d(TAG, "✓ Text replacement attempted, cursor at position $textLength")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting text in node", e)
        }
    }

    private fun replaceTextInGmail(generatedText: String) {
        try {
            Log.d(TAG, "=== Gmail compose detected ===")
            Log.d(TAG, "Generated text: '${generatedText.take(100)}...'")
            
            // Parse the generated text to extract subject and body
            val (subject, body) = parseEmailContent(generatedText)
            Log.d(TAG, "Parsed subject: '$subject'")
            Log.d(TAG, "Parsed body: '${body.take(100)}...'")
            
            // Get the root window
            val root = rootInActiveWindow
            if (root == null) {
                Log.e(TAG, "rootInActiveWindow is null!")
                currentEditText?.let { setTextInNode(it, generatedText) }
                return
            }
            
            // Find ALL editable text fields in Gmail compose window
            val allFields = mutableListOf<AccessibilityNodeInfo>()
            findAllEditableNodes(root, allFields)
            
            Log.d(TAG, "=== Found ${allFields.size} editable fields ===")
            
            var subjectField: AccessibilityNodeInfo? = null
            var bodyField: AccessibilityNodeInfo? = null
            
            // Analyze each field with detailed logging
            allFields.forEachIndexed { index, field ->
                val hint = field.hintText?.toString() ?: ""
                val desc = field.contentDescription?.toString() ?: ""
                val id = field.viewIdResourceName ?: ""
                val className = field.className?.toString() ?: ""
                val text = field.text?.toString() ?: ""
                
                Log.d(TAG, """
                    |Field #$index:
                    |  Hint: "$hint"
                    |  Desc: "$desc"
                    |  ID: "$id"
                    |  Class: "$className"
                    |  Text: "${text.take(30)}"
                """.trimMargin())
                
                // Identify subject field - check multiple indicators
                val isSubject = hint.contains("subject", ignoreCase = true) ||
                               desc.contains("subject", ignoreCase = true) ||
                               id.contains("subject", ignoreCase = true) ||
                               id.contains("subject_text", ignoreCase = true)
                
                // Identify body/compose field
                val isBody = hint.contains("compose", ignoreCase = true) ||
                            desc.contains("compose", ignoreCase = true) ||
                            id.contains("body", ignoreCase = true) ||
                            id.contains("composearea", ignoreCase = true) ||
                            className.contains("ComposeEditText", ignoreCase = true) ||
                            id.endsWith("body") ||
                            // Body field is usually the largest editable field
                            (text.length > 50 && !isSubject)
                
                when {
                    isSubject -> {
                        subjectField = field
                        Log.d(TAG, "  ✓✓✓ IDENTIFIED AS SUBJECT")
                    }
                    isBody -> {
                        bodyField = field
                        Log.d(TAG, "  ✓✓✓ IDENTIFIED AS BODY")
                    }
                    field == currentEditText -> {
                        Log.d(TAG, "  → Current field (where @gemini was typed)")
                        // If current field isn't identified as subject, assume it's body
                        if (subjectField != field && bodyField == null) {
                            bodyField = field
                        }
                    }
                }
            }
            
            Log.d(TAG, "=== Field Identification Results ===")
            Log.d(TAG, "Subject field: ${if (subjectField != null) "FOUND" else "NOT FOUND"}")
            Log.d(TAG, "Body field: ${if (bodyField != null) "FOUND" else "NOT FOUND"}")
            
            // Strategy 1: Fill subject field if found and subject exists
            val finalSubjectField = subjectField
            if (subject.isNotEmpty() && finalSubjectField != null) {
                Log.d(TAG, "📧 Filling SUBJECT: '$subject'")
                fillTextField(finalSubjectField, subject)
            } else if (subject.isNotEmpty()) {
                Log.w(TAG, "⚠️ Have subject but no subject field found!")
            }
            
            // Strategy 2: Fill body field
            val finalBodyField = bodyField
            if (body.isNotEmpty()) {
                val targetBodyField = finalBodyField ?: currentEditText
                if (targetBodyField != null) {
                    Log.d(TAG, "📝 Filling BODY: '${body.take(50)}...'")
                    fillTextField(targetBodyField, body)
                } else {
                    Log.w(TAG, "⚠️ No body field found!")
                }
            }
            
            // Fallback: If nothing worked, just put everything in current field
            if (subjectField == null && bodyField == null) {
                Log.w(TAG, "⚠️ Field detection failed - using fallback")
                currentEditText?.let { 
                    fillTextField(it, generatedText)
                }
            }
            
            // Cleanup
            allFields.forEach { it.recycle() }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in replaceTextInGmail", e)
            e.printStackTrace()
            currentEditText?.let { setTextInNode(it, generatedText) }
        }
    }
    
    private fun fillTextField(field: AccessibilityNodeInfo, text: String) {
        try {
            // Method 1: Try direct text setting
            val args = android.os.Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val success = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            
            if (success) {
                Log.d(TAG, "✓ Text set successfully via ACTION_SET_TEXT")
            } else {
                // Method 2: Try focus + paste approach
                Log.d(TAG, "Trying alternative method: focus + set text")
                field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                Thread.sleep(100)
                field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error filling text field", e)
        }
    }
    
    private fun findAllEditableNodes(node: AccessibilityNodeInfo?, result: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        
        try {
            if (node.isEditable && node.className?.contains("EditText") == true) {
                result.add(node)
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    findAllEditableNodes(child, result)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error traversing nodes", e)
        }
    }

    private fun parseEmailContent(text: String): Pair<String, String> {
        // Look for common patterns like "Subject: ..." or split by newlines
        val lines = text.lines()
        var subject = ""
        var body = ""
        
        if (text.contains("Subject:", ignoreCase = true)) {
            // Pattern: "Subject: xyz\nBody: abc" or "Subject: xyz\n\nabc"
            val parts = text.split("\n", limit = 3)
            for (i in parts.indices) {
                val line = parts[i].trim()
                when {
                    line.startsWith("Subject:", ignoreCase = true) -> {
                        subject = line.substringAfter(":").trim()
                    }
                    line.startsWith("Body:", ignoreCase = true) -> {
                        body = line.substringAfter(":").trim()
                        // Add remaining lines
                        if (i + 1 < parts.size) {
                            body += "\n" + parts.subList(i + 1, parts.size).joinToString("\n")
                        }
                        break
                    }
                    line.isNotEmpty() && subject.isNotEmpty() && body.isEmpty() -> {
                        // First non-empty line after subject becomes body
                        body = parts.subList(i, parts.size).joinToString("\n").trim()
                        break
                    }
                }
            }
        } else if (lines.size >= 2) {
            // First line is subject, rest is body
            subject = lines[0].trim()
            body = lines.drop(1).joinToString("\n").trim()
        } else {
            // Single line - use as body
            body = text.trim()
        }
        
        Log.d(TAG, "Parsed - Subject: '$subject', Body: '${body.take(50)}...'")
        return Pair(subject, body)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        hideFloatingButton()
        Log.d(TAG, "Service destroyed")
    }
}
