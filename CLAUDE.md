# NerdWatch

A Wear OS watch face for nerds who care about time and utility.

## North star

> **A dense, high-contrast dial that shows the time and the three things you
> actually want on your wrist, with the stopwatch always one tap away — never a
> menu.**

Confirmed 2026-07-21. Use this to decide what does *not* go in: if a feature adds
a menu level, or crowds the dial without earning its pixels, it fails the north
star.

The 2019 Tizen build (see `_ARCHIVE/`) had: compact date, chronograph/stopwatch,
step count, battery arc, flashlight, and a red "night vision" mode that engaged
when the ambient light sensor read dark. Those are the proven features to grow
back toward — not a wishlist.

## Platform constraint — read this before proposing an architecture

Verified empirically on 2026-07-21 by installing the same APK on two emulators:

| Platform | `set-watchface` | Result |
|---|---|---|
| Wear OS 5 (API 34) | `result=2` | `WFInfoResolver: Unsupported legacy watch face` |
| Wear OS 4 (API 33) | `result=1` | Renders correctly |

Google's rules:
- Wear OS 5+ devices **only** support **Watch Face Format** (WFF) — declarative
  XML, no executable code.
- Since **2026-01-14**, AndroidX/WSL "legacy" watch faces can no longer be
  installed from Google Play on any watch. Sideloading still works on Wear OS 4.

Consequence: a programmatic Kotlin watch face **cannot be the active face on a
Wear OS 5+ watch**. WFF cannot express a tap-driven stopwatch or sensor-driven
night vision. Any plan that wants both must split them — a WFF dial plus a
Kotlin app/tile for the interactive parts.

## Architecture — the hybrid (decided 2026-07-21)

Two separate bundles, because WFF forbids mixing:

| Module | What it is | Carries |
|---|---|---|
| `:watchface` | WFF, resource-only, `hasCode="false"` | The always-on dial: time, date, steps, battery, complications |
| `:app` | Kotlin / AndroidX | The interactive nerd utilities: stopwatch, flashlight, night vision, sensors |

The dial is what you see on your wrist; tapping it launches the app for anything
that needs real logic. This is the only shape where every archived feature
survives on hardware you can actually buy today.

The AndroidX watch face that `:app` held in Layer 1 has been **removed** — it ran
only on Wear OS 4, which the target Galaxy Watch 6 is well past. `:app` is now a
Compose for Wear OS app.

### The design: Avionics Mk II

`initialDesignPrompt.md` is the authoritative visual spec — aircraft-cockpit HUD,
amber phosphor on warm black, Rajdhani numerals and Michroma stencil labels.

Its entire "Behaviors" section (chronometer, 550ms long-press arc, light mode,
timer pages) is **impossible in WFF** — no state, no gestures, no navigation. So
the design is built as the Compose app, and the WFF dial will mirror only its
static layout with tap zones that launch the app. Read the mapping table in the
design discussion before assuming any element can live on the dial.

## Current state

- **Layer 1 (Blank Screen): DONE.** Verified on emulator, both modules build,
  unit tests green.
- **Layer 2 (First Breath): DONE for the dial.** The WFF face renders the real
  current time on **Wear OS 5 and Wear OS 6**. Verified by screenshot on both,
  not just by a green build.
- **Layer 3 (Grow by Observation): in progress.**
  - *Increment 1 DONE* — the Avionics face at rest renders on the 480x480 Wear OS
    6 emulator: battery, date, hairlines, glowing time with fixed-width cells,
    bordered seconds box, STEPS/TEMP cells, NEXT chip, conformal button bar.
    Live clock; other values still the design's reference data.
  - *Increment 2 DONE* — CHRON chronometer and the shared long-press system.
    Tap start/stop repurposes the time display to accent `MM:SS` + `.hh`; the
    helper line shows; a >550ms hold on CHRON resets to the clock; the progress
    arc sweeps the top half. All verified on the emulator by driving `adb input`
    and screenshotting each state (run / arc mid-sweep / reset).
  - *Observed for Layer 4 polish:* while the chrono is engaged, the extra helper
    line compresses the fixed-height column and squeezes the NEXT chip so its
    `NEXT · <event>` label clips. Expected from the spec (the line must go
    somewhere) but worth revisiting.
  - *Next:* LIGHT, then TIMER, then real data sources, then the WFF mirror dial.

### Chronometer / long-press design

- Pure logic is isolated and unit-tested: `chrono/Chronometer.kt` (immutable,
  computes elapsed against a caller-supplied monotonic `nowMs`) and
  `chrono/ChronoFormatter.kt`. No Android types, so they run as plain JVM tests.
- `ui/LongPressGesture.kt` is the shared 550ms system (tap vs fire-at-threshold,
  arc after 100ms). The frame-paced progress loop runs on a
  `rememberCoroutineScope()`, **not** the pointer gesture: a held finger emits no
  pointer events to tick against, and `PointerInputScope` is not a
  `CoroutineScope`. Passing the scope in is deliberate, not incidental.
- Timing uses `SystemClock.uptimeMillis()` (monotonic), never wall-clock — the
  clock changing must not corrupt a running stopwatch.

### Compose layout gotchas paid for already

- **`Trim.Both` will not shrink a line below the font's natural metrics.**
  Rajdhani's ascent+descent is ~1.28em, so the time row measured **150px instead
  of 96px** and squeezed the next-event chip to *zero height*. Fixed-width glyph
  cells therefore need an explicit `.height()` plus
  `wrapContentHeight(unbounded = true)` on the text — setting `lineHeight` alone
  is not enough.
- **`Modifier.blur` defaults to a bounded blur** and paints a hard-edged
  rectangle behind the numerals. The glow needs
  `BlurredEdgeTreatment.Unbounded`.
- **Only lock cell widths for digits and `:` / `.`** — letters (`T-2H 37M`, the
  `°`) need their natural advance or they collide.
- When a layout looks wrong, **measure it** with `Modifier.onSizeChanged` and
  logcat. Two speculative fixes failed here; the measurement found it instantly.

## Target hardware

**Galaxy Watch 6**, arriving ~2026-07-28. It runs **Wear OS 6** (One UI 8 Watch,
rolled out to the Watch 6 in the US around December 2025) — *not* the Wear OS 4
it originally shipped with.

Consequences:
- WFF is mandatory on this device. The `:app` AndroidX face will never be the
  active face on it. `NerdWatch_Wear6` is therefore the **primary** test AVD.
- `format.version="2"` is still correct — WFF is backward compatible, and a lower
  version reaches more watches. Only raise it if a feature demands it.
- Until the watch lands, everything is emulator-only. Sensor-dependent work
  (ambient light for night vision, real step counts, battery) **cannot be
  validated** and should not be called done.

## WFF gotchas learned the hard way

- **`:` is the ternary operator in WFF expressions.** `[HOUR_0_23]:[MINUTE_Z]`
  does not mean "hour colon minute" — it parses as a broken conditional and
  silently renders the literal text `false`. Put separators in the `Template`
  literal and use one `<Parameter>` per value.
- WFF failures are silent: no logcat error, just wrong pixels. **Always
  screenshot after changing `watchface.xml`.**
- Activation uses a *string extra*, not a component:
  `--es watchFaceId com.nerdwatch.dial`. The AndroidX face uses
  `--ecn component <pkg>/<class>` instead. They are not interchangeable.
- Set `format.version` to the lowest that supports the features used (currently
  `2`, for Wear OS 5). Higher numbers cut off older watches.

## Practices

- **Validate every change with passing unit tests.** Run `.\gradlew
  testDebugUnitTest` before moving on. Never stack up untested changes.
- **Run it, then look at it.** After a change that affects rendering, install to
  the emulator and screenshot. A passing test is not a working watch face.
- **Object-oriented, one class per file.** Class name matches file name.
- **Keep pure logic free of `android.*` imports** so it stays JVM-unit-testable.
  `NerdWatchPalette` is the pattern: plain ARGB ints, no `android.graphics.Color`
  (which is a throwing stub under unit tests).
- **Solve the problem in front of you.** No speculative features.

## Commands

```powershell
# Build + test
.\gradlew testDebugUnitTest assembleDebug

# Emulators (Wear OS 4 is the only one that will run the AndroidX face)
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd NerdWatch_Wear4 -no-snapshot-save
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd NerdWatch_Wear    # Wear OS 5

# The WFF dial on Wear OS 5 (emulator-5554) — the primary loop
adb -s emulator-5554 install -r watchface\build\outputs\apk\debug\watchface-debug.apk
adb -s emulator-5554 shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE `
  --es operation set-watchface --es watchFaceId com.nerdwatch.dial

# The AndroidX face on Wear OS 4 (emulator-5556) — legacy, Wear OS 4 only
adb -s emulator-5556 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5556 shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE `
  --es operation set-watchface --ecn component com.nerdwatch/com.nerdwatch.NerdWatchFaceService

# Look at it — mandatory after any watchface.xml change
adb -s emulator-5554 shell screencap -p /sdcard/shot.png; adb -s emulator-5554 pull /sdcard/shot.png
```

## Toolchain

- JDK 17 (Temurin), Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21
- `compileSdk`/`targetSdk` 34, `minSdk` 30
- SDK at `%LOCALAPPDATA%\Android\Sdk` (`ANDROID_HOME` is set at user scope)
- AVDs, all `wearos_large_round` 454x454:
  - `NerdWatch_Wear6` — Wear OS 6, **matches the real Galaxy Watch 6. Use this.**
  - `NerdWatch_Wear` — Wear OS 5
  - `NerdWatch_Wear4` — Wear OS 4, the only one that runs the AndroidX face

## Useful MCPs

- **playwright** — not useful for the Kotlin build; was relevant only to the
  abandoned browser-first plan.
- **talisman** — progress reminders. Was unreachable on 2026-07-21.
