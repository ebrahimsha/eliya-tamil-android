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

## Version 1.0.2
- Added a fixed native app header with the official Eliya Tamil logo and title. Website content scrolls below it.


## Version 1.0.2
This update hides the duplicate website branding header inside the Android WebView. The native app header remains fixed. The website itself is unchanged, and bottom navigation is not intentionally hidden.
