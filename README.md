# ShelfDeck

Frontend + emulation-orchestration engine for Android, inspired by the console-shelf launcher in the reference video.

## What it does
- **3D box-art shelf** per system (rotating boxes, reflections, swipe / D-pad / L1-R1), tinted per console.
- **Steam-style library** mode (search, grouped sidebar, hero, green PLAY, play time, last played). Toggle with *Select* or Settings.
- **Inspect viewer**: front, back, disc, manual with flip animation.
- **Details panel**, favourites, per-game art import (front/back/disc/manual).
- **Boot screen** per console ("Booting GameCube - Starting in Dolphin").
- **Engine** (`engine/EmulationEngine.kt`): per-system emulator profiles, installed-emulator detection,
  launch via `ACTION_VIEW` + granted content URI, play-time tracking, store link if missing.
- **Library scanner** (SAF folder picker): detects the system from extension + folder name (`GameCube/`, `Wii/`, `PSP/`...).
- Gamepad: D-pad, L1/R1, A, B, X, Y, Start, Select.

## Important limits
- It does **not** emulate CPUs/GPUs. It launches real emulator apps (Dolphin, PPSSPP, Azahar, melonDS, NetherSX2, Vita3K, Eden).
  Wii U and Xbox 360 have no usable Android emulator, so they are listed but cannot launch.
- Emulator package/activity names live in `data/Systems.kt` and may need adjusting per version/fork.
- No cover scraping: import your own art in the Details panel (generated placeholder covers are used otherwise).
- Not compiled or run yet (no Android SDK in the build environment).

## Build
Open the `ShelfDeck` folder in Android Studio (Ladybug or newer), let it sync (it creates the Gradle wrapper), run on a device.
Needs JDK 17, compileSdk 34.
