# MediaScreenSaver

MediaScreenSaver is an Android application that acts as a screen saver (Daydream) and displays information about the currently playing media on your device.

## Features

*   Displays media metadata (title, artist, album) of the currently playing track.
*   Activates as a screen saver when your device is charging or docked.

## How it works

The app uses Android's Notification Listener service to access media playback notifications from other apps. When media is playing, apps like Spotify, YouTube Music, and others post a notification with media metadata. MediaScreenSaver reads this information and displays it in a clean, minimalist interface when the screen saver is active.

## Setup

1.  **Grant Notification Access:**
    *   Open the MediaScreenSaver app.
    *   You will be prompted to grant "Notification Access".
    *   Click "Open Settings" to go to the Notification Access screen in your device's settings.
    *   Find "MediaScreenSaver" in the list and enable it.

2.  **Set as Screen Saver:**
    *   After granting permission, go back to the app.
    *   Click "Open Screen Saver Settings".
    *   Select "MediaScreenSaver" as your screen saver.
    *   You can also configure when the screen saver should start (e.g., while charging, docked, or both).

## Permissions

*   **Notification Access (`BIND_NOTIFICATION_LISTENER_SERVICE`):** This permission is required to read media playback notifications from other applications. MediaScreenSaver only reads notifications related to media sessions and does not collect any other personal information.

## Building the project

You can open this project in Android Studio and build it like any other Android application.

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Build and run the `app` module.
