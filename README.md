# MOTO AI — Android AI Assistant

> **“Your Voice. Your AI. Your MOTO.”**

MOTO AI is a complete, futuristic voice assistant and device commander for Android powered by Google Gemini.

---

## Key Features

1. **Futuristic Futuristic AI Orb & Voice UI**:
   - Holographic animated AI orb with dynamic pulse and real-time audio reactive feedback.
   - Dual-ring gyroscopic rotation with state glow (Listening = Neon Green, Thinking = Deep Violet, Speaking = Bright Cyan).
   - Stop/cancel voice command button.

2. **Core AI Brain (Gemini 3.5 & Veo)**:
   - Powered by the latest Gemini REST API model (`gemini-3.5-flash`).
   - Zero hardcoded API keys: keys are configured via Settings and securely encrypted on-device with AES-GCM via the **Android Keystore**.
   - Structured JSON intent understanding with fallback on-device router when offline or unconfigured.

3. **Multilingual Voice Assistant**:
   - Wake name: **“MOTO”** (e.g. *“MOTO, open YouTube”*, *“MOTO, search for Free Fire”*, *“MOTO, call Rahul”*).
   - Speech-to-text and Text-to-speech with user-customizable voice pitch and rate.
   - Multilingual recognition and speech: English (US & India), Hindi (हिन्दी), Urdu (اردو), and natural Hinglish.

4. **Smart Android Actions**:
   - Launch installed apps (YouTube, Chrome, Free Fire, WhatsApp, etc.).
   - If an app is not installed, seamlessly opens its official Google Play Store page.
   - Web searches via Google Search Intent.
   - Android settings launcher (Wi-Fi, Bluetooth, Display, Apps, Battery).
   - Turn-by-turn navigation via Google Maps.
   - System reminders and alarms.
   - Safe phone call launcher with user confirmation dialogs (*“MOTO wants to call Rahul. Continue?”*).

5. **AI Image Generation**:
   - Generates artwork from text prompts using Gemini 2.5 Flash Image and Neural Diffusion.
   - Gallery viewing, download saving, and native Android sharing.

6. **AI Video Generation**:
   - Generates videos with camera choreography, resolution badges, and optional audio generation.
   - Progress bar tracking generation stages.
   - In-app preview and external playback.

7. **Security & Privacy**:
   - Android Keystore AES-GCM hardware-backed storage for credentials.
   - Clear local history and session wipe at any time.
   - Transparent microphone status indicator with runtime Android permission checks.

---

## Build & Run

1. Open the project in **Android Studio Hedgehog / Ladybug** or newer.
2. Ensure JDK 17 or JDK 21 is selected in Gradle settings.
3. Build the APK:
   ```bash
   gradle assembleDebug
   ```
4. Install on device or emulator:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. On first launch, open **Settings** (top-right gear icon) to input your Gemini API Key or run immediately with the on-device action router.
