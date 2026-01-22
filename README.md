# Gemini Anywhere

**AI assistance in any text field on Android**

Gemini Anywhere is a system-level accessibility app that brings Gemini AI directly into any text input across your Android device. Simply type `@gemini` followed by your request, and watch AI-powered responses appear instantly.

## ✨ Features

- 🎯 **Universal Integration**: Works in any app with text input (WhatsApp, Gmail, Twitter, etc.)
- 🤖 **14+ AI Models**: Access latest Gemini 3, 2.5, and 2.0 models including Pro, Flash, and specialized variants
- 🔄 **Auto-Retry Logic**: Automatically retries up to 3 times if model is busy or rate-limited
- 🎨 **Context-Aware**: Automatically adapts tone based on the app you're using
  - Messaging apps: Casual and friendly
  - Email apps: Professional and formal
  - Social media: Engaging and creative
- ⚡ **Real-time Generation**: Powered by latest Gemini models for lightning-fast responses
- 🔒 **Privacy-Focused**: Only activates when you type `@gemini`
- 🎨 **Modern UI**: Beautiful Material Design 3 interface with emoji indicators
- ⚙️ **In-App Configuration**: Set API key, select model, and configure retry settings

## 🚀 How It Works

1. Type `@gemini` in any text field
2. Add your request (e.g., `@gemini write a professional thank you email`)
3. A floating Gemini button appears
4. Tap the button to generate AI response
5. Text is automatically inserted in place!

## 📋 Setup Instructions

### Prerequisites
- Android 7.0 (API 24) or higher
- Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)

### Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/yourusername/gemini-anywhere.git
   cd gemini-anywhere
   ```

2. Open the project in Android Studio

3. Build and run on your device

### Configuration

1. **Set API Key**
   - Open the app
   - Tap "Set API Key"
   - Enter your Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)

2. **Select AI Model**
   - Choose from available Gemini models:
     - **Gemini 3 Pro Preview** ⭐ SOTA reasoning and multimodal understanding (New!)
     - **Gemini 3 Flash Preview** ⚡ Speed with frontier intelligence (New!)
     - **Gemini 2.5 Pro** 🧠 Advanced reasoning and coding excellence
     - **Gemini 2.5 Flash** 💫 Balanced with 1M token context (Recommended)
     - **Gemini 2.5 Flash-Lite** 🪶 Most cost-effective at scale
     - **Gemini Flash Latest** 🔄 Auto-updates to newest Flash model
     - **Gemini 2.0 Flash** ⚡ Great multimodal performance
     - **Plus specialized models** for images, TTS, and robotics

3. **Enable Accessibility Service**
   - Tap "Accessibility Service"
   - Find "Gemini Anywhere" in the list
   - Toggle it on
   - Confirm permissions

4. **Grant Overlay Permission**
   - Tap "Overlay Permission"
   - Enable "Allow display over other apps"

## 🏗️ Project Structure

```
app/
├── src/main/java/com/geminianywhere/app/
│   ├── api/
│   │   └── GeminiApiClient.kt          # Gemini API integration
│   ├── service/
│   │   ├── GeminiAccessibilityService.kt  # Monitors text fields
│   │   └── FloatingOverlayService.kt      # Floating UI overlay
│   ├── ui/
│   │   └── MainActivity.kt              # Main setup screen
│   └── utils/
│       └── PreferenceManager.kt         # Settings storage
├── res/
│   ├── layout/
│   │   ├── activity_main.xml            # Main screen layout
│   │   └── floating_button_layout.xml   # Floating button UI
│   └── values/
│       ├── colors.xml
│       ├── strings.xml
│       └── themes.xml
└── AndroidManifest.xml
```

## 🔧 Technical Details

### Core Components

- **Accessibility Service**: Monitors text input events across all apps
- **Floating Overlay**: Material FAB that appears near the cursor
- **Gemini API Client**: Retrofit-based client for Gemini 2.0 Flash API
- **Context Detection**: Identifies app type for appropriate response tone

### Permissions Required

- `INTERNET`: API communication
- `SYSTEM_ALERT_WINDOW`: Floating overlay display
- `BIND_ACCESSIBILITY_SERVICE`: Text field monitoring
- `FOREGROUND_SERVICE`: Background operation
- `POST_NOTIFICATIONS`: Status notifications

## 📱 Supported Apps

Works with any app that has editable text fields:
- ✅ Messaging: WhatsApp, Telegram, Messages, Messenger
- ✅ Email: Gmail, Outlook, Yahoo Mail
- ✅ Social: Twitter, Facebook, LinkedIn, Instagram
- ✅ Productivity: Slack, Teams, Notion, Google Docs
- ✅ And many more!

## 🎨 UI/UX Highlights

- Material Design 3 components
- Smooth animations and transitions
- Clear status indicators
- Intuitive setup flow
- Responsive floating button positioning
- Loading states during API calls

## 🔒 Privacy & Security

- No data collection or storage
- Only activates with explicit `@gemini` trigger
- API key stored locally and encrypted
- Text sent only when user taps the button
- Open source for transparency

## 🛠️ Built With

- **Kotlin**: Modern Android development
- **Material Design 3**: Beautiful UI components
- **Retrofit**: Type-safe HTTP client
- **Coroutines**: Asynchronous programming
- **Android Accessibility Services**: System-wide text monitoring
- **Gemini 2.0 Flash API**: Advanced AI language model

## 📝 Usage Examples

**Messaging:**
```
@gemini reply to this: "Hey, want to grab dinner?"
```

**Email:**
```
@gemini write a professional email declining a meeting
```

**Social Media:**
```
@gemini create an engaging post about AI innovation
```

**Translation:**
```
@gemini translate this to Spanish: "Hello, how are you?"
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Google Gemini API for powerful AI capabilities
- Material Design team for beautiful components
- Android Accessibility Framework for system integration

## 📧 Contact

For questions or support, please open an issue on GitHub.

---

**Made with ❤️ for seamless AI assistance everywhere**
