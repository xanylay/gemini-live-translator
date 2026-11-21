# Gemini Live Translator — Build & APK Guide

Real-time Android audio translator built with Kotlin, Jetpack Compose, Material 3, Android Keystore AES-256 encryption, OkHttp WebSocket/REST streaming, and a draggable/resizable `WindowManager` Burmese floating subtitle overlay.

---

## 1. Project Structure

```text
app/src/main/
├── AndroidManifest.xml                      # Permissions & Foreground Service declarations
├── java/com/example/
│   ├── TranslatorApplication.kt             # App container (Settings, Room DB, StateHolder)
│   ├── MainActivity.kt                      # Edge-to-edge UI, adaptive nav, permissions, SAF .txt export
│   ├── domain/model/
│   │   └── TranslatorModels.kt              # AudioSourceMode, EngineMode, AppSettings, LiveSubtitleState
│   ├── data/
│   │   ├── local/
│   │   │   ├── CryptoManager.kt             # Android Keystore AES-GCM encryption for API key
│   │   │   ├── SettingsRepository.kt        # DataStore preferences & encrypted API key vault
│   │   │   ├── TranscriptDatabase.kt        # Room DB & DAO for Burmese transcript segments
│   │   │   └── TranscriptRepository.kt      # Persistence & UTF-8 .txt transcript formatter
│   │   └── remote/
│   │       ├── GeminiApiTester.kt           # Validates user Gemini API key & detects Live models
│   │       └── GeminiLiveTranslatorClient.kt# BidiGenerateContent WebSocket + Chunked Flash REST
│   ├── audio/
│   │   └── AudioCaptureManager.kt           # 16kHz PCM Mic & Android 10+ AudioPlaybackCapture + WavEncoder
│   ├── service/
│   │   ├── TranslatorStateHolder.kt         # Shared StateFlow bridging Service & Compose UI
│   │   ├── OverlayWindowController.kt       # Draggable, resizable, minimizable floating Burmese subtitle box
│   │   └── TranslatorOverlayService.kt      # Foreground Service (microphone | mediaProjection)
│   └── ui/
│       ├── theme/                           # Color.kt, Theme.kt, Type.kt (Noto Sans Myanmar)
│       ├── components/StudioComponents.kt   # Waveform visualizer, status badge, overlay preview
│       ├── screens/
│       │   ├── StudioScreen.kt              # Readiness checklist, source toggle, live Burmese monitor
│       │   ├── TranscriptsScreen.kt         # Searchable history, copy, share, SAF .txt export
│       │   └── SettingsScreen.kt            # API Key Vault, model selection, overlay customization
│       └── viewmodel/TranslatorViewModel.kt # MVVM state & actions
└── res/
    ├── font/                                # noto_sans_myanmar.ttf, space_grotesk.ttf, plus_jakarta_sans.ttf, jetbrains_mono.ttf
    └── drawable/                            # Custom adaptive launcher icon & studio hero banner
```

---

## 2. Key Permissions (`AndroidManifest.xml`)

- `INTERNET` & `ACCESS_NETWORK_STATE`: Connect to Gemini Live WebSocket / REST APIs.
- `RECORD_AUDIO`: Required for Microphone capture and Android `AudioRecord` playback capture.
- `SYSTEM_ALERT_WINDOW`: Draw the draggable Burmese subtitle window over other apps.
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`: Keep audio capture alive while watching videos in other apps (Android 14+ compliant).
- `POST_NOTIFICATIONS`: Foreground service notification on Android 13+.

---

## 3. Build APK in Android Studio

1. Open this project folder in **Android Studio** (JDK 17+ configured under *Settings > Build, Execution, Deployment > Build Tools > Gradle*).
2. Wait for Gradle Sync to finish.
3. **Debug APK**:
   - Menu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**
   - Or Terminal: `./gradlew :app:assembleDebug`
   - Output file: `app/build/outputs/apk/debug/app-debug.apk`
4. **Release APK**:
   - Menu: **Build > Generate Signed Bundle / APK > APK**
   - Create or select your `.jks` upload keystore, choose `release`, and click **Finish**.
   - Output file: `app/build/outputs/apk/release/app-release.apk`

---

## 4. Build APK Automatically via GitHub Actions

A complete workflow is included at `.github/workflows/android-apk.yml`.

1. Push this repository to GitHub on `main`.
2. Open the **Actions** tab -> **Build Android APK**.
3. Every push builds `:app:assembleDebug` and uploads `gemini-live-translator-debug-apk`.
4. To also build a signed Release APK in CI, add these GitHub **Repository Secrets**:
   - `KEYSTORE_BASE64`: Base64-encoded `.jks` keystore (`base64 -w 0 my-upload-key.jks`)
   - `STORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Key alias (`upload`) password
