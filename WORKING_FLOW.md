# Gemini Anywhere - Working Flow Documentation

## Overview
This document describes the complete working flow of the Gemini Anywhere application, an Android accessibility service that integrates AI assistance into any text field.

---

## 1. APPLICATION STARTUP FLOW

### 1.1 App Launch
```
User Opens App
    ↓
MainActivity.onCreate()
    ↓
Initialize PreferenceManager
    ↓
Setup UI Components
    ↓
Check Permissions Status
    ↓
Display Welcome Dialog (First Launch)
```

### 1.2 Initial Configuration
```
MainActivity
    ├─→ Set API Key (via Dialog)
    │   └─→ Save to PreferenceManager
    │
    ├─→ Select AI Model (Spinner)
    │   └─→ Save to PreferenceManager
    │
    ├─→ Enable Accessibility Service
    │   └─→ Opens Android Settings
    │       └─→ User enables "Gemini Anywhere"
    │
    └─→ Grant Overlay Permission
        └─→ Opens Android Settings
            └─→ User grants "Display over other apps"
```

---

## 2. ACCESSIBILITY SERVICE FLOW

### 2.1 Service Lifecycle
```
User Enables Accessibility Service
    ↓
GeminiAccessibilityService.onCreate()
    ↓
Initialize PreferenceManager
    ↓
Set instance reference
    ↓
Start Monitoring System Events
```

### 2.2 Text Field Monitoring
```
User Types in ANY App
    ↓
Android System Fires AccessibilityEvent
    ↓
onAccessibilityEvent() Triggered
    ↓
Event Type Check:
    ├─→ TYPE_VIEW_TEXT_CHANGED
    ├─→ TYPE_WINDOW_CONTENT_CHANGED
    └─→ TYPE_VIEW_FOCUSED
    ↓
Get Source Node from Event
    ↓
Check if Node is Editable
    ↓
Call checkNodeForTrigger()
```

### 2.3 Trigger Detection
```
checkNodeForTrigger(node)
    ↓
Check if Trigger is Enabled
    ↓
Get Current Text from Node
    ↓
Extract Trigger Pattern (default: "@gemini")
    ↓
Does Text Contain Trigger?
    ├─→ NO: Hide Floating Button
    │         └─→ Exit
    │
    └─→ YES: Continue Processing
        ↓
    Extract Text After Trigger
        ↓
    Check for Command Pattern (starts with "/")
        ├─→ NO COMMAND: Regular prompt
        │   └─→ Save prompt
        │       └─→ Show Floating Button
        │
        └─→ HAS COMMAND: Process command
            ↓
        Extract Command Name
            ├─→ /voice: Voice Input
            │   └─→ Show Voice Interface
            │
            ├─→ /fix: Fix Text
            ├─→ /explain: Explain Code
            ├─→ /translate: Translate
            └─→ Other Custom Commands
                ↓
            Get Command Template from PreferenceManager
                ↓
            Replace {text} with User Content
                ↓
            Save Final Prompt
                ↓
            Show Floating Button
```

---

## 3. FLOATING BUTTON FLOW

### 3.1 Button Display
```
Accessibility Service Detects Trigger
    ↓
Send Intent to FloatingOverlayService
    ↓
FloatingOverlayService.onStartCommand()
    ↓
Parse Intent Action:
    ├─→ ACTION_SHOW: Show Button
    ├─→ ACTION_HIDE: Hide Button
    └─→ ACTION_VOICE_INPUT: Show Voice UI
    ↓
Start as Foreground Service
    ↓
Create/Update Floating View
    ↓
Position Near Text Field
    ↓
Attach to WindowManager
    ↓
Display Gemini Button Overlay
```

### 3.2 Button Positioning
```
Get EditText Position
    ↓
Calculate Screen Coordinates
    ↓
Determine Available Space:
    ├─→ Below Text Field (Preferred)
    ├─→ Above Text Field (Fallback)
    └─→ Center Right (Last Resort)
    ↓
Update WindowManager Layout Params
    ↓
Move Button to Position
```

---

## 4. AI GENERATION FLOW

### 4.1 User Triggers Generation
```
User Taps Floating Button
    ↓
FloatingOverlayService.handleButtonClick()
    ↓
Show Loading State (Pulsing Animation)
    ↓
Get Current Prompt from AccessibilityService
    ↓
Get App Context (messaging/email/social)
    ↓
Launch Coroutine for API Call
```

### 4.2 API Request Flow
```
Start Coroutine (Background Thread)
    ↓
GeminiApiClient.generateResponse()
    ↓
Build Contextual Prompt:
    ├─→ Add Context-Specific Instructions
    │   ├─→ Messaging Apps: Casual tone
    │   ├─→ Email Apps: Professional tone
    │   └─→ Social Media: Engaging tone
    │
    └─→ Combine with User Prompt
    ↓
Create GeminiRequest Object
    ├─→ contents: [prompt]
    ├─→ generationConfig:
    │   ├─→ temperature: 0.7
    │   ├─→ maxOutputTokens: 300
    │   └─→ topK, topP values
    │
    └─→ Send HTTP POST Request
        ↓
    Retry Logic (up to 2 retries):
        ├─→ Attempt 1: Immediate
        ├─→ Attempt 2: Delay 1s
        └─→ Attempt 3: Delay 2s
```

### 4.3 Response Handling
```
Receive API Response
    ↓
Parse GeminiResponse
    ↓
Extract Text from Candidates
    ↓
Success?
    ├─→ YES: Return Generated Text
    │   └─→ Switch to Main Thread
    │       └─→ Insert Text into Field
    │
    └─→ NO: Error Occurred
        ├─→ Network Error
        ├─→ Rate Limit (429)
        ├─→ Model Busy (503)
        └─→ Invalid API Key
            ↓
        Show Error Toast
            ↓
        Hide Floating Button
```

---

## 5. TEXT INSERTION FLOW

### 5.1 Replace Trigger with AI Response
```
AI Response Received
    ↓
Get Current EditText Node
    ↓
Get Full Text Content
    ↓
Find Trigger Position in Text
    ↓
Calculate Replacement Range:
    ├─→ Start: Trigger position
    └─→ End: End of user prompt
    ↓
Create Replacement Text:
    ├─→ Text Before Trigger
    ├─→ + AI Generated Response
    └─→ + Text After Prompt
    ↓
Set New Text via Accessibility API
    ↓
Move Cursor to End
    ↓
Hide Floating Button
    ↓
Show Success Animation
```

---

## 6. VOICE INPUT FLOW

### 6.1 Voice Activation
```
User Types "@gemini /voice"
    ↓
Accessibility Service Detects Command
    ↓
Send ACTION_VOICE_INPUT to FloatingOverlayService
    ↓
Show Voice Input UI
    ├─→ Microphone Button
    ├─→ Status Text
    ├─→ Transcription Display
    └─→ Action Buttons
```

### 6.2 Recording Process
```
User Taps Microphone Button
    ↓
VoiceInputHandler.startRecording()
    ↓
Request RECORD_AUDIO Permission (if needed)
    ↓
Initialize Android SpeechRecognizer
    ↓
Start Listening
    ↓
Update UI:
    ├─→ Pulsing Microphone Animation
    └─→ "Listening..." Status
    ↓
Receive Partial Results (real-time)
    ↓
Update Transcription Text
```

### 6.3 Voice Processing
```
User Stops Speaking
    ↓
SpeechRecognizer.onResults()
    ↓
Get Final Transcription
    ↓
Display in Editable Field
    ↓
User Options:
    ├─→ Send: Use transcription as prompt
    │   └─→ Trigger AI Generation
    │
    ├─→ Replay: Record again
    │   └─→ Clear & Restart Recording
    │
    └─→ Cancel: Discard
        └─→ Hide Voice UI
```

---

## 7. SETTINGS FLOW

### 7.1 Settings Activity
```
User Taps Settings Button
    ↓
Open SettingsActivity
    ↓
Display Settings Options:
    ├─→ Trigger Settings
    │   ├─→ Enable/Disable Trigger
    │   └─→ Custom Trigger Text
    │
    ├─→ AI Model Selection
    │   └─→ 14+ Model Options
    │
    ├─→ Retry Configuration
    │   └─→ Max Retry Attempts
    │
    ├─→ Context Awareness
    │   └─→ Enable/Disable
    │
    └─→ Custom Commands
        └─→ Open CommandsActivity
```

### 7.2 Commands Management
```
User Opens CommandsActivity
    ↓
Display Command List:
    ├─→ /fix: Fix grammar/spelling
    ├─→ /explain: Explain code/concept
    ├─→ /translate: Translate text
    ├─→ /summarize: Summarize content
    └─→ Custom Commands
    ↓
User Actions:
    ├─→ Add New Command
    │   ├─→ Command Name (e.g., /shorten)
    │   ├─→ Prompt Template
    │   └─→ Save to PreferenceManager
    │
    ├─→ Edit Command
    │   └─→ Modify Template
    │
    └─→ Delete Command
        └─→ Remove from Preferences
```

---

## 8. PERMISSION FLOW

### 8.1 Accessibility Permission
```
User Needs Accessibility Service
    ↓
Tap "Enable Accessibility Service"
    ↓
Open System Settings
    ↓
Navigate to: Settings → Accessibility → Downloaded Services
    ↓
Find "Gemini Anywhere"
    ↓
Toggle ON
    ↓
Confirm Permission Dialog
    ↓
Service Starts Automatically
```

### 8.2 Overlay Permission
```
User Needs Overlay Permission
    ↓
Tap "Grant Overlay Permission"
    ↓
Open System Settings
    ↓
Navigate to: Settings → Apps → Special Access → Display over other apps
    ↓
Find "Gemini Anywhere"
    ↓
Toggle ON
    ↓
Return to App
    ↓
Permission Granted
```

### 8.3 Audio Permission (Voice)
```
User Triggers Voice Input (First Time)
    ↓
Check RECORD_AUDIO Permission
    ↓
Permission Denied?
    ├─→ YES: Request Permission
    │   ↓
    │   Show System Dialog
    │   ↓
    │   User Choice:
    │   ├─→ Allow: Continue
    │   └─→ Deny: Show Error
    │
    └─→ NO: Continue with Recording
```

---

## 9. CONTEXT AWARENESS FLOW

### 9.1 App Detection
```
Text Field Changed
    ↓
Get Current Package Name
    ↓
Identify App Category:
    ├─→ Messaging Apps:
    │   ├─→ WhatsApp
    │   ├─→ Telegram
    │   ├─→ SMS
    │   └─→ Context: "casual"
    │
    ├─→ Email Apps:
    │   ├─→ Gmail
    │   ├─→ Outlook
    │   └─→ Context: "professional"
    │
    ├─→ Social Media:
    │   ├─→ Twitter/X
    │   ├─→ Facebook
    │   ├─→ LinkedIn
    │   └─→ Context: "social"
    │
    └─→ Other Apps:
        └─→ Context: "general"
```

### 9.2 Context Application
```
Generate API Prompt
    ↓
Apply Context Rules:
    ├─→ Casual: Short, friendly, emojis
    ├─→ Professional: Formal, structured
    ├─→ Social: Engaging, hashtags
    └─→ General: Balanced tone
    ↓
Prepend Context Instructions to Prompt
    ↓
Send to Gemini API
```

---

## 10. ERROR HANDLING FLOW

### 10.1 API Errors
```
API Call Failed
    ↓
Identify Error Type:
    ├─→ Network Error:
    │   └─→ "Check internet connection"
    │
    ├─→ Rate Limit (429):
    │   ├─→ Wait & Retry
    │   └─→ Show "Model busy, retrying..."
    │
    ├─→ Server Error (503):
    │   ├─→ Retry with Delay
    │   └─→ Show "Service temporarily unavailable"
    │
    ├─→ Invalid API Key (400/401):
    │   └─→ "Please check your API key"
    │
    └─→ Timeout:
        └─→ "Request timed out, please try again"
```

### 10.2 Permission Errors
```
Permission Check Failed
    ↓
Identify Missing Permission:
    ├─→ Accessibility Service:
    │   └─→ Show Banner: "Enable Accessibility"
    │
    ├─→ Overlay Permission:
    │   └─→ Show Banner: "Grant Overlay Permission"
    │
    └─→ Audio Permission:
        └─→ Request at Runtime
```

---

## 11. DATA FLOW SUMMARY

### 11.1 User Input → AI Response
```
User Types "@gemini Hello"
    ↓
[Accessibility Service]
    → Detects trigger
    → Extracts prompt: "Hello"
    → Gets app context
    ↓
[Floating Overlay Service]
    → Shows button
    → Waits for tap
    ↓
User Taps Button
    ↓
[API Client]
    → Builds contextual prompt
    → Calls Gemini API
    → Retries on failure
    ↓
[Response Handler]
    → Receives AI response
    → Validates content
    ↓
[Text Inserter]
    → Replaces "@gemini Hello"
    → Inserts AI response
    → Positions cursor
    ↓
Done!
```

### 11.2 Component Communication
```
MainActivity
    ↓ (Saves Settings)
PreferenceManager
    ↑ (Reads Settings)
    ├─→ GeminiAccessibilityService
    ├─→ FloatingOverlayService
    └─→ GeminiApiClient
    
GeminiAccessibilityService
    ↓ (Sends Intents)
FloatingOverlayService
    ↓ (Makes API Calls)
GeminiApiClient
    ↓ (HTTP Requests)
Gemini API (Google)
```

---

## 12. STATE MANAGEMENT

### 12.1 Service States
```
GeminiAccessibilityService States:
    ├─→ IDLE: Monitoring events
    ├─→ TRIGGER_DETECTED: Button shown
    └─→ TEXT_INSERTED: Processing complete

FloatingOverlayService States:
    ├─→ HIDDEN: No overlay visible
    ├─→ VISIBLE: Button displayed
    ├─→ LOADING: API call in progress
    ├─→ VOICE_INPUT: Recording voice
    └─→ ERROR: Error state

VoiceInputHandler States:
    ├─→ IDLE: Not recording
    ├─→ LISTENING: Active recording
    ├─→ PROCESSING: Finalizing transcription
    └─→ COMPLETE: Transcription ready
```

---

## 13. LIFECYCLE EVENTS

### 13.1 App Lifecycle
```
App Launch
    → onCreate()
    → onStart()
    → onResume()
    ↓
User Interaction
    ↓
App Background
    → onPause()
    → onStop()
    ↓
Services Continue Running (Accessibility, Overlay)
    ↓
App Return
    → onRestart()
    → onStart()
    → onResume()
```

### 13.2 Service Lifecycle
```
Service Start
    → onCreate()
    → onStartCommand()
    → Start Foreground (Notification)
    ↓
Service Running
    ↓
Service Stop
    → onDestroy()
    → Cleanup Resources
```

---

## 14. KEY INTERACTIONS

### 14.1 User → System
- User types in any app
- Android sends accessibility events
- App monitors these events

### 14.2 App → Android System
- Request accessibility events
- Display overlay windows
- Insert text via accessibility API

### 14.3 App → External API
- HTTP requests to Gemini API
- Receive JSON responses
- Parse and display results

### 14.4 Component → Component
- Activities → Services (via Intents)
- Services → Shared Preferences
- Services → UI Updates

---

## 15. PERFORMANCE OPTIMIZATIONS

### 15.1 Event Filtering
- Only process relevant events (text changes, focus)
- Ignore non-editable fields
- Debounce rapid events

### 15.2 API Efficiency
- Limit token output (300 max)
- Timeout after 15 seconds
- Retry logic with exponential backoff

### 15.3 Resource Management
- Foreground service for reliability
- Proper view recycling
- Coroutine cancellation on service stop

---

## FLOWCHART RECOMMENDATIONS

For creating visual flowcharts, consider these main flows:

1. **Main User Flow**: User types → Detection → Button appears → Tap → AI response
2. **Service Initialization**: App launch → Permissions → Service start
3. **API Call Flow**: Request → Retry logic → Response → Text insertion
4. **Voice Input Flow**: Voice trigger → Recording → Transcription → Send
5. **Error Handling**: Error detection → Categorization → User feedback → Recovery

Use tools like:
- **Mermaid.js** (for markdown-based diagrams)
- **Draw.io / Diagrams.net** (web-based)
- **Lucidchart** (professional)
- **PlantUML** (code-based)

---

## NOTES

- All services run in background for system-wide integration
- Accessibility Service is the core monitoring component
- Floating Overlay displays the AI button
- PreferenceManager handles all persistent data
- Context awareness adapts responses to app type
- Voice input provides hands-free operation
- Custom commands allow user personalization
