# FitTrackr 🏋️‍♂️📊

![FitTrackr Banner](images/fittrackr-banner.png)

FitTrackr is a comprehensive fitness tracking app designed to help users monitor and improve their health and fitness journey. From managing workouts to tracking nutrition and setting goals, FitTrackr provides all the tools you need to stay on track and motivated.

---

## Table of Contents

1. [Demo Video](#-demo-video)
2. [Purpose](#purpose)
3. [Features](#features)
4. [Design Considerations](#design-considerations)
5. [GitHub Integration](#github-integration)
6. [GitHub Actions](#github-actions)
7. [Getting Started](#getting-started)
8. [AI Use](#ai-use)

---

## 🎥 Demo Video

**Watch the complete application demonstration:**
[FitTrackr Demo Video](https://youtu.be/yq7gIIwdLGo)

The demo showcases:
- User registration and Google SSO login
- Settings management and profile customization
- REST API integration with backend database
- All custom fitness tracking features
- Offline functionality and data synchronization


---

## Purpose

The primary goal of FitTrackr is to empower users to take control of their fitness journey. The app allows users to:

- Maintain a **workout library** with pre-defined exercises.
- Start and track workout sessions with real-time metrics including **time, heart rate, and calories burned**.
- Monitor **nutrition** intake and set dietary goals.
- Set **personal goals** for workouts, calories, heart rate targets, and more.
- Visualize progress over time through detailed summaries and charts.

FitTrackr is built with the philosophy that fitness is easier to maintain when all aspects of your health are tracked in a single, intuitive platform.

---

## Features

- **Workout Library** – Browse exercises, add custom workouts, and categorize routines.
- **Session Starter** – Track active workout metrics such as duration, heart BPM, and calories burned.
- **Nutrition Tracker** – Log meals and track calories, macros, and hydration.
- **Goal Management** – Set and monitor personal fitness targets.
- **Progress Overview** – Visual charts and summaries to track achievements over time.
- **User-Friendly UI** – Designed to be intuitive for both beginners and advanced users.

---

## Release Notes

### Version 1.0 - Final POE Release

This release represents the complete implementation of FitTrackr, transforming the Part 1 prototype into a fully functional fitness tracking application.

#### New Features Since Prototype

**Authentication & Security:**
- ✅ **Single Sign-On (SSO)** - Google Sign-In integration via Firebase Authentication
- ✅ **Biometric Authentication** - Fingerprint and facial recognition support for secure, quick access
- ✅ **Secure User Sessions** - Token-based authentication with automatic session management

**Backend Integration:**
- ✅ **REST API Integration** - Custom Node.js/Express backend connected to Firebase Firestore
- ✅ **Real-time Data Sync** - Automatic synchronization between local Room database and cloud backend
- ✅ **Offline-First Architecture** - Full functionality when offline with automatic sync when connection is restored

**Data Visualization (User Defined Feature 4):**
- ✅ **Progress Charts** - Interactive bar and line charts using MPAndroidChart library
- ✅ **Weekly Trends** - Visual representation of steps and calories burned over the week
- ✅ **Progress Bars** - Quick visual indicators for goal completion (works alongside charts)

**Custom Workout System (User Defined Feature 5):**
- ✅ **Custom Workout Creation** - Create personalized workout routines with exercises
- ✅ **Exercise Management** - Add exercises with reps, sets, and duration tracking
- ✅ **Workout Library** - Save and reuse custom workouts

**Notifications:**
- ✅ **Push Notifications** - Firebase Cloud Messaging (FCM) integration
- ✅ **Real-time Alerts** - Achievement notifications, workout reminders, and daily progress updates

**Localization:**
- ✅ **Multi-language Support** - English, isiZulu (Zulu), and Afrikaans
- ✅ **Dynamic Language Switching** - Change language in settings without app restart
- ✅ **Localized Content** - All UI elements translated to supported languages

**Data Storage:**
- ✅ **Room Database** - Local SQLite database for offline data storage
- ✅ **Base64 Image Storage** - Profile images stored as Base64 strings in Firestore (blob storage implementation)
- ✅ **Data Persistence** - All user data persisted locally and synced to cloud

**Additional Features:**
- ✅ **Nutrition Tracking** - Comprehensive meal logging with macronutrient tracking
- ✅ **Daily Activity Tracking** - Steps, calories, distance, and active minutes
- ✅ **Goal Management** - Set and track fitness goals with progress monitoring
- ✅ **Profile Management** - Customizable user profiles with image upload

#### Technical Improvements

- **Offline Sync Worker** - WorkManager-based automatic sync when network is available
- **Network Monitoring** - ConnectivityManager integration for real-time network state detection
- **Error Handling** - Comprehensive error handling and user feedback
- **Code Quality** - KDoc comments, logging, and code references throughout
- **Testing** - Unit tests and instrumented tests for core functionality
- **CI/CD** - GitHub Actions workflow for automated builds and testing

#### User Defined Features

**Feature 4: Data-driven Progress Tracking with Charts**
- Interactive bar charts for weekly steps visualization
- Line charts for calories burned trends
- Progress bars for quick goal completion overview
- Historical data analysis and trend identification

**Feature 5: Custom Workout Creation System**
- Create custom workout templates with multiple exercises
- Exercise details: reps, sets, duration, and notes
- Save and reuse custom workouts
- Integration with workout session tracking

#### Known Limitations

- Profile images are stored as Base64 strings (suitable for small images, max 1MB)
- Offline sync requires network connection to complete
- Charts display weekly data only (monthly/yearly views planned for future releases)

---

## Design Considerations

When designing FitTrackr, the following aspects were prioritized:

- **User Experience (UX):** Simple, intuitive interfaces that make navigation effortless.
- **Real-Time Tracking:** Metrics like heart rate and calories are updated live during workouts.
- **Data Security:** Users’ personal fitness and nutrition data is stored securely.
- **Scalability:** Designed to handle a growing library of exercises and user data.
- **Cross-Platform Compatibility:** Ensures consistent experience across devices.


---

## GitHub Integration

FitTrackr leverages **GitHub** for version control and collaborative development:

- **Repository Management:** All source code is maintained in a public/private GitHub repository for version tracking and collaboration.
- **Branching Strategy:**  
  - `main` branch: Stable production-ready code   
  - Feature branches: Individual features or bug fixes
- **Pull Requests:** All changes are reviewed through pull requests before merging into `main`.

---

## GitHub Actions

FitTrackr uses **GitHub Actions** to automate development workflows:

- **Continuous Integration (CI):** Automatically builds and tests the app whenever code is pushed to `main`.
- **Continuous Deployment (CD):** Deploys stable versions to production or staging environments automatically after passing all tests.
- **Code Quality Checks:** Linting, formatting, and unit tests are executed to maintain high code quality.

---

## Getting Started

To run FitTrackr locally:

1. Clone the repository:  
   ```bash
   [git clone https://github.com/YourUsername/FitTrackr.git](https://github.com/IIEWFL/prog7314-part-2-ASBS.git)
2. Run Application on Android Studio

## Data Storage & Blob Storage

FitTrackr uses a hybrid storage approach combining local and cloud storage:

**Local Storage (Room Database):**
- SQLite database for offline-first architecture
- Stores all user data locally for instant access
- Automatic sync to cloud when network is available

**Cloud Storage (Firebase Firestore):**
- User profiles, workouts, nutrition entries, and goals
- Real-time synchronization across devices
- Secure, scalable cloud infrastructure

**Blob Storage (Base64 Encoding):**
- Profile images are stored as Base64-encoded strings in Firestore
- This approach provides:
  - Simple implementation without separate storage service
  - Direct integration with Firestore documents
  - Suitable for profile images (max 1MB per image)
  - No additional storage costs or configuration needed
- Images are compressed and resized before encoding to optimize storage
- Base64 encoding is a valid blob storage implementation for small binary data

**References:**
- Room Database: https://developer.android.com/training/data-storage/room
- Firebase Firestore: https://firebase.google.com/docs/firestore
- Base64 Encoding: https://developer.android.com/reference/android/util/Base64

---

## Play Store Preparation

FitTrackr is ready for publication on the Google Play Store. The following assets and configurations are prepared:

**Release Build:**
- Signed release APK configuration in `app/build.gradle.kts`
- ProGuard rules configured for code obfuscation
- Version code and version name set appropriately

**Required Assets:**
- App icon (ic_launcher.png) - 512x512px
- Feature graphic - 1024x500px (to be created)
- Screenshots - Phone (1080x1920px) and tablet (optional)
- App description and release notes

**Play Console Setup:**
- Firebase project configured with Play Console integration
- App signing by Google Play enabled
- Release tracks configured (internal testing, alpha, beta, production)

**Compliance:**
- Privacy policy prepared
- Content rating completed
- Target audience defined
- Permissions documented

For detailed Play Store preparation instructions, see `docs/play-store-assets/README.md`

---

## AI Use

**AI Tools Used:**

**ChatGPT for Image Generation**
ChatGPT was used to generate visual assets for the FitTrackr Android fitness application. This included:
- Creating the main app logo and icon (icon_fitness.png)
- Generating navigation icons for the bottom navigation bar
- Designing vector graphics for UI elements and visual components

**Cursor IDE for Code Debugging**
Cursor IDE with AI assistance was used throughout the development process to:
- Debug Android code issues and errors
- Provide code suggestions and corrections
- Assist with Kotlin syntax and Android framework integration
- Help with navigation implementation and UI component development

These AI tools were used as development assistants to support the creation of the fitness tracking application, helping with both visual design elements and technical implementation challenges.

**References:**
- ChatGPT: https://openai.com/chatgpt
- Cursor IDE: https://cursor.sh/

