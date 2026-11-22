# OpenLauncher Custom — User Guide & Reference

**Version covered: 0.9.179 (versionCode 179)**

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
- **Four pages (2×2 grid)** — swipe left/right to move within a row (1↔2, 3↔4), up/down to move within a column (1↔3, 2↔4); no diagonal jumps. Tap the 2×2 page-dot grid to jump directly. The last-used page is remembered and restored on launch. Each page has its own widget layout **and** its own per-page widget settings (shortcuts, clock style, spectrum, weather forecast, etc.).
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
| Weather | Current conditions + multi-day forecast (wttr.in or Open-Meteo); optional offline **fallback gallery** (image slideshow from a folder) |
| Now Playing | Track/artist, album art background, play/pause/next/prev, radio deck |
| Telemetry (Compass) | Compass rose, heading, GPS status |
| Volume | Volume up / down / mute |
| Quick Actions | Toggles: Bluetooth, Wi-Fi, blackout, mute, system settings, day/night |
| Altimeter | Roll, pitch & altitude (with zero calibration) |
| Speedometer | GPS speed — three modes via long-press: **Dial track** (analog gauge), **Digital only** (big number), **Animals** (animal for your speed range from the sprite sheet — tree at 0, tortoise → peregrine falcon up to 141+ km/h — with a small speed readout at the bottom) |
| Vitals | Head-unit health (CPU/RAM/temp etc.), configurable elements |
| Trip Tracker | Trip distance, drive/idle time, average speed + 0–100 accel test (see §8) |
| Soundboard | Custom sound pads |
| App Shortcuts | Quick app launch pad (1–4 shortcuts; count, apps and icon size are per-page) |
| OBD2 | Live engine & sensor telemetry from a Bluetooth ELM327 adapter |
| Network Speed | Upload/download speed |
| Spectrum | Live audio spectrum visualizer (with Automatic Gain Control for stable levels) |
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
| **Home Pages** | **Auto-Switch Pages** by driving state (column-based: right = driving pages 2 & 4, left = parked 1 & 3, row preserved), Driving Column (LEFT/RIGHT), Switch Speed (km/h), Idle Time |
| **Display Animation** | Car image beside the vehicle name — drives across when moving, rests when stopped; choose your own car image |

### Widgets

> **Widget settings live in each widget's long-press context menu** — the old per-widget Settings sections (Weather / Map / Spectrum / Now Playing / Widget Content) were removed. Long-press a widget to open its menu: Weather (provider, GPS/custom location, forecast days, hide-forecast, icons, **fallback gallery**), Map (provider, map type, traffic, control size), Spectrum (bars, style, mirror, color, gain, AGC, rise/fall/contrast, fallback image), Now Playing (radio app, artwork size/dim, edge blend), App Shortcuts (count, icon size), Soundboard, OBD (adapter, metrics, fallback image), etc.

### Vehicle & OBD

| Section | Options |
|---|---|
| **Vehicle & OBD** | Vehicle Name (tappable on home = profile picker), Unit System (metric/imperial), **OBD2 Bluetooth Adapter** (pair an ELM327), OBD fallback image |
| **GPS & Calibration** | Reset A-GPS data, Magnetometer Sweep (parking-lot calibration), Compass Heading Offset (−180°…+180°) |

### Launcher

| Section | Options |
|---|---|
| **Profiles** | Active profile (4 buttons), Rename Profile, **Auto-Switch by Wi-Fi** (see §7); each profile boots with its own default appearance (accent/font/light-dark) |
| **Sound on Tap** | Enable + sound type (tick / beep) |
| **Gestures** | Two-Finger Tap → app, Three-Finger Tap → app (hidden by default — tap the **Sound on Tap** header 5× to reveal) |
| **Telegram Messages** | **Driver Inbox** — remote messages from a Telegram bot (see §9a): token, allowed senders, look-back window, auto-dismiss, read receipts, quick replies, command toggles, header indicator, test + log |
| **Startup** | Auto-Launch App + delay (1–30 s) |
| **Recovery** | Restart Launcher |
| **Backup & Restore** | Export settings to JSON file, Restore from JSON |
| **Permissions** | Status + shortcuts for: Default Launcher, Notification Access, Location, Draw Over Other Apps, Microphone, Usage Access, Accessibility (Back key), **Modify System Settings** (volume) |
| **Maintenance** | **System Status** (diagnostics screen incl. a full volume/audio report), **Log Collector** (opt-in), **Export Debug Log**, **Volume Calibration**, Re-run Setup Wizard |
| **About** | Link to the fork (GitHub) |

---

## 5. Media, radio & OBD

- **Now Playing** mirrors the active Android media session (Spotify, Radio apps, local players…) with playback controls and album-art background. Tap the track title to open the source app.
- **Radio deck**: if your unit exposes a hardware radio (auto-detected, e.g. szchoiceway/Microntek MCU), the deck mirrors FM/AM — band chips, seek, presets, frequency display. A "station change" glass popup can be enabled from the Now Playing widget's long-press menu. Otherwise you can assign a radio app, or leave it on AUTO-DETECT.
- **OBD2**: pair an ELM327 Bluetooth adapter (Vehicle & OBD → OBD2 Bluetooth Adapter) to get live engine sensors in the OBD widget.

---

## 6. Floating quick-controls panel

Enable in **Dashboard → Floating Panel → Floating Quick Controls**.

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

**Export / Import a profile** (Settings → Launcher → Profiles): pick a **slot**, then *Export Profile* writes that driver's layout/colors/options to a JSON file; *Import Profile* overwrites the chosen slot from a file. This is how you build a profile on an emulator and bring it onto the head unit — hardware/shared settings (OBD, radio, volume, CarPlay/AA/PiP, names) are **never** overwritten by an import.

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

- **Normal messages** pop up as a top banner for the *Dismiss After* time (default 15 s), with **quick replies** and a ✕. Messages are **queued** — each one is shown and acknowledged ("✅ Read") in order, with a "+N" badge when more are waiting. Dismissal sends a **"✅ Read"** receipt back to the sender.
- **Commands** (case-insensitive): `GPS` → location + Maps link · `TRACK 5` → position/speed/heading every minute for 5 minutes, each with a Maps link · `CLEAR` → clears pinned messages + stops tracking · `STATUS` → speed, GPS, profile, battery, uptime, network path (Wi-Fi/mobile) and allowed senders · `WARN <text>` → a pinned alert that cannot be dismissed for 30 s · `?` / `help` → lists the available commands.
- **Priority markers** — add `$1`, `$2` or `$3` to any message: it's trimmed from the shown text and plays one of three escalating alert sounds on the unit.
- **Look-back window** ignores messages older than the selected age (1–12 h) so stale backlog doesn't flood the screen after being offline.
- A small **header dot** shows while the bot is connected (independent toggle).
- Security: the bot **only answers allowed senders** (numeric IDs, `@usernames`, or `@all` for anyone — `@all` is insecure); the token never leaves the device.

---

## 10. Hidden easter eggs

- **Speed-tinted vehicle name** — the name in the header changes color with speed (green ≥ 50, orange ≥ 100, red ≥ 120 km/h).
- **Speedometer test boost** — **triple-tap** the speedometer widget: it adds **+10 km/h** to the indicated speed, **holds for 500 ms** so you can read it, then falls back at **20 km/h per second**. Works in all three speedometer modes (dial, digital, animals) to test the display while stationary.
- **Retro 80s LCD theme** — long-press the version line at the bottom of Settings (*"v0.9.179 • Original by David Lam…"*) to toggle a phosphor-green CRT theme: scanlines, flicker, IBM VGA font, DSEG14 digits. Long-press again to return to normal.
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

- **0.9.179** — Weather animated icon now pauses when the app isn't in the foreground (screen off / another app on top) to cut idle CPU, and resumes automatically. System Status report now includes a full VOLUME/AUDIO section (resolved protocol, level/max, mute, `canWriteSettings`, `mcu_system_volume`, `mcu_mute_state`, `mcu_volume_max/min`, `av_volume`, `getParameters`, vendor packages) for G5 volume debugging.
- **0.9.178** — Stability: moved the weather fallback-gallery folder listing off the main thread (it was doing a synchronous storage query during composition, which could jank/ANR with large folders). Stress-tested on emulator (80+ swipes, relaunch) with no crash or launcher ANR.
- **0.9.177** — Weather: removed the false fallback-to-gallery that happened while online. The widget was treating "weather data older than 5 minutes" as offline, but the fetch interval is ~20 min, so it kept dropping to the gallery even with a live connection. Now offline = no data / failed fetch / device actually offline only.
- **0.9.176** — Spectrum: added Automatic Gain Control so the bars follow the audio level across quiet and loud sources (replaces the old fixed 3.5× boost). New "Auto Gain (AGC)" toggle + "AGC Target Level" slider in the Spectrum settings, per page.
- **0.9.175** — Fixed the Wi-Fi status icon: it now also checks whether the Wi-Fi radio is actually enabled, so it correctly shows "off" when Wi-Fi (and mobile data) are switched off, instead of lingering as "connected" (an emulator/head-unit quirk where the active network still reports a Wi-Fi transport).
- **0.9.174** — Weather recovery: while offline the app now retries the weather fetch every minute (instead of only every 20 min), so it switches back from the fallback gallery/offline placeholder to live weather within ~1 minute of the connection returning, even when the network callback misses the event.
- **0.9.173** — Spectrum fallback image now uses zoom-to-fill (crop) instead of letterboxed fit, so it fills the whole widget tile without stretching.
- **0.9.172** — Weather fallback gallery: when weather is offline (or no update for >5 min) the widget can now show a slideshow from a user-picked folder instead of the plain offline placeholder. New "Fallback Gallery" entry in the weather long-press menu with: enable, choose folder (SAF + persistable permission), time-per-image, transition duration, and transition effect (Crossfade/Slide/Zoom/Fade). If disabled or no images, the normal offline placeholder remains.
- **0.9.171** — Reserved the top ~15% of the home screen from the vertical "page-down" swipe, so reaching up for the Android notification shade/taskbar no longer flips pages. Horizontal and other vertical swipes are unchanged.
- **0.9.170** — Spectrum is now fully per-page (Rise Speed, Fall-off Speed and Peak/Low Contrast moved into the per-page content, alongside the already per-page bars/style/mirror/color/gain/fallback image). The weather "Hide Forecast Days" toggle is now per-page too. The spectrum engine reconfigures on page change.
- **0.9.169** — Verified per-page widget settings across all 4 pages. App-Shortcut icon size is now per-page too (was global), so each page's shortcuts, icon count AND icon size are fully independent. All other per-page widgets (clock, speedometer, vitals, OBD metrics, quick actions, soundboard, spectrum) already store their settings per page.
- **0.9.168** — Home pages expanded to a 2×2 grid: page 1 (top-left), 2 (top-right), 3 (bottom-left), 4 (bottom-right). Swipe left/right to move within a row, up/down to move within a column (no diagonals). Auto-switch by speed now switches columns (right = driving pages 2 & 4, left = parked 1 & 3) while preserving the row, with the same manual-override behavior.
- **0.9.167** — Driver profiles now have distinct default appearances so switching profiles visibly changes the look. Defaults pulled from `ref.json`: profile 1 (DRIVER 1) = purple accent + Cal Sans font + light mode; DRIVER 2 = teal + Commissioner + dark; DRIVER 3 = green + Fira + light; DRIVER 4 = amber + Play + dark. Widgets and screen layouts are untouched.
- **0.9.166** — Now Playing text/controls now always follow the tile background (the album-art "always white" override was removed), so a whitish tile produces dark text/controls even with artwork/logo present. The artwork veil is now adaptive too (darkens on dark tiles, lightens on light tiles) to keep text readable both ways.
- **0.9.165** — Settings → Launcher reordered to: Profiles, Sound on Tap, Gestures, Telegram Messages, Startup, Recovery, Backup & Restore, Permissions, Maintenance, About (renamed from Updates). "Gestures" is now hidden by default — tap the "Sound on Tap" header 5× to reveal it. Updates/Telegram/Maintenance are now correctly confined to the Launcher category.
- **0.9.164** — Logging is now opt-in: added a "Log Collector" toggle in Settings → Maintenance (off by default, so the app no longer writes log lines during normal use — zero I/O overhead). Enable it, reproduce the issue, then use "Export Debug Log".
- **0.9.163** — Three widget tweaks: (1) Now Playing text/controls (title, artist, RDS/radio info, time, prev/next) now derive their color from the tile's background so they're never white-on-white; (2) Spectrum "Response Curve" renamed to "Peak / Low Contrast" to make the peak-vs-low gap control obvious (Gain is overall sensitivity only); (3) App Shortcuts widget gained Small/Normal/Large icon-size options in its long-press menu.
- **0.9.162** — Now Playing PLAYER/RADIO tabs: rebuilt the color logic so the selected tab uses the accent color (with auto black/white label), and the inactive tab uses a neutral chip + label derived from the tile's own text color. The inactive chip no longer collapses to a full-black box with unreadable text on any theme; also made the radio deck chips follow the tile's light/dark content color.
- **0.9.161** — Now Playing artwork edge blend now detects the tile's own background color (light/dark/custom) and fades into it instead of always black; added an "Artwork Edge Blend" slider (0–100%, 0 = off) in the Now Playing settings for full control.
- **0.9.160** — Now Playing widget: album-art edges now fade softly to transparent (radial mask), so the artwork blends into the widget card instead of sitting as a hard rectangle.
- **0.9.159** — Fixed the "Hide Forecast Days" weather toggle: the new setting wasn't being written to/read from DataStore, so the change never stuck. It now persists and correctly hides/shows the forecast strip.
- **0.9.158** — Settings top block reorganized: the search field now sits as the third slot in the second category row (same width as a category button), so both category rows read 3-across; Open System Settings is now alone on its own row at half width.
- **0.9.157** — Weather widget: added a "Hide/Show Forecast Days" option in the long-press menu. Hiding the forecast removes the multi-day strip and enlarges the current conditions (bigger icon, temperature, location and stats) to fill the freed area, across all tile sizes (wide strip, tall, medium, large).
- **0.9.156** — Settings cleanup: removed the redundant "Widget Content" section (app-shortcut and soundboard counts are set from each widget's context menu), moved "Floating Panel" into the Dashboard category, removed the now-empty "Widgets" category, and stripped all dead code left behind by the widget-settings migration (no ghost elements).
- **0.9.155** — Map widget: added a ⚙ gear button (top-right, under the zoom controls) that opens the widget menu, because the map's own pan/zoom gestures swallow the long-press that normally opens it. The gear gives access to MAP SETTINGS, resize, remove, etc.
- **0.9.154** — Finished unifying widget settings under each widget's long-press context menu. Weather, Map, Spectrum, Now Playing and the OBD fallback image are now configured from the context menu (new unified settings dialogs), and those per-widget sections were removed from Settings. Restore point saved at `.restorepoint_pre_spectrum/`.
- **0.9.150** — Started unifying widget settings under the long-press context menu: removed the redundant ⚙ config buttons from the Quick-Actions and Vitals widget faces/edit mode (their settings are already in the context menu). Restore point saved at `.restorepoint_pre_unify/`.
- **0.9.149** — Map "Large" controls slightly smaller; OBD adapter "Choose" moved into the long-press context menu (off the widget face); App Library search got a clear ✕ button.
- **0.9.148** — OBD2 widget redesigned: pick which live values to show (long-press → Configure Metrics) and the selected gauges auto-arrange and scale to fill any tile size (bigger icons/values when there's room). All 8 values (RPM, speed, coolant, throttle, load, fuel, intake, battery) available.
- **0.9.147** — Altimeter redesigned to a clean altitude-only gauge (removed the meaningless roll/pitch "vehicle attitude" + ZERO); every text field now closes the keyboard via its Done key; weather offline/loading text no longer wraps vertically at large text scale.
- **0.9.146** — Text/UI scale sliders now go up to 200%; settings search dismisses the keyboard (Done / clear); map no longer flies across the screen on bad GPS fixes (jump guard, Google + OSM).
- **0.9.145** — NWD volume: added the `WRITE_SETTINGS` permission and a "Modify System Settings" grant in Settings → Permissions (required so the volume widget can actually *write* `mcu_system_volume`, not just read it).
- **0.9.144** — Added volume support for NWD / Qualcomm (Snapdragon) head units: the real channel is `Settings.System mcu_system_volume` (+ `mcu_mute_state`), now auto-detected when `com.nwd.*` is present, and the widget stays in sync with the physical knob.
- **0.9.143** — Startup smoothing: the first weather fetch is now deferred a couple of seconds after launch (shows "Connecting…" instead of flashing OFFLINE), so the heavier weather re-composition doesn't fight the first frame.
- **0.9.142** — Performance: split the giant `readSettings` loader into 8 small helpers (was a single ~14 MB JIT method at startup). No behavior change.
- **0.9.141** — Performance: split the giant HomeScreen composable into `HomeWidgetGrid` + `WidgetContent` to reduce runtime JIT method size on low-power units (no visual change).
- **0.9.139** — Performance/correctness: coalesced the startup weather fetch (no more redundant "fetch cancelled" churn at launch) and fixed the weather-fetch cancellation so a superseded request no longer marks the weather offline.
- **0.9.137** — Reworked the Volume widget layout to be flexible at every size: the volume bar now sits beside the number on the top level, and the controls are a single [DOWN][MUTE][UP] row with mute in the middle — so short 2×1 tiles no longer clip the buttons.
- **0.9.136** — Hid the experimental Light Glare option behind an easter egg: tap the "TILES" section header 5 times in Settings to reveal the glare toggle + strength (kept in the code but out of normal sight).
- **0.9.135** — Reworked the widget glare into a single "passing glint" light source: one diagonal band sweeps across the whole widget area (masked to the tiles only) at a random 25–60° angle, then hides for a few seconds. Fixes the old per-tile proportional-glare artifacts.
- **0.9.134** — Rewrote the constellation particle effect from scratch as the classic "particle network": dots drift in straight lines at size-dependent speeds and link with distance-faded lines. Removed gravity entirely (it caused collapse/swarm) — the field is now guaranteed calm, uniform and non-clumping.
- **0.9.133** — Rebalanced the constellation particle physics (superseded by 0.9.134).
- **0.9.132** — New "Light Glare" option (Settings → Tiles): a soft diagonal light sweep that periodically passes across the widget tiles, with a strength slider. Subtle by design — adapts to light/dark tiles and never intercepts touches.
- **0.9.131** — Reworked the constellation gravity wells into a one-shot "slingshot": a passing dot is grabbed hard and released immediately (cooldown), so the well only bends each dot's direction instead of swarming/orbiting them around it.
- **0.9.130** — Fixed the constellation particle effect freezing after a few seconds (gravity damping collapsed the dots onto their attractors); dots now keep drifting and orbit the gravity wells instead of stopping.
- **0.9.129** — Particle overhaul: snow now has two rarer large flake sizes that fall slower with more drift (and 12-point arms); rain was redesigned with three depth layers + a consistent wind slant + bottom splash ripples; stars gained rare large breathing/glowing stars and occasional meteorites with fading tails; constellation gained rare slow "gravity wells" that gently attract nearby dots.
- **0.9.128** — Expanded the settings search dictionary: added the missing "Maps" section keywords plus many more synonyms/aliases across every section, so menus are easier to find by common terms (e.g. "obd2", "equalizer", "google", "traffic", "default launcher", "font size", "wifi").
- **0.9.127** — Driver selection now follows Text/UI Scale: the export/import profile dropdown (trigger + menu items) and the Active Profile segmented selector scale with the same size rules as the rest of the app.
- **0.9.126** — Text Scale & UI Scale now apply to every dialog (app picker, font picker, widget library/resize/color, profile picker, quick-actions & vitals config, soundboard pad, OBD adapter, floating-slot, shortcut/icon menus and more). Long lists and preset grids wrap/scroll so nothing overflows at large scales.
- **0.9.125** — (internal) color-picker dialog scaled and rewrapped into a scrollable preset grid.
- **0.9.124** — Profile export/import redesigned into a single row: profile dropdown + EXPORT + IMPORT.
- **0.9.123** — Restyled confirmation dialogs (reset / restore / profile-import) to match the launcher's native dialog look instead of the default Android alert style.
- **0.9.122** — Per-profile export/import: pick one of the 4 slots, export its layout/colors/options to JSON, or import/overwrite a slot (hardware/shared settings preserved).
- **0.9.121** — Fixed Telegram message queue: rapid messages are now queued and each acknowledged in order (previously a single slot dropped + never-acked earlier messages; auto-dismiss also paused while a WARN is pinned).
- **0.9.120** — Telegram: `$1`/`$2`/`$3` priority markers (trimmed + 3 escalating alert sounds), `?`/`help` command list, and STATUS now reports network path + allowed senders.
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
