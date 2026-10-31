<div align="center">
  <img width="256" height="256" alt="logoo" src="https://github.com/user-attachments/assets/4c5c4ddb-836d-4c59-8325-76b8c8d78bb3" />
  <h1>Open Launcher (Custom Fork)</h1>
  <p><strong>An open-source, offline-first Android launcher customized and optimized for aftermarket car head units.</strong></p>

  [![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

</div>

---

> [!WARNING]
> **Work in Progress:** This customized fork is actively under development and evolving. While tested on specific hardware, it needs to be thoroughly verified across different Android head units, screen resolutions, and MCU architectures. Feedback, bug reports, and test results from various head units are warmly welcomed!

---

## 🌟 Credits & Acknowledgements

This project is built upon the wonderful work of the open-source community:

- **David ([@dw2lam](https://github.com/dw2lam))**: Huge thanks to David for creating the original [Open Launcher](https://github.com/dw2lam/openlauncher). The core launcher architecture, lightweight design, and car-centric foundation originate from his work.  
  ☕ **Please consider supporting David for his original work:** [Donate via PayPal](https://paypal.me/dw2lam)
- **itsTwistys ([@itsTwistys](https://github.com/itsTwistys))**: Special thanks to [itsTwistys/openlauncher](https://github.com/itsTwistys/openlauncher) for the great ideas, inspiration, and concepts that guided many of the additional features and enhancements implemented in this fork.

---

## 🚀 Key Features & Enhancements in This Fork

### 🚗 Vehicle Integration & Telemetry
- **OBD-II Bluetooth Telemetry Widget (`ObdWidget`)**:
  - Direct connection to ELM327 Bluetooth OBD-II adapters without needing external companion apps.
  - Live vehicle gauges: Engine RPM (`010C`), Speed (`010D`), Coolant Temperature (`0105`), Throttle Position (`0111`), Calculated Engine Load (`0104`), Fuel Level (`012F`), Intake Air Temperature (`010F`), and Battery Voltage (`ATRV`).
  - Robust parser filtering out echoes, prompts, and noisy multi-ECU replies.
- **Car Display Animation & Avatar (`CarAnimation`)**:
  - Set a custom car PNG image on your dashboard.
  - **Drive-by Overlay**: Smooth, full-screen animation where your vehicle drives across the dashboard upon movement detection.

---

### 🔊 Advanced Head Unit Volume Protocols & Calibration
Many Android car head units do not use standard Android audio streams for the physical volume dial. This fork introduces deep head-unit audio integration:
- **Multiple Built-in Volume Protocols**:
  - Standard Android (`STREAM_MUSIC`) — for emulators, phones, and Qualcomm/Snapdragon units.
  - Microntek / MTC (`com.microntek.volumechange`) — for Rockchip PX3, PX5, PX6 units.
  - Junsun / Forfan (`forfan.intent.action.VOLUMESET`).
  - FYT / Joying / SYU (`com.syu.volumechange`).
  - HCT (`com.hct.volumechange`).
  - Rockchip Audio HAL (`av_volume`, `amp_volume`, `mcu_volume`).
- **Hardware Volume Knob Sniffer (`HeadUnitInspector`)**:
  - Non-root, non-ADB diagnostic tool. Watches `Settings.System`/`Settings.Global` changes and system broadcast intents in real time when you turn the physical volume knob to discover your unit's volume protocol.
- **Volume Calibration Wizard (`VolumeCalibrationScreen`)**:
  - Step-by-step interactive test wizard with audio test tones to test each protocol and lock in the right one for your vehicle.

---

### 📱 Dual-Page Dashboard & Driving State Auto-Switching
- **Two Independent Home Pages**: Swipe left/right between Page 0 and Page 1, each supporting its own distinct widget grid layout and widget content configurations.
- **Smart Driving Mode Auto-Switch**:
  - Automatically switches to a dedicated "Driving" dashboard page when vehicle GPS speed exceeds a configured threshold (e.g. 8 km/h).
  - Automatically reverts to the "Parked" page when the vehicle has been stopped for a set idle duration (e.g. 10 seconds).

---

### 🧩 Reworked & New Widgets
- **Expanded Grid (6×4)**: More flexible widget sizing and layout options.
- **Volume Widget (`VolumeWidget`)**: Dedicated tile with quick slider, step controls, mute toggle, and protocol status.
- **Quick Actions Widget (`QuickActionsWidget`)**: 1-tap toggles for Bluetooth, Wi-Fi, Screen Blackout (for night driving), Mute, and Launcher Settings.
- **App Shortcuts Widget (`AppShortcutsWidget`)**: Customizable launcher tile holding 1 to 4 apps with selectable vector icons.
- **Dynamic Weather Widget (`WeatherWidget`)**:
  - High-quality animated weather icons (Meteocons Lottie and SVG vector assets).
  - Dual weather providers: **Open-Meteo** and **wttr.in**.
  - Multi-day forecasts (up to 7 days).
  - GPS-based weather with configurable fallback location (e.g., city name).
- **Custom Soundboard Widget (`SoundboardWidget`)**:
  - Up to 9 configurable audio pads.
  - Preloaded fun audio clips (What's Up Doc, Arnold "I'll be back", A Few Moments Later, Greek Stamata, Cat meow, Horn, Rooster, Boing, Chime) with support for custom audio URIs.
- **Telemetry, Speedometer & Altimeter**: Digital-only options, compass offset calibration, and pitch/roll level calibration.

---

### 🛠️ Diagnostics, Safety & Backup Tools
- **Full Diagnostics Screen (`DiagnosticsScreen`)**: Real-time display of GPS coordinates, altitude, bearing, accuracy, sensors, active media playback sessions, network status, OBD connection status, and head unit hardware/firmware details.
- **Settings Backup & Restore (`SettingsBackup`)**: Versioned JSON export and import for all launcher preferences, coordinates, and widget layouts.
- **SAF Safe Export (`SafeExport`)**: Dual-write verification with internal fallback to prevent 0-byte corrupt exports on unstable head-unit storage providers.
- **Enhanced App Library (`AppLibraryScreen`)**: Filter applications by categories (User, System, Tools, Selected) and uninstall apps directly from the launcher.

---

### 🎨 Personalization & Visual Styling
- **Expanded Fonts**: Includes 8 typography choices: JetBrains Mono, Source Code Pro, Cal Sans, Fugaz One, Geom Variable, Noto Sans Display, Roboto, and System default.
- **Flexible Theming**: Dark, Light, Custom, Auto (day/night based on sunrise/sunset or light sensor), and System modes.
- **Per-Tile Widget Colors**: Each widget can follow the active theme, use default styling, or use a custom background color and transparency.
- **Widget Title Toggle**: Option to toggle widget title headers on or off for a cleaner minimalist appearance.
- **Background Gradients**: Support for Top-to-Bottom, Left-to-Right, Diagonal, and Radial gradients with customizable endpoint colors.
- **Status Indicators Toggle**: Option to hide top-right Bluetooth and Wi-Fi icons if your head unit already displays them in a system status bar.
- **Wallpaper Dimming**: Adjustable dim slider for improved widget legibility over wallpapers.

---

## 📥 Building & Installing

### Prerequisites
- Android Studio Ladybug | 2024.2.1 or newer (or command line Gradle)
- Android SDK 36 (minSdk 21)
- JDK 17

### Build via Command Line
```bash
# Clone the repository
git clone https://github.com/limbo666/openlauncher_custom.git
cd openlauncher_custom

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```
The compiled APK will be located in `app/build/outputs/apk/`.

---

## 🤝 Contributing & Feedback

Since aftermarket car head units run highly diverse Android forks, MCU boards, and custom hardware configurations, testing on real-world head units is invaluable.

If you encounter issues or have suggestions:
- Open an [Issue](https://github.com/limbo666/openlauncher_custom/issues) with your head unit model, MCU version, and Android version.
- Pull requests are always welcome!

---

## ☕ Support Original Creators

Please remember to support David for the original project that made this possible:
- **Original Repository:** [https://github.com/dw2lam/openlauncher](https://github.com/dw2lam/openlauncher)
- **Donate to David:** [https://paypal.me/dw2lam](https://paypal.me/dw2lam)
