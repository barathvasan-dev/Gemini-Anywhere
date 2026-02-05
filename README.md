# 🚀 Gemini Anywhere

<div align="center">

**AI-Powered Inline Assistant for Android**

*Write, Edit, and Transform Text Anywhere with Voice & AI*

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📱 Overview

**Gemini Anywhere** is a powerful Android accessibility service that brings Google's Gemini AI directly into any text field across all your apps. No more switching between apps or copying text—just type `@gemini` and let AI handle the rest.

### ✨ Key Features

- 🎯 **Universal AI Access** - Works in every app: WhatsApp, Gmail, Twitter, Slack, and more
- 🎤 **Voice Input** - Speak your commands with `@gemini /voice` for hands-free AI assistance
- ⚡ **Context-Aware Responses** - Automatically formats output for emails, social posts, or messages
- 🚀 **Response Caching** - Instant responses for repeated prompts (99% faster)
- 🔋 **Battery Optimized** - Smart event debouncing reduces battery drain by 79%
- 🧠 **Conversation Memory** - Multi-turn conversations with automatic context tracking (5-turn sliding window)
- 📜 **Command History** - Track and reuse your last 50 commands with smart search
- ⭐ **Favorite Prompts** - Save frequently used prompts with categories and tags
- 🌐 **Multi-Language Support** - Available in English, Spanish, French, German, and Japanese
- 🎨 **Professional UI** - Modern Material Design 3 interface with smooth animations
- 🔒 **Privacy First** - On-device speech recognition, no audio uploads
- 🔐 **Secure Storage** - API keys encrypted with AES-256-GCM via Android Keystore
- ⚙️ **Customizable** - Create custom slash commands for frequent tasks
- 🌐 **Model Selection** - Choose between Gemini Flash, Pro, and experimental models
- 📝 **Markdown-Free Output** - Clean text output ready to send

---

## 🎥 Demo

> *Add screenshots or GIF demonstrations here*

---

## 🚀 Getting Started

### Prerequisites

- Android 7.0 (API 24) or higher
- Google Gemini API Key ([Get one here](https://makersuite.google.com/app/apikey))
- Accessibility service permissions

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/barathvasan-dev/Gemini-Anywhere.git
   cd Gemini-Anywhere
   ```

2. **Open in Android Studio**
   - Use Android Studio Hedgehog or newer
   - Sync Gradle files

3. **Build and Install**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Configure the App**
   - Launch Gemini Anywhere
   - Enter your Gemini API key
   - Enable the Accessibility Service in Android Settings
   - Grant overlay and microphone permissions

---

## 💡 Usage

### Basic Text Generation

1. Open any app with a text field (WhatsApp, Gmail, Notes, etc.)
2. Type `@gemini` followed by your prompt
3. Tap the floating AI button
4. Watch as AI generates and inserts the response

**Example:**
```
@gemini write a professional email declining a meeting
```

### Voice Input

1. Type `@gemini voice` in any text field (no `/` needed!)
2. Tap the microphone button when it appears
3. Speak your command
4. Edit the transcription if needed
5. Use "Record More" button to append additional voice input
6. Tap "Send" to process with AI

**Voice Features:**
- 🎯 Professional Material Design 3 interface
- 🌊 Live waveform visualization during recording
- 💫 Animated pulse ring around microphone
- ✏️ Edit transcription before sending
- 🎤 Record More - append additional recordings
- 🔇 Noise reduction for better accuracy
- ⚡ Fast response (1000ms timeout)

### Conversation Memory

Have multi-turn conversations with context:

```
You: @gemini What is Paris?
AI: Paris is the capital of France...

You: @gemini What's the population?  ← Uses previous context!
AI: Paris has about 2.2 million people...

You: @gemini Tell me about the Eiffel Tower
AI: The Eiffel Tower in Paris is...
```

- Automatically tracks last 5 conversation turns
- 5-minute timeout (resets context naturally)
- No manual context management needed

### Command History

Access your previous commands:

1. Open the app → History
2. Search through past commands
3. Reuse or add to favorites
4. View usage statistics

**Features:**
- Last 50 commands saved
- Smart deduplication (1-hour window)
- Search by text or context
- Delete individual or clear all

### Favorite Prompts

Save frequently used prompts:

1. Open the app → Favorites
2. Tap + to create new favorite
3. Add title, prompt, category, and tags
4. Quick-use from any text field

**Example Favorites:**
- "Professional Thank You" → `write a professional thank you email`
- "Meeting Summary" → `summarize this meeting in bullet points`
- "Social Post" → `write engaging LinkedIn post about [topic]`

### Custom Commands

Create shortcuts for frequent tasks (no `/` prefix needed!):

- `email` - Format as professional email
- `reply` - Generate contextual reply
- `summary` - Summarize long text
- `translate` - Translate to another language
- `voice` - Launch voice input mode

**Usage Example:**
```
@gemini email thank the client for their patience
@gemini summary [paste long text]
@gemini voice [speak your command]
```

**Configure commands in:** Settings → Custom Commands

---

## 🏗️ Architecture

### Technology Stack

- **Language:** Kotlin 100%
- **UI Framework:** Material Design 3
- **Architecture:** Service-based with Coroutines
- **Networking:** Retrofit + OkHttp
- **Speech:** Android SpeechRecognizer API
- **Permissions:** Accessibility Service + Overlay

### Key Components

```
app/
├── api/
│   └── GeminiApiClient.kt         # API integration with timeouts & retries
├── service/
│   ├── GeminiAccessibilityService.kt  # Text field monitoring
│   ├── FloatingOverlayService.kt      # UI overlay management
│   └── VoiceInputHandler.kt           # Voice recognition
├── ui/
│   ├── MainActivity.kt                # API key setup
│   ├── SettingsActivity.kt            # App configuration
│   └── CommandsActivity.kt            # Custom commands
└── utils/
    └── PreferenceManager.kt           # Settings storage
```

### Core Features Implementation

#### 🎯 Context-Aware Formatting
Automatically detects app context and formats output appropriately:
- **WhatsApp:** Short, casual (2-3 sentences)
- **Email:** Subject line + formal body
- **LinkedIn:** Hook + Body + CTA structure

#### ⚡ Performance Optimizations
- 8s connection timeout
- 15s read timeout
- 2 retry attempts with 500ms delay
- 300 token limit for faster responses
- Gemini 2.0 Flash model (latest & fastest)

#### 🔒 Privacy & Security
- On-device speech-to-text (no audio upload)
- Local prompt building
- Markdown sanitization (prevents injection)
- **AES-256-GCM encryption** for API key storage via Android Keystore
- Hardware-backed security on supported devices
- Automatic migration from plain-text storage
- No data collection or analytics
- HTTPS-only communication

**See [SECURITY.md](SECURITY.md) for detailed security implementation**

---

## ⚙️ Configuration

### API Settings

Navigate to **Settings** in the app:

| Setting | Options | Description |
|---------|---------|-------------|
| **API Key** | Your key | Get from [Google AI Studio](https://makersuite.google.com/app/apikey) |
| **Model** | Flash / Pro / Experimental | Select AI model (Flash recommended) |
| **Max Retries** | 1-5 | Number of retry attempts on failure |

### Permissions Required

- ✅ **Accessibility Service** - Monitor text fields
- ✅ **Draw Over Other Apps** - Show floating button
- ✅ **Record Audio** - Voice input feature
- ✅ **Internet** - API communication

---

## 🎨 Features in Detail

### 1. Conversation Context Memory 🧠
- **Sliding Window:** Tracks last 5 conversation turns automatically
- **Smart Expiry:** 5-minute timeout prevents stale context
- **Memory Efficient:** Max 2000 characters, no token overload
- **Seamless:** No manual context management needed
- **Perfect for:** Multi-turn conversations, follow-up questions, iterative refinement

### 2. Command History 📜
- **Capacity:** Last 50 commands with timestamps
- **Smart Search:** Find commands by text or app context
- **Deduplication:** Prevents duplicate entries (1-hour window)
- **Statistics:** Track total commands and daily usage
- **Quick Actions:** Reuse prompts or add to favorites instantly

### 3. Favorite Prompts ⭐
- **Unlimited Storage:** Save as many favorites as you need
- **Organization:** Categories, tags, and usage tracking
- **Quick Access:** Use favorites directly from any text field
- **Import/Export:** Share favorites as JSON
- **Usage Analytics:** See which prompts you use most

### 4. Multi-Language Support 🌐
- **Available Languages:**
  - 🇬🇧 English (Default)
  - 🇪🇸 Spanish (Español)
  - 🇫🇷 French (Français)
  - 🇩🇪 German (Deutsch)
  - 🇯🇵 Japanese (日本語)
- **Auto-Detection:** Automatically uses your device language
- **Complete Translation:** All UI elements, settings, and messages

### 5. Floating Button UI
- Draggable circular button
- Auto-hides when not needed
- Material ripple effects
- Smooth fade animations

### 6. Voice Recording Interface
- **Modern UI** - Material Design 3 with animations
- **Live Waveform** - 7-bar animated visualization during recording
- **Pulse Effect** - Animated ring around microphone
- **Tap to Record** - Start/stop recording with single tap
- **Live Transcription** - See text as you speak
- **Record More** - Append additional voice recordings
- **Edit Mode** - Modify transcription before sending
- **Action Buttons** - Cancel or Send with clean button design
- **Close Anytime** - X button for quick exit
- **Noise Reduction** - Optimized for accuracy

### 7. Markdown Sanitization
Removes all formatting for clean output:
- Bold (`**text**`)
- Italic (`*text*`)
- Code (`` `code` ``)
- Headings (`# Title`)
- Lists (`- item`)
- Blockquotes (`> quote`)

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Guidelines

- Follow Kotlin coding conventions
- Write meaningful commit messages
- Test on multiple Android versions
- Update documentation for new features

---

## 🐛 Troubleshooting

### Common Issues

**Q: AI button not appearing**
- Ensure Accessibility Service is enabled
- Grant "Draw over other apps" permission
- Restart the app

**Q: Voice input not working**
- Check microphone permission
- Ensure device has internet for Gemini API
- Test microphone in other apps

**Q: Slow response times**
- Check internet connection
- Try switching to Gemini Flash model
- Reduce prompt complexity

**Q: API errors**
- Verify API key is correct
- Check API quota at [Google AI Studio](https://makersuite.google.com)
- Ensure Gemini API is enabled

---

## 📜 Changelog

### Version 1.4.0 (2026-02-05)
- ✨ **NEW:** Command syntax simplified - removed "/" prefix (just type `command` instead of `/command`)
- 🎨 **UI:** Professional voice input interface with animated waveforms and pulse effects
- 🎤 **VOICE:** "Record More" feature - append additional voice input to existing recordings
- ⚡ **PERFORMANCE:** Noise reduction in voice recognition with 1000ms faster timeout
- 🔧 **FIX:** Custom trigger debounce (500ms delay) prevents auto-trigger while typing
- 🔧 **FIX:** Settings exclusion - trigger disabled in app's own settings
- 🎨 **UI:** Reduced spacing in voice input dialog for cleaner appearance
- 🧹 **CLEANUP:** Removed build artifacts and unnecessary files
- 📚 **DOCS:** Updated README with latest features and improvements

### Version 1.3.0 (2026-01-30)
- ✨ **NEW:** Conversation context memory with 5-turn sliding window
- ✨ **NEW:** Command history tracking (last 50 commands)
- ✨ **NEW:** Favorite prompts with categories and tags
- ✨ **NEW:** Multi-language support (Spanish, French, German, Japanese)
- 🎨 **UI:** New History and Favorites activities with Material Design 3
- 📱 **UX:** Smart search and filtering across history and favorites
- 🔧 **IMPROVEMENT:** Better memory management for context storage
- 📚 **DOCS:** Added FEATURES.md with comprehensive usage guide

### Version 1.2.0 (2026-01-25)
- ⚡ **PERFORMANCE:** Response caching with 99% speed improvement
- ⚡ **PERFORMANCE:** Event debouncing reduces battery usage by 79%
- ⚡ **PERFORMANCE:** 97% reduction in accessibility event processing
- 🔋 **OPTIMIZATION:** Battery-aware throttling based on device state
- 📚 **DOCS:** Added PERFORMANCE.md with benchmarks and metrics

### Version 1.1.0 (2026-01-23)
- 🔐 **SECURITY:** AES-256-GCM encryption for API keys
- 🔐 **SECURITY:** Android Keystore integration
- 🔐 **SECURITY:** Automatic migration from plain-text storage
- 🔐 **SECURITY:** Hardware-backed security on supported devices
- 📚 **DOCS:** Added SECURITY.md with detailed implementation guide

### Version 1.0.0 (2026-01-22)
- ✨ Initial release
- 🎤 Voice input with professional UI
- 🎯 Context-aware formatting (WhatsApp/Email/LinkedIn)
- ⚡ Performance optimizations (8s/15s timeouts)
- 🧹 Markdown sanitization
- 🎨 Material Design 3 UI
- 🔧 Custom slash commands
- 🚀 Gemini 2.0 Flash integration

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Barathvasan**
- GitHub: [@barathvasan-dev](https://github.com/barathvasan-dev)
- Project: [Gemini Anywhere](https://github.com/barathvasan-dev/Gemini-Anywhere)

---

## 🙏 Acknowledgments

- Google Gemini API for powering the AI features
- Material Design team for the beautiful components
- Android Accessibility team for the robust framework
- Retrofit & OkHttp for reliable networking

---

## ⭐ Show Your Support

If you find this project helpful, please give it a ⭐ on GitHub!

---

<div align="center">

**Made with ❤️ using Kotlin & Gemini AI**

[Report Bug](https://github.com/barathvasan-dev/Gemini-Anywhere/issues) · [Request Feature](https://github.com/barathvasan-dev/Gemini-Anywhere/issues) · [Documentation](https://github.com/barathvasan-dev/Gemini-Anywhere/wiki)

</div>
