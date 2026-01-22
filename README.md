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
- 🎨 **Professional UI** - Modern Material Design 3 interface with smooth animations
- 🔒 **Privacy First** - On-device speech recognition, no audio uploads
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

1. Type `@gemini /voice` in any text field
2. Tap the microphone button when it appears
3. Speak your command
4. Edit the transcription if needed
5. Tap "Send" to process with AI

### Custom Commands

Create shortcuts for frequent tasks:

- `/email` - Format as professional email
- `/reply` - Generate contextual reply
- `/summary` - Summarize long text
- `/translate` - Translate to another language

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
- API key stored in encrypted SharedPreferences

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

### 1. Floating Button UI
- Draggable circular button
- Auto-hides when not needed
- Material ripple effects
- Smooth fade animations

### 2. Voice Recording Interface
- **Tap to Record** - Start/stop recording
- **Live Transcription** - See text as you speak
- **Edit Mode** - Modify transcription before sending
- **Action Buttons** - Cancel, Replay, or Send
- **Close Button** - Force quit anytime

### 3. Markdown Sanitization
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
