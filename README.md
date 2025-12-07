# ⭐ RaceMe — Android Running Companion App

*A Beginner Friendly Motivation-Driven Performance & Community Running Platform*

---

# 📘 Abstract

RaceMe is a comprehensive mobile running companion application designed to support, motivate, and empower runners through interactive tracking, analytics, performance insights, community engagement, and gamified achievement systems.
Developed as a final academic project, RaceMe integrates real-time tracking, Firebase cloud services, social features, and motivational tools to encourage consistent physical activity and healthy competition.

The application demonstrates proficiency in **Android development, Kotlin**, **Firebase integration**, **sensor usage**, **real-time UI updates**, and **data-driven visualization**. It is structured using modern Android development practices such as ViewBinding, RecyclerView Adapters, and modularized Activity design.

---

# 🎯 Key Features Overview

## 🏃 Core Running Functionality

* **Start Run / Real-Time Tracking**
  Live distance, time, pace tracking using device GPS and sensors.

* **Runs & Ratings**
  View recent run history with performance summaries.

* **Step Counting**
  Uses built-in sensors to track steps during runs or events.

---

## 📊 Performance Analytics & Reporting

* **Weekly Reports**
  Visual summaries of daily mileage & minutes (7-day window).

* **Monthly Reports**
  Long-term mileage, effort, and consistency tracking (30-day window).

Uses MPAndroidChart for interactive and clean visualization.

---

## 🎖 Motivation, Rewards & Habit Formation

* **Challenges System**
  Structured goals with progress bars and achievement statuses.

* **Badges System**
  Unlockable badges with emoji icons for milestone achievements.

* **Reminders**
  Daily/weekly notifications via AlarmManager + BroadcastReceiver.

---

## 👥 Social & Community Features

* **Leaderboard**
  Ranks runners by distance, pace, or total runs.

* **Add Friends**
  Search, send requests, and view friend activity.

* **Community Feed**
  Create posts with text or images. Supports likes and engagement.

---

## 🏟 Track & Event Exploration

* **Local Tracks Browser**
  Explore curated nearby tracks/routes.

* **Group Races / Events**
  Schedule multi-user events for shared community running.

* **Create Custom Race**
  Define distance, goals, and run structure.

---

## 👤 User Profile

* View user stats, achievements, badges earned, challenge progress, and community presence.

---

# 🛠️ Technical Architecture & Stack

## 🔧 Technologies Used

| Layer                  | Technology                                      |
| ---------------------- | ----------------------------------------------- |
| Programming Language   | Kotlin                                          |
| IDE                    | Android Studio                                  |
| Backend / Data Storage | Firebase Authentication, Firestore, Storage     |
| UI Libraries           | ViewBinding, RecyclerView, MPAndroidChart, Coil |
| Sensors                | Step Counter, GPS                               |
| Notifications          | AlarmManager + BroadcastReceiver                |

---

# 📁 File Structure & Component Explanation

This section is written in a clear academic style for evaluation and professional documentation.

### **1. Activity XML Files**

Layouts that define the **user interface for full screens**.
Examples:

* `activity_home.xml`
* `activity_reports.xml`
  These contain buttons, charts, RecyclerViews, images, etc.

---

### **2. Item XML Files**

Reusable layouts for **single list items** inside RecyclerViews.
Examples:

* `item_post.xml`
* `item_badge.xml`
* `item_challenge.xml`
  These define the structure of each row (e.g., one post, one badge, one challenge).

---

### **3. Activity Kotlin Files (`.kt`)**

Logic controllers for each screen.
Examples:

* `ReportsActivity.kt`
* `StartRunActivity.kt`
* `ProfileActivity.kt`

Activities handle UI updates, Firebase calls, adapters, notifications, and user interactions.

---

### **4. Adapter Kotlin Files**

These bind lists of data to RecyclerView item XMLs.
Examples:

* `PostsAdapter.kt`
* `BadgesAdapter.kt`
* `ChallengeAdapter.kt`
  Adapters handle row layout inflation and data binding.

---

### **5. Model Classes**

Represent structured data objects stored in Firestore or used throughout the app.
Examples:

* `Post.kt`
* `BadgeRow.kt`
* `RunPoint.kt`
  Model classes ensure consistent data formatting and type safety.

---

### **6. Utility Components**

* **`BaseActivity.kt`** – Shared logic for screens that need consistent setup.
* **`ReminderReceiver.kt`** – Handles notification alarms.
* Additional helper classes for formatting, data handling, etc.

---

# 🔗 Firebase Integration Summary

### Authentication

* Email/password auth for user login and identity management.

### Firestore

* Stores runs, challenges, badges, posts, friends, and leaderboard data.
* Real-time syncing for community features.

### Storage

* Handles photo uploads for community posts.

---

# 🧭 Installation & Setup Instructions

### ✔ Requirements

* Android Studio Flamingo or newer
* Android SDK 33
* Internet connection
* Firebase account

---

### ✔ Steps

1. Clone or download the project.
2. Open Android Studio → **Open existing project**.
3. Place your Firebase file here:

   ```
   app/src/main/google-services.json
   ```
4. Build the project (Gradle will sync dependencies automatically).
5. Run on an emulator or physical device.

---

# 📘 Academic Contribution Statement

This project demonstrates mastery in:

* Android UI design and event handling
* Kotlin language fundamentals
* Firebase cloud services (Auth, Firestore, Storage)
* Charting and visualization
* Sensor integration
* Real-time data binding


---

# 📚 Future Enhancements (Optional Section)

* Calender API
* Google Map Overlay
