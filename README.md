# Reaction Challenge Mobile App

A native Android reaction game where players must respond to different events as quickly as possible to earn points before the timer runs out.

The game includes configurable difficulty levels and time limits, while keeping track of scores using local persistence.

## Features

* Fast-paced reaction-based gameplay
* Timed game sessions
* Multiple difficulty levels
* Configurable game duration
* Score tracking
* Local score persistence
* Simple mobile-friendly interface
* Game state managed using Android architecture components

## How It Works

The goal is simple: react correctly to the events displayed on screen before the available reaction window expires.

Players earn points for successful reactions, while the selected difficulty influences the challenge of the game.

At the end of a session, the player's result can be stored locally so scores remain available between app sessions.

## 🛠️ Built With

* **Java**
* **Android SDK**
* **AndroidX**
* **Material Components**
* **ViewModel**
* **LiveData**
* **Gradle Kotlin DSL**

## Architecture

The application separates game state and UI logic using Android architecture components.

Some of the main classes include:

* **MainActivity** — handles the main application screen and game configuration.
* **GameActivity** — manages the active game interface.
* **GameViewModel** — manages game state and logic independently from the activity lifecycle.
* **GameUiState** — represents the current state of the game UI.
* **game/** — contains game-related logic.
* **data/** — handles application data and local persistence.

Using `ViewModel` and `LiveData` helps keep the game's state management separate from the Android UI lifecycle.

## Requirements

* Android Studio
* Android SDK 24 or newer
* JDK 11+
* Gradle / Android Gradle Plugin configured through the project

Current Android configuration:

* `minSdk`: **24**
* `targetSdk`: **36**
* Java compatibility: **Java 11**

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Nehuelin/Reaction-Challenge-Mobile-App.git
```

### 2. Open the project

Open **Android Studio** and select the cloned project.

### 3. Sync Gradle

Allow Android Studio to download and synchronize the required dependencies.

### 4. Run the application

Select an Android emulator or connect a physical Android device and press **Run**.

## Project Structure

```text
app/
└── src/
    └── main/
        ├── java/com/example/reactionchallenge/
        │   ├── data/
        │   ├── game/
        │   ├── GameActivity.java
        │   ├── GameUiState.java
        │   ├── GameViewModel.java
        │   └── MainActivity.java
        ├── res/
        └── AndroidManifest.xml
```

## Purpose

This project was created to practice native Android development while exploring reaction-based game mechanics, timed events, state management, Android architecture components, and local data persistence.

## License

No license has currently been specified for this project.
