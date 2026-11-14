# OpenLauncher Custom — User Guide & Reference

**Version covered: 0.9.119 (versionCode 119)**

A launcher built for Android car head units (PX30 "NaviRadio" rigs, FYT/Microntek and similar). It replaces the stock launcher with a fully customisable dashboard: widgets on a grid, live GPS/telemetry, media control, hardware-radio mirroring, OBD2 telemetry, an audio spectrum, per-driver profiles, and a floating quick-controls panel — designed to be operated with one finger while driving.

---

## 1. First run

When the launcher starts for the first time a 5-step wizard walks you through:

1. **Introduction** — tap the ✕ (top-right) at any time to exit the wizard; you can bring it back from Settings → Maintenance → Run Setup Wizard.
2. **Location Services** — needed for the GPS speedometer, compass, altimeter, weather and trip meter.
3. **Media Integration** — notification access so the Now Playing widget can read and control music.
4. **Audio & System** — microphone (spectrum visualizer), usage access (floating panel auto-hide), **draw-over-apps** (floating panel above other apps), **accessibility** (floating BACK key), **Bluetooth** (OBD2) — each with a live granted/missing status.
5. **Ready to Go** — optionally set OpenLauncher as the default Home app.

Every permission can be granted or re-checked later in **Settings → Permissions**. You can re-run the wizard anytime from **Settings → Maintenance → Run Setup Wizard**.

---

## 2. The home screen

- **Header** — edit (✏️) button enters *edit mode*; the vehicle name is tappable and opens the **driver profile picker** (see §7). The title itself is smart: it shows the **active driver's name**, unless the car has been renamed from the default "MY CAR" (Settings → Vehicle & OBD → Vehicle Name), in which case it shows the car name. Right after a profile switch, the driver name flashes for 5 seconds before this rule kicks in.
- **Two pages** — swipe left/right or tap the page dots under the grid to switch between two independent widget pages.
- **Sidebar (menu bar)** — can be on the left, right or bottom (Settings → Display → Sidebar). Contains:
  - **Shortcut slots** — tap to launch the assigned app; **long-press** for the action sheet (Open app / Change app / Customize icon / Remove); **drag** to reorder; an empty slot opens the unified app picker on tap.
  - **Nav buttons** — Apps, Settings, Home.
- **Edit mode** (✏️ icon):
  - **Drag** widgets to move them, drag the bottom-right corner to resize.
  - **+** opens the widget library (add/remove widgets).
  - **Palette** (appears next to Done/+ while editing) opens the glass-tint dialog for **all active widgets at once** — pick a preset tint or "Follow theme" / "Default (smoke)".
  - **Long-press** a widget for its context menu: background color/mode, per-widget configuration, remove, etc.
- **Two-finger tap** and **three-finger tap** on the home screen can launch any app (Settings → Launcher → Gestures).
- **Tap sound** — every real control (buttons, switches, chips, widget controls) plays a short tick/beep if enabled (Settings → Launcher → Sound on Tap).

### Widgets

| Widget | What it shows |
|---|---|
| Clock | Digital or analog clock |
| Weather | Current conditions + multi-day forecast (wttr.in or Open-Meteo) |
| Now Playing | Track/artist, album art background, play/pause/next/prev, radio deck |
| Telemetry (Compass) | Compass rose, heading, GPS status |
| Volume | Volume up / down / mute |
| Quick Actions | Toggles: Bluetooth, Wi-Fi, blackout, mute, system settings, day/night |
| Altimeter | Roll, pitch & altitude (with zero calibration) |
| Speedometer | GPS speed — three modes via long-press: **Dial track** (analog gauge), **Digital only** (big number), **Animals** (animal for your speed range from the sprite sheet — tree at 0, tortoise → peregrine falcon up to 141+ km/h — with a small speed readout at the bottom) |
| Vitals | Head-unit health (CPU/RAM/temp etc.), configurable elements |
| Trip Tracker | Trip distance, drive/idle time, average speed + 0–100 accel test (see §8) |
| Soundboard | Custom sound pads |
| App Shortcuts | Quick app launch pad (1–8 slots) |
| OBD2 | Live engine & sensor telemetry from a Bluetooth ELM327 adapter |
| Network Speed | Upload/download speed |
| Spectrum | Live audio spectrum visualizer |
| Map | Live map with position — Google or OSM tiles (see Maps settings). Chips: map type + traffic (Google) or OSM badge; **SEARCH** chip (online only) finds places and can navigate; zoom buttons + recenter; drag to browse (recenter resumes follow). An **OFFLINE** badge appears when there is no internet — the widget then serves cached OSM tiles only |

Widgets can be added on **each page independently** (the same widget can exist on both pages with different content — see Widget Content below).

---

## 3. App Library

Open via the sidebar **Apps** button.

- **Search** by app name.
- **Filter chips**: `Selected` · `Tools` · `Installed` · `System` · `All`.
- **Tap** an app to launch it.
- **Long-press** an app for the management dialog:
  - ⭐ **Star** — mark as "Selected" (pinned to the Selected filter)
  - 🛠️ **Tool** — mark as a diagnostic tool (Tools filter)
  - **Uninstall**
- The app library is also used full-screen as the picker for CarPlay / Android Auto / PiP / radio app assignments.

### The unified app picker

Every "choose an app" action in the launcher — sidebar shortcuts, app-shortcut widget slots, floating-panel buttons, radio app, gestures, auto-launch — uses the **same dialog**:

- search by app name *or* package,
- the same `Selected / Tools / Installed / System / All` filters,
- an icon grid with the currently assigned app highlighted,
- a red **CLEAR** button (labelled `AUTO-DETECT` in the radio-app picker) to remove the assignment.

---

## 4. Settings reference

Settings are organised in 6 categories (top of the screen): **All · Display · Dashboard · Widgets · Vehicle & OBD · Launcher**. There is also a **search field** — type a keyword to filter sections.

Each subcategory (section) has its **own colour** shown as a left-edge bar, and while you scroll a small **sticky banner** at the top-left shows the current section's name with the same colour dot — so you always know where you are without reading the small headers.

### Display

| Section | Options |
|---|---|
| **Appearance** | Display Mode (Light / Dark / Custom / Auto / follow system), Accent Color (6 presets + picker), custom Background color, Font Color, Help Text Color, **Gradient** background + direction, **Wallpaper** (image + dim; picking a new image also turns the video wallpaper off, and REMOVE WALLPAPER clears both), **Video Wallpaper** (looping muted video — H.264 MP4 recommended, pauses when the launcher is hidden, picker opens in Downloads with all file categories), **Particle Effect** (snow / rain / stars + amount), Status Bar Indicators (BT/Wi-Fi/data), Network Speed Indicator |
| **Typography** | Font (opens a grid of live preview cards — each renders its name and "Free by Limbo666" in its own face; System, JetBrains Mono, Source Code Pro, Cal Sans, Fugaz One, Geom, Noto Sans Display, Roboto, DSEG14 LCD, IBM VGA 9x16, Commissioner, Didact Gothic, Fira Sans, Handjet, GFS Neohellenic, Play, Playpen Sans, Sansation, Tektur, Ubuntu), Bold Font, Text Scale, UI Scale |
| **Tiles** | Tile color for dark/light mode, tile transparency |
| **Sidebar** | Sidebar Position (Left/Right/Bottom), Menubar Width, Menubar Icon Size, Shortcuts Side (which edge the shortcut slots sit on), shortcut slots (assign an app per slot), **Add Slot** |

### Dashboard

| Section | Options |
|---|---|
| **Home Pages** | **Auto-Switch Pages** by driving state, Driving Page, Switch Speed (km/h), Idle Time |
| **Display Animation** | Car image beside the vehicle name — drives across when moving, rests when stopped; choose your own car image |

### Widgets

| Section | Options |
|---|---|
| **Widget Content** | Per-page editing selector; App Shortcuts Count; Soundboard Buttons |
| **Spectrum** | Rise Speed (attack), Fall-off Speed (decay), Response Curve, per-page bars count / style (bars, rounded, dots) / mirror / color / gain, fallback image |
| **Now Playing** | Radio App (assign or AUTO-DETECT), Show FM/AM Band Chips, Station Change Popup, Artwork Size, Artwork Dim |
| **Maps** | Map widget (enable from the widget library), **Map Provider (Google / OSM)** — OSM is free & Google-free with an offline tile cache, **Control Size (Normal / Large)** — Large enlarges the map's buttons/chips/search panel 30% for easier tapping in the car, Map Type (Roadmap / Satellite / Hybrid / Terrain — Google only), Live Traffic (Google only) |
| **Weather** | Dynamic GPS location, Custom Location / Fallback City, Provider (wttr.in / Open-Meteo), Animated Icons (Meteocons), Forecast Days (3/5/7) |
| **Floating Panel** | See §6 |

### Vehicle & OBD

| Section | Options |
|---|---|
| **Vehicle & OBD** | Vehicle Name (tappable on home = profile picker), Unit System (metric/imperial), **OBD2 Bluetooth Adapter** (pair an ELM327), OBD fallback image |
| **GPS & Calibration** | Reset A-GPS data, Magnetometer Sweep (parking-lot calibration), Compass Heading Offset (−180°…+180°) |

### Launcher

| Section | Options |
|---|---|
| **Profiles** | Active profile (4 buttons), Rename Profile, **Auto-Switch by Wi-Fi** (see §7) |
| **Gestures** | Two-Finger Tap → app, Three-Finger Tap → app |
| **Startup** | Auto-Launch App + delay (1–30 s) |
| **Sound on Tap** | Enable + sound type (tick / beep) |
| **Telegram Messages** | **Driver Inbox** — remote messages from a Telegram bot (see §9a): token, allowed senders, look-back window, auto-dismiss, read receipts, quick replies, command toggles, header indicator, test + log |
| **Recovery** | Restart Launcher |
| **Backup & Restore** | Export settings to JSON file, Restore from JSON |
| **Permissions** | Status + shortcuts for: Default Launcher, Notification Access, Location, Draw Over Other Apps, Microphone, Usage Access, Accessibility (Back key) |

At the bottom of the Launcher category (always visible): **Updates** (link to the fork) and **Maintenance** — System Status (diagnostics screen), **Volume Calibration**, Export Debug Log, Re-run Setup Wizard.

---

## 5. Media, radio & OBD

- **Now Playing** mirrors the active Android media session (Spotify, Radio apps, local players…) with playback controls and album-art background. Tap the track title to open the source app.
- **Radio deck**: if your unit exposes a hardware radio (auto-detected, e.g. szchoiceway/Microntek MCU), the deck mirrors FM/AM — band chips, seek, presets, frequency display. A "station change" glass popup can be enabled (Widgets → Now Playing). Otherwise you can assign a radio app, or leave it on AUTO-DETECT.
- **OBD2**: pair an ELM327 Bluetooth adapter (Vehicle & OBD → OBD2 Bluetooth Adapter) to get live engine sensors in the OBD widget.

---

## 6. Floating quick-controls panel

Enable in **Widgets → Floating Panel → Floating Quick Controls**.

- A draggable overlay panel that floats **above other apps** (Home / Back / Volume / Mute / Screen blackout / Day-Night / any app).
- **Drag the handle** to move it; the position is remembered.
- **Tap a button row** (Settings → Floating Panel → Buttons) to change its action — up to 4 buttons, the "Launch App" action uses the unified app picker.
- Options: Panel Size, Panel Opacity, Vertical Layout.
- The panel **auto-hides** while the launcher is visible and over sensitive apps (needs Usage Access).

---

## 7. Driver profiles

Four independent profiles (DRIVER 1–4, renameable). Each keeps its **own** widgets, colors, layout, gestures, sounds, pages and all settings.

**Shared across profiles** (hardware-level, not per-driver): OBD pairing, radio app, volume channel, CarPlay/AA/PiP apps, profile names, Wi-Fi auto-switch rules, retro theme.

Ways to switch:
- **Home header**: tap the vehicle name → picker dialog.
- **Settings → Launcher → Profiles**: tap a profile button.
- **Automatically by Wi-Fi**: enable *Auto-Switch by Wi-Fi* and type your phone hotspot's name (SSID) into your profile's field. When the unit connects to that Wi-Fi, your profile activates automatically. (Requires Location permission — Android redacts the SSID without it.)

---

## 8. Trip Tracker & 0–100 accel test

The trip widget is a manual session meter (does **not** survive launcher restarts):

- **OPR** — start/stop the trip (counts only above ~1.8 km/h; idle time counts below that).
- **RST** — reset distance / drive time / idle time / average speed.
- **EXT** — switch to the **0–100 accel test** (0–60 in imperial): arms automatically when the vehicle moves, times the run to 100 km/h, keeps the best record.
- **SET** — reserved.
- Hidden toy: in accel mode while READY, **tap the speed readout** to run a simulated acceleration.

---

## 9. Diagnostics, calibration & recovery

- **System Status** (Settings → Maintenance): live permissions, sensors, network, screen info — with a **Save Status Report** export.
- **Volume Calibration**: a mute-probe wizard that finds which volume channel your head unit responds to (TEST → listen for the ~1.5 s mute → confirm). Includes a **Knob Sniffer** that logs Settings keys/broadcasts while you turn the physical knob.
- **Restart Launcher** (Settings → Launcher → Recovery) — last line of defence for runtime issues.
- **Backup & Restore** — export/import the complete configuration as JSON.

---

## 9a. Driver Inbox (Telegram remote messages)

Receive messages on the head unit from a **Telegram bot** — no phone number, no Telegram app, no server of your own.

**One-time setup:** create a bot with **@BotFather** (one message), then on the unit go to *Settings → Launcher → Telegram Messages → Driver Inbox*, paste the **bot token**, add your **Telegram user ID** under *Allowed Senders*, and flip *Master Switch* on. Anyone on the allow-list just opens a normal chat with the bot and types.

- **Normal messages** pop up as a top banner for the *Dismiss After* time (default 15 s), with **quick replies** and a ✕. Dismissal sends a **"✅ Read"** receipt back to the sender.
- **Commands** (case-insensitive): `GPS` → location + Maps link · `TRACK 5` → position/speed/heading every minute for 5 minutes, each with a Maps link · `CLEAR` → clears pinned messages + stops tracking · `STATUS` → speed, GPS, profile, battery, uptime · `WARN <text>` → a pinned alert that cannot be dismissed for 30 s.
- **Look-back window** ignores messages older than the selected age (1–12 h) so stale backlog doesn't flood the screen after being offline.
- A small **header dot** shows while the bot is connected (independent toggle).
- Security: the bot **only answers allowed senders** (numeric IDs, `@usernames`, or `@all` for anyone — `@all` is insecure); the token never leaves the device.

---

## 10. Hidden easter eggs

- **Speed-tinted vehicle name** — the name in the header changes color with speed (green ≥ 50, orange ≥ 100, red ≥ 120 km/h).
- **Speedometer test boost** — **triple-tap** the speedometer widget: it adds **+10 km/h** to the indicated speed, **holds for 500 ms** so you can read it, then falls back at **20 km/h per second**. Works in all three speedometer modes (dial, digital, animals) to test the display while stationary.
- **Retro 80s LCD theme** — long-press the version line at the bottom of Settings (*"v0.9.119 • Original by David Lam…"*) to toggle a phosphor-green CRT theme: scanlines, flicker, IBM VGA font, DSEG14 digits. Long-press again to return to normal.
- **Trip accel simulation** — tap the speed readout in the 0–100 mode (see §8).

---

## 11. Troubleshooting quick reference

| Symptom | Likely fix |
|---|---|
| No speed/GPS data | Grant Location access; run the setup wizard again |
| Now Playing empty | Grant Notification Access (Settings → Permissions) |
| Floating panel won't show over apps | Grant "Draw over other apps" |
| Panel never appears / stays hidden | Check panel toggle + Usage Access; it hides over sensitive apps by design |
| Spectrum stays idle | Grant Microphone (RECORD_AUDIO); start playing audio; enable the widget |
| Auto-switch profile never fires | Grant Location (SSID is redacted without it); check the SSID spelling |
| Volume controls do nothing | Run Volume Calibration to find the right MCU channel |
| Album art looks soft | Normal — the source app decides the resolution; the launcher already prefers full-res art URIs and renders with high-quality filtering |
| Something feels broken | Restart Launcher (Settings → Launcher → Recovery), then Export Debug Log if it persists |

---

## 12. Map search & navigation (free, online only)

The map widget has a **SEARCH** chip — it appears **only while the device is online** (offline it stays hidden, and the widget falls back to cached tiles + the OFFLINE badge).

- Tap **SEARCH** → type a place (e.g. "Kalamata" or a street/POI) → results appear live (up to 5). Tap a result: the map centers on it and drops a pin.
- A small bar appears bottom-left with the place name, **NAVIGATE** and ✕. Tap **NAVIGATE** to draw the driving route from your GPS position and show distance + ETA.
- Tap ✕ (or pick another result) to clear the pin/route. Tap the recenter button to return to follow mode.
- Everything uses **free OpenStreetMap services** (Nominatim for search, Valhalla for routing) — no Google billing involved, on either map provider. Search/routing need internet at the moment of use; tiles keep working from the OSM cache when offline.

## 13. Version history (recent)

- **0.9.119** — Security: moved the signing-keystore secret out of source into a git-ignored `keystore.properties`; added a private Google-detach runbook.
- **0.9.118** — TRACK updates now include a tappable Maps link with each position (same as the GPS reply).
- **0.9.117** — Driver Inbox polish: all quick replies show (wrap), allowed senders accept numeric IDs / @usernames / "@all", token & sender format examples, and a ✕ clear button on the Settings search field.
- **0.9.115** — Telegram "Driver Inbox": bot-based remote messages (setup page, allowed senders, look-back, auto-dismiss, receipts, quick replies, commands GPS/TRACK/CLEAR/STATUS/WARN, header indicator, test + log).
- **0.9.114** — Speedometer test boost tuned for real screens: triple-tap instead of 4 taps, and the boosted speed holds for 500 ms before dropping at 20 km/h/s.
- **0.9.113** — Hidden speedometer test mode: taps add +10 km/h that bleeds off at 20 km/h/s, in every speedometer mode.
- **0.9.112** — Speedometer "Animals" mode: 16-animal sprite (tree → peregrine falcon by speed range) with a small speed readout; selectable via the widget's long-press menu.
- **0.9.111** — Performance & safety hardening: sanitized search input, bounded route geometry, finite-coordinate guards, double-tap protection and defensive wrappers around all map/search/scroll-tracker code (no features removed).
- **0.9.110** — Section colour edges enlarged to full-height 7px strips on the left of each subcategory block (fixed to span the whole section).
- **0.9.108** — Settings navigation aids: colour-coded left-edge bar per section + sticky "current section" banner while scrolling.
- **0.9.107** — Map Control Size tuned to a gentle 30% enlargement (Large), down from 2×.
- **0.9.106** — Map Control Size setting (Normal / Large) for the map widget's chips, zoom buttons and search panel.
- **0.9.105** — Map widget search + navigation on free OSM services (Nominatim + Valhalla), online-only, offline auto-hide.
- **0.9.103** — Housekeeping build (widget registry cleanup).
- **0.9.102** — OSM map provider (osmdroid, offline tile cache) + OFFLINE badge, provider switch in Maps settings.
- **0.9.87** — Retro 80s LCD hidden theme (long-press version line), DSEG14 & IBM VGA fonts.
- **0.9.86** — Wi-Fi (hotspot SSID) automatic profile switching.
- **0.9.85** — Album-art rendering polish (exact-size decode, crossfade, popup filter quality).
- **0.9.84** — Unified app picker dialog (search + drawer filters) for sidebar, widget and floating-menu app choices.
- **0.9.83** — Profile rename, profile picker from the vehicle name.
