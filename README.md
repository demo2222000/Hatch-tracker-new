# HatchMaster - Smart Egg Incubator Manager

HatchMaster is a modern, high-precision Android application designed for poultry breeders, homesteaders, and hobbyists to monitor incubation environments, track egg turning schedules, log candling results, and generate complete incubation reports.

---

## 🚀 GitHub Actions: Automated APK Build

This repository includes continuous integration (CI) via GitHub Actions that automatically builds and packages your Android APK whenever code is pushed.

### Workflow Configuration File:
- **Location:** `.github/workflows/build-apk.yml`

### When is the APK built automatically?
1. **On Every Push:** Pushes to `main` or `master` branches automatically trigger a build.
2. **On Release Tags:** Pushing a version tag (e.g., `git tag v1.0 && git push origin v1.0`) will build the APK and publish it directly to the **GitHub Releases** page of your repository.
3. **Manual Trigger (`workflow_dispatch`):** 
   - Go to your repository on GitHub.
   - Click the **Actions** tab.
   - Select **"Build Android Debug APK"** from the left sidebar.
   - Click **"Run workflow"** (you can optionally toggle the checkbox to publish directly as a GitHub Release).

---

## 📥 How to Download the APK from GitHub

### Option A: From GitHub Actions Artifacts (For Every Build)
1. Navigate to your repository on GitHub.
2. Click the **Actions** tab.
3. Click on the latest workflow run (e.g., "Build & Package APK").
4. Scroll down to the **Artifacts** section at the bottom of the summary page.
5. Click **`HatchMaster-debug-apk`** to download the ZIP file containing `HatchMaster-debug.apk`.

### Option B: From GitHub Releases (Direct Download)
- When a tag is pushed or the release option is selected in manual trigger, the APK file is attached directly under the **Releases** tab on your GitHub repository page.

---

## 🛠️ Key Files for Building the APK

| File Path | Description |
|---|---|
| `.github/workflows/build-apk.yml` | GitHub Actions workflow that installs JDK 21, sets up the Android SDK, decodes the keystore, compiles the project, and uploads the APK. |
| `gradlew` & `gradlew.bat` | Portable Gradle wrapper executables for Linux/macOS and Windows. |
| `gradle/wrapper/gradle-wrapper.properties` | Defines the Gradle distribution version (`gradle-9.3.1-bin.zip`). |
| `gradle/wrapper/gradle-wrapper.jar` | The Gradle wrapper binary used by CI and local builds without needing Gradle pre-installed. |
| `debug.keystore.base64` | Base64-encoded debug signing keystore preserved for reproducible builds on GitHub Actions runners. |
| `app/build.gradle.kts` | App-level build script configuring Android SDK 36, Jetpack Compose, dependencies, and APK signing configurations. |
| `settings.gradle.kts` | Project structure and plugin repository settings. |
| `gradle/libs.versions.toml` | Centralized version catalog for Android dependencies. |

---

## 💻 Building the APK Locally

If you clone the repository to your own computer:

```bash
# Ensure gradlew has execution permissions
chmod +x ./gradlew

# Build the debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```
