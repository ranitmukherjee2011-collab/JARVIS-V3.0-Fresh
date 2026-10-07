# JARVIS V3.0 — Fresh Build

Native Android Kotlin + Jetpack Compose starter.

Included:
- Original animated JARVIS Heart Core
- Idle/listening/processing/speaking/executing/success/error states
- SpeechRecognizer voice input
- Text-to-Speech
- Dynamic installed-app launching
- WhatsApp launch
- Dialer
- Calendar event creation screen
- Android settings
- Microphone foreground-service scaffold

Production integrations still need secure backend/OAuth configuration:
Gemini agent, Gmail, Google Calendar API, Home Assistant and a dedicated
low-power wake-word engine. Never embed private API keys in the APK.

Open in Android Studio with JDK 17. Build:
gradlew.bat assembleDebug
