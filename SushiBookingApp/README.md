# Sushi Table — Android Studio starter project

A student project prototype for Mobile Application Development. Built with Kotlin, Jetpack Compose, Room, Retrofit, and Kotlin Coroutines.

## Features
- Multiple screens: Home, Book, My Bookings, Sushi Helper
- Data persistence: Room stores bookings locally on the device
- External API: Retrofit requests recipe results from `https://dummyjson.com/recipes/search?q=sushi`
- Asynchronous work: API calls and database operations run inside `viewModelScope.launch`
- Chatbot: simple rule-based demo chatbot (not a generative AI model)

## Open in Android Studio
1. Extract the ZIP file.
2. Open the `SushiBookingApp` folder in Android Studio.
3. Use JDK 17 if prompted.
4. Allow Gradle to sync and download dependencies.
5. Run on an emulator or Android device with internet access.

## API explanation
The app sends an HTTP GET request to DummyJSON. The server responds with JSON recipe data. Retrofit converts JSON into Kotlin data classes. A coroutine runs the request without blocking the UI.

## Important limitations
- Branch names and booking times are sample data.
- Bookings are saved only on the device using Room.
- The demo does not connect to Sushiro, Katsumidori, or any real restaurant reservation system.
- The chatbot uses simple keyword rules, not a real AI service.
- A production app needs a backend API that validates availability and prevents double-booking. To use a real AI model, connect to an AI provider through a secure backend; do not embed secret API keys in the Android app.
