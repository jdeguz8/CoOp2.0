# NearMe - Locatation Based Reminder app

NearMe is a lightweight Android app built with Jetpack Compose, Google Maps, and Cloud Firestore that lets you save reminders tied to real-world locations.
Drop a pin anywhere → add a quick note → NearMe shows how far you are from each saved location using your device’s GPS.

## Features
	-Long-press to add reminders directly on the map
	-Save title + description
	-Stores reminders in Cloud Firestore (notes collection)
	-Shows current GPS position
	-Displays distance (“X meters away”) for every saved reminder
	-Real-time updates from Firestore through snapshot listeners
	-Material 3 UI (Compose) with a clean top app bar & reminder card overlay

## Tech Stack
- Kotlin
- Jetpack Compose
- Google Maps Compose
- Firebase Firestore
- ViewModel + StateFlow
