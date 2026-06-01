
# Project Orion - AI-driven Screen Reader for Android
### Open-Source Screen Reader using Gemini AI

Orion is a screen reader designed to make Android devices more accessible by using Gemini AI. It works as an accessibility service, letting users convert on-screen content into spoken words. With a floating button called **Read Screen**, Orion allows users to hear information on their screens with just a tap.

![Orion in Action](OrionGIF1.gif)

## Features
- **Accessibility Service Integration**: Works as an accessibility service to provide screen reading capabilities.
- **Floating Controls**: Use **Read Screen**, then pause, resume, or repeat the generated reading.
- **AI-Powered**: Captures a snapshot of the current screen, sends it to Gemini AI, and reads the result aloud.
- **Reading Modes**: Choose between a concise screen description and reading visible text aloud.
- **Voice Engines**: Use Android Text-to-Speech by default or opt into a more natural online Gemini TTS voice.
- **Custom Configuration**: Configure your language, reading speed, voice, haptic feedback, and private Gemini API key.
- **Diagnostics**: View, share, or clear recent diagnostic entries from Orion settings when troubleshooting.


## Addressing Accessibility

Orion is designed to help people with visual impairments or blindness access their smartphone screens more easily. Accessing information from a screen can be challenging for users with limited or no vision, and Orion aims to make this process simple. Here are some key ways Orion helps:

- **Screen Content Read-Aloud**: Orion reads out any text or relevant information on the screen, making it easy to understand content without seeing it.
- **Simple Floating Button**: The **Read Screen** button is easy to locate and use. Users just tap the button to access the content, making navigation more straightforward.
- **AI-Powered Context Extraction**: Orion, powered by [Gemini AI](https://gemini.google.com/), goes beyond reading text. It summarizes relevant information from the screen and skips unnecessary parts, helping users focus on what matters most.
- **Tactile Feedback**: Optional vibrations provide feedback for actions and reading progress, including a heartbeat while Orion is processing.
- **Playback Controls**: Large floating **Play/Pause** and **Repeat** controls appear after a reading is ready.
- **Customizable Settings**: Open Orion from the app drawer to adjust the language, reading mode, speed, voice engine, Gemini voice, and haptic feedback.

The main goal of this project is to support users who have difficulty accessing content on apps or websites that overlay content with popups or other interactive elements.

Orion bridges the accessibility gap, helping visually impaired individuals interact more easily with their Android devices. By combining advanced AI with a simple user interface, Orion gives users more independence in their daily digital activities.

## How It Works
1. **Accessibility Service**: After installing the app, Orion works as an accessibility service to get the needed permissions.
2. **Floating Button Activation**: The **Read Screen** button floats over your device screen.
3. **Screen Capture**: When the user taps the button, Orion takes a snapshot of the current screen.
4. **AI Processing**: The snapshot is sent to Gemini AI, which extracts meaningful information. The floating control shows **Thinking...** while Orion prepares the reading.
5. **TTS Output**: The processed information is read aloud using Android TTS or the optional Gemini TTS voice engine.
6. **Playback Controls**: Pause, resume, or repeat the generated reading from the floating controls.

## Getting Started

### Prerequisites
- **Android Device**: Orion works with Android versions that support accessibility services.
- **Internet Connection**: Orion needs a stable internet connection to process snapshots using Gemini AI.

### Installation
1. **Download**: Install the Orion APK on your Android device. **Find the latest release APKs here:** [https://github.com/archudzik/orionAIScreenReader/releases](https://github.com/archudzik/orionAIScreenReader/releases)
2. **Accessibility Permissions**: Enable the accessibility service for Orion via Settings to allow screen reading.
3. **API Key Setup**: Go to [Google AI Studio](https://aistudio.google.com/app/api-keys) to create an API key and enter it in Orion settings.

When migrating from Orion `v1.4` or older, uninstall the previous app first. Then install the latest release, enable the Orion accessibility service again, and re-enter your settings.

### Permissions Required
The app needs the following permissions to work:
- **Vibrate** (`android.permission.VIBRATE`): To provide optional haptic feedback for actions and reading progress.
- **Internet** (`android.permission.INTERNET`): To send screenshots to Gemini for analysis and, when enabled, send generated reading text to Gemini TTS for audio generation.
- **Foreground Service** (`android.permission.FOREGROUND_SERVICE`): To capture screen content.
- **System Alert Window** (`android.permission.SYSTEM_ALERT_WINDOW`): To display the floating **Read Screen** button.

Screenshots are stored temporarily in the app's private storage and deleted after processing.

## Usage
1. Tap the **Read Screen** button, which floats over your screen.
2. Orion captures a snapshot of the visible screen.
3. The content is processed by Gemini AI to generate meaningful information.
4. The result is read aloud using Android TTS or the optional Gemini TTS voice engine.
5. Use the floating **Play/Pause** and **Repeat** controls when a reading is ready.

### Voice Engines
- **Android TTS** is the default. It is faster, uses less data, can work offline for playback, and uses your device's configured voice.
- **Gemini TTS** is optional. It requires internet access, sends the generated reading text to Gemini to create audio, may use more data, and can take slightly longer. The floating control shows **Synthesizing...** while audio is generated. If Gemini TTS fails, Orion falls back to Android TTS.

### Troubleshooting
Open Orion from the app drawer to access settings. If a reading does not complete, use **View recent diagnostics** or **Share diagnostics** to collect the latest entries. Gemini API keys are automatically redacted from shared diagnostics.

## Google API Key
![Orion in Action](OrionGIF2.gif)

## Development
### Code Overview
- **AndroidManifest.xml**: Declares required permissions and components, including activities and services.
- **MyAccessibilityService.kt**: Implements the core functionality for screen content accessibility.
- **ScreenCaptureActivity.kt**: Manages screen capture when triggered by the floating button.
- **ScreenCaptureForegroundService.kt**: Handles foreground tasks related to screen capture.
- **SettingsActivity.kt**: Provides the UI for configuring app settings.

### Build
To build the project, use Android Studio and make sure all permissions are declared correctly in `AndroidManifest.xml`.

## Contributing
Contributions are welcome! Please fork the repository, create a feature branch, and submit a pull request for review.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
