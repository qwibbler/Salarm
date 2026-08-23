Welcome to the "Salarm" project! Since you're working with the "BlackyHawky/Clock" codebase (a modern fork of AOSP DeskClock), here is your roadmap for the core components you requested.
1. Alarm Logic: Definition, Saving, and Handling
   The app uses a ContentProvider-based architecture for persistence and a state-machine approach for scheduling.
   •
   Data Definition (Model):
   ◦
   Alarm.java: Defines the core alarm properties (time, repeat days, label, ringtone, etc.).
   ◦
   AlarmInstance.java: Represents a specific occurrence of an alarm (e.g., the instance firing tomorrow at 7:00 AM).
   •
   Database/Persistence:
   ◦
   ClockDatabaseHelper.java: The SQLite helper that manages the alarms and instances tables.
   •
   Handling & Scheduling:
   ◦
   AlarmStateManager.java: This is the heart of the alarm engine. It handles state transitions (e.g., from SILENT to FIRED to SNOOZED) and communicates with the Android AlarmManager to schedule the system-level wakeups.
   ◦
   AlarmUpdateHandler.java: Handles CRUD operations (adding, updating, or deleting alarms) and ensures the state is refreshed.
2. Main UI Layout (Alarm List Screen)
   The UI is built using standard Android XML layouts and the modern ViewBinding library.
   •
   Main Container:
   ◦
   alarm_fragment.xml: The primary layout for the alarm list screen.
   •
   List Item:
   ◦
   alarm_item.xml: Defines how an individual alarm row looks in the list.
   •
   Fragment Code:
   ◦
   AlarmFragment.java: The logic behind the alarm list UI.
3. Gradle Configuration
   To add the "adhan" library, you should modify the app-module level build file:
   •
   File: app/build.gradle
   •
   Location: Add the implementation line inside the dependencies { ... } block (usually found at the bottom of the file).
   dependencies {
   // ... existing dependencies
   implementation 'com.batoulapps.adhan:adhan:1.2.1' // Or the latest version
   }
   Quick Summary Roadmap
   Component
   Primary File Path
   Alarm Model
   app/src/main/java/com/best/deskclock/provider/Alarm.java
   Engine/Scheduling
   app/src/main/java/com/best/deskclock/alarms/AlarmStateManager.java
   Database
   app/src/main/java/com/best/deskclock/provider/ClockDatabaseHelper.java
   UI Layout
   app/src/main/res/layout/alarm_fragment.xml
   Gradle Config
   app/build.gradle
   Since you're planning to integrate relational salah alarms, AlarmStateManager and AlarmUpdateHandler will be your most important entry points for injecting the prayer time calculation logic. How would you like to proceed? I can help you set up the dependency or start drafting a plan for the Adhan integration.
