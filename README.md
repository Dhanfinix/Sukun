# Sukun

Sukun is an Android app that helps you manage prayer times and automatically silences your phone during prayers. It utilizes Aladhan API for fetching accurate prayer schedules and uses AlarmManager to handle background silencing tasks precisely.

## Features

- **Prayer Times:** Gets accurate prayer times based on your location.
- **Auto-Silence:** Automatically sets your phone to vibrate or silent mode during prayer times.
- **Onboarding:** Simple setup to get started with location and notification permissions.

## Tech Stack

- **UI:** Jetpack Compose
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
- **Local Data:** Room Database for caching prayer times, DataStore for user preferences
- **Network:** Retrofit + kotlinx.serialization for the Aladhan API
- **Background Tasks:** AlarmManager for exact device silence scheduling and restoring volume

## Getting Started

1. Clone the repository.
   ```bash
   git clone https://github.com/Dhanfinix/Sukun.git
   ```
2. Open the project in Android Studio.
3. Build and run the app on an Android emulator or a physical device.

## License

This project is licensed under the MIT License.
