<!--suppress CheckImageSize -->
# <img width="48" height="48" alt="image" src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" /> Salarm
**Salarm** is a customizable, privacy‑friendly, open-source clock/salah app forked from BlackyHawky's Clock.  
It combines modern design, powerful features, and transparency — giving you full control over your time.

# 📑 Table of Contents

- [Download](#-download)
- [Features](#-features)
- [Common Issues](#-common-issues)
- [Contributing](#-contributing)
- [License](#-license)
- [Screenshots](#-screenshots)
- [Credits](#-credits)

# 📥 Download

[<img src="/images/badge_github.png" alt="Get it on GitHub" height="80">](https://github.com/qwibbler/Salarm/releases)
[<img src="/images/badge_f-droid.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.qwibbler.salarm/)
[<img src="/images/badge_izzy_on_droid.png" alt="Get it on IzzyOnDroid" height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.qwibbler.deskclock/)
[<img src="/images/badge_obtainium.png" alt="Get it on Obtainium" height="80">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/qwibbler/Salarm/releases)

> [!NOTE]  
> **Build variants:**
> - **Release:** Stable versions recommended for everyday use (available on all platforms above).
> - **Nightly:** Experimental builds with the latest changes, may be unstable (available on GitHub, and also on Obtainium if you enable _"Include prereleases"_).
> - **Debug:** Developer-oriented builds with extra logging and diagnostics (available only on GitHub).
>
> All variants (Release, Nightly, Debug) can be installed side by side without conflict.

# ✨ Features

### • 🕌 **Relational & Salah Alarms (Salarm Exclusive)**
  * Set alarms relative to Islamic prayer times (e.g., 15 mins before Fajr, 30 mins after Dhuhr, or halfway between Fajr and Dhuhr). 
  * One single rule lasts all year—alarm triggers dynamically shift as seasons change and daytime lengths adjust. 
  * Automatic background recalculation as your location or coordinates change (via GPS or manual city selection). 
  * Set "no earlier than" or "no later than" caps to prevent dynamic alarms from shifting into forbidden or unwanted windows. 
  * Dynamic auto-silence cutoffs tied to prayer events to mark alarms as "missed" before the next prayer window begins. 
  * Dynamic calculations for both Shafi'i and Hanafi Asr prayer times.

### • 🕌 **Masjid Alarms & Community Suite (Salarm Exclusive)**
  * Manage community mosques with a dedicated Masjid tab, custom calculation methods, and localized coordinates.
  * Configure 1 to 3 date intervals per month (e.g., 1st–15th and 16th–31st) to accurately mirror changing jama'at (congregational) times.
  * Supports both fixed-time jama'at schedules (e.g., Dhuhr/Juma) and relational offsets (e.g., 10 mins after Adhan).
  * Automated Do Not Disturb (DND) mode on device during jama'at times, with customizable before and after buffer minutes.
  * Selectable prayer event check-boxes to sync masjid events directly to active alarms.
  * Export printable yearly or monthly timesheets in PNG, PDF (A4 Portrait, A4 Landscape, Pocket Cards), or SVG formats.
  * Share and import entire masjid configurations instantly via compact QR codes.
  * Save multiple masjids and switch between them with quick-tap or long-press navigation controls.

### • 🧭 **Salah & Qibla Suite**
  * Live countdown to the next prayer event alongside full daily prayer schedules. 
  * View past or future prayer schedules, adjust calculation methods, or toggle manual location overrides. 
  * Dedicated Qibla visualizer and directional guide.

### • ⏰ **Advanced Alarms**
  * Set alarms to a specific date
  * Flip or shake to dismiss/postpone
  * Use power or volume buttons to snooze/stop
  * Swipe to delete, duplicate, or customize alarms
  * Custom titles, backgrounds, and ringtones (including random playback)

### • 🌐 **International Alarms**
  * Lock alarms to specific target timezones (e.g., `America/New_York`, `Asia/Riyadh`, `Europe/London`) so you never miss an event when traveling or coordinating across borders.
  * Automatically adjusts for Daylight Saving Time (DST) and regional time changes without manual recalculation.
  * Displays live local time in the target timezone and UTC offset (e.g., UTC+3) directly on alarm cards, edit bottom sheets, and notifications.
  * Automatic tagging ("International") for organized filtering and full JSON backup/restore support.

### • 🎨 **Customization**
  * Light, dark, or system theme
  * AMOLED mode for deep blacks
  * Digital or analog clock styles
  * Customizable interface, screensaver, and widgets
  * Dynamic colors for Android 12+

### • 🌍 **World Clock**
  * Display home time when abroad
  * View time in multiple cities worldwide

### • ⏱️ **Timer & Stopwatch**
  * Built-in timer and stopwatch
  * Share stopwatch results with contacts

### • ⚙️ **Extra Features**
  * Quick settings tiles (Android 7+)
  * Backup & restore (except custom ringtones)
  * Material Design UI
  * Support for [Direct Boot](https://developer.android.com/privacy-and-security/direct-boot)
  * Alarm support on some Snapdragon devices when powered off
  * [Reproducible Builds](https://reproducible-builds.org/) for transparency

> [!NOTE]  
> Some extra features may not work on certain devices:
> - **Direct Boot support**: see the discussion [here](https://github.com/BlackyHawky/Clock/issues/396).
> - **Power‑off alarm on Snapdragon devices**: may fail even if the _"com.qualcomm.qti.poweroffalarm"_ system app is present. See the discussion [here](https://github.com/BlackyHawky/Clock/issues/88).

# 🐞 Common Issues

* Device-specific issues may occur due to limited testing.
* On Android 14+ with HyperOS, the _"Full screen notification"_ permission may be revoked. Possible solution [here](https://github.com/BlackyHawky/Clock/discussions/303#discussioncomment-13407709).
* MIUI users may face problems due to aggressive battery optimizations.
  * Please make sure that battery optimizations are disabled for the app before opening an issue.

> [!NOTE]  
> I’m not an expert developer, so some problems may require community help to solve.

# 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on reporting issues, translations, and code contributions.

[//]: # ()
[//]: # (<details>)

[//]: # (<summary><b>Click here to see the translation status</b></summary>)

[//]: # (<br>)

[//]: # ()
[//]: # ([![Translation status]&#40;https://translate.codeberg.org/widget/clock/clock/multi-auto.svg&#41;]&#40;https://translate.codeberg.org/engage/clock/&#41;)

[//]: # (</details>)

# 📜 License

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Salarm is licensed under **GNU General Public License v3.0 (GPLv3)**.
This strong copyleft license requires that any modifications or larger works using Clock must also be distributed under the same license, with complete source code available.

See the [LICENSE](LICENSE) file for full details.

> [!NOTE]  
> Since Salarm is based on **AOSP Clock**, which is licensed under **Apache License 2.0**, an additional [Apache 2.0](LICENSE-Apache-2.0) license file is provided in this repository.

# 📷 Screenshots

<details>
<summary><b>Click here to see screenshots</b></summary>
<br>
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/AlarmPage.jpeg" alt="Screenshot Alarm Page" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/SetSalarmAlarm.jpeg" alt="Screenshot Set Salarm Alarm" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/SalarmAlarm.jpeg" alt="Screenshot Salarm Alarm" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/NamazTimes.jpeg" alt="Screenshot Salah Times" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/QiblaPage.jpeg" alt="Screenshot Qibla" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/01.jpg" alt="Screenshot 01" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/02.jpg" alt="Screenshot 02" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/03.jpg" alt="Screenshot 03" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/04.jpg" alt="Screenshot 04" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/05.jpg" alt="Screenshot 05" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/06.jpg" alt="Screenshot 06" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/07.jpg" alt="Screenshot 07" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/08.jpg" alt="Screenshot 08" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/09.jpg" alt="Screenshot 09" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/10.jpg" alt="Screenshot 10" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/11.jpg" alt="Screenshot 11" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/12.jpg" alt="Screenshot 12" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/13.jpg" alt="Screenshot 13" width="200" />
 <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/14.jpg" alt="Screenshot 14" width="200" />
</details>

# 🏅 Credits
* 🖼️ **App** forked from [BlackyHawkyClock](https://github.com/BlackyHawky)/[Clock](https://github.com/BlackyHawky/Clock)
* 💻 Code references and inspiration from:
    * [LineageOS](https://github.com/LineageOS/android_packages_apps_DeskClock)
    * [crDroid Android](https://github.com/crdroidandroid/android_packages_apps_DeskClock)

[//]: # (* 🌍 Translations provided by the community via [Weblate]&#40;https://translate.codeberg.org/projects/Salarm/&#41;)
* 🤝 Thanks to all [contributors](https://github.com/BlackyHawky/Clock/graphs/contributors) who help improve Salarm
