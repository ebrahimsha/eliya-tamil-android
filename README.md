# Eliya Tamil Android App

Android WebView application for https://eliyatamil.com.

## Build APK using GitHub Actions
1. Upload the contents of this project to the root of the `eliya-tamil-android` repository.
2. Open the **Actions** tab.
3. Select **Build Eliya Tamil APK** and run the workflow, or wait for the push-triggered build.
4. Open the completed workflow run and download the `eliya-tamil-debug-apk` artifact.

Package ID: `com.eliyatamil.app`  
Minimum Android version: 7.0 (API 24)

This initial build creates a debug APK for testing. Before public distribution, create and securely store a release signing key. Keep the same key for every future APK update. Never commit the keystore or its passwords to this repository.
