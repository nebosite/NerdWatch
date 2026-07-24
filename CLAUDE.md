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
- **Phase B (WFF mirror dial): first pass DONE.** The always-on `:watchface`
  dial now mirrors the Avionics face's top half — battery, `DOW · MON DD` date,
  big time, and steps — in the amber-on-warm-black palette, and a `Launch`
  element on the time opens the full Kotlin app (verified: tapping the dial's
  time brought `com.nerdwatch/.MainActivity` to the foreground). That closes the
  hybrid loop the architecture was built around.
  - *WFF data tags, verified by on-emulator probe* (unknown tags render blank
    silently, so each was checked): `[BATTERY_PERCENT]`, `[STEP_COUNT]`,
    `[DAY_OF_WEEK_S]` (→"Thu"), `[MONTH_S]` (→"Jul"), `[DAY]` (→"23"). Dead ends
    that rendered blank: `[DAY_1_31]`, `[DAY_0_31]`, `[DAY_OF_MONTH]`.
  - Literal `%` in a `Template` needs `%%`. Seeing the dial requires leaving the
    app (`KEYCODE_BACK`) so the watch face shows, not the running activity.
  - *Follow-ups for the real watch:* bundle the Rajdhani font into the dial
    (currently `SYNC_TO_DEVICE`), add TEMP + next-event (needs a complication or
    weather source), and consider an ambient-mode variant.

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
  - *Tweaks (per user):* the `STOPWATCH · HOLD BTN TO RESET` helper line was
    removed entirely — no extra text on the face in either mode. Removing it also
    resolved the chip-squeeze that engaging the chrono used to cause. The seconds
    box is now baseline-aligned to the main time: `FixedWidthNumerals` takes a
    `cellAlignment` (bottom-anchored here) and the box is lifted by
    `SECONDS_BASELINE_NUDGE` (7.5 design px) to close the descent gap between the
    two font sizes. Verified by pixel-measuring both ink bottoms to delta 0.
  - *Increment 3 DONE* — LIGHT (flashlight) mode. Tapping LIGHT swaps the whole
    palette to `AvionicsPalette.LIGHT` and holds the screen awake
    (`view.keepScreenOn`, cleared on toggle-off — verified via `dumpsys window`
    showing `fl=KEEP_SCREEN_ON` appear and disappear). Per the user's "don't
    change the physical layout" rule, the spec's `ALWAYS-ON · NO SLEEP` top label
    is **deliberately omitted** — it would add structure to the face.
  - *Increment 4 DONE* — TIMER, on its own screens so the face never moves.
    Preset grid (1..60) → running screen (big glowing M:SS, ±5/±1 adjusters
    clamped to ≥1s, HOLD-time-to-cancel via the shared arc, ‹FACE that leaves it
    running). The main-face TIMER button pulses and shows `H:MM:SS` remaining in
    the empty space *under* its label (a Column keeps the label pinned, so no
    shift). At zero: full-face accent flash (`TIME UP` / `TAP TO DISMISS`) with a
    one-shot vibration, tap dismisses to the face. All paths walked on the
    emulator. Pure logic in `timer/CountdownTimer.kt` + `timer/TimerFormatter.kt`
    (10 tests). Timing is monotonic `uptimeMillis`, like the chrono.
  - *Increment 5 DONE (partial by design)* — real data + sub-app launches.
    Battery is live from `BatteryManager` (verified: `adb ... battery set level 42`
    showed 42% on the face). Tapping battery / steps / temp / next opens the
    corresponding app via `SubAppLauncher` (verified: battery → Wear settings).
    Steps read the hardware step counter with a graceful fallback — **the
    emulator has no such sensor, so steps still show the placeholder and can only
    be validated on the watch.** Temp stays the design's `78°` stub (spec allows
    it). Calendar *data* is deferred until a paired account exists; only its tap
    (open calendar) is wired. Taps use a footprint-free `tapGesture`, so the face
    layout is unchanged; the spec's ≥44px touch targets are not enforced where
    that would enlarge an element (the no-layout-shift rule wins).
  - *Increment 6 DONE* — low-battery warning. Below 20% a red glow hugs the
    circle's edge and pulses 0.35↔0.95 over every screen, and the battery readout
    turns `warn`. The vignette is a `Canvas` with no pointer modifier, so it never
    intercepts touches. Verified via `adb ... battery set level 15`.
  - *Next:* the WFF mirror dial (Phase B).

### Deferred until the Galaxy Watch 6 arrives (~2026-07-28)

Two data sources cannot be meaningfully verified on the emulator and are
best-effort with fallbacks until then: **steps** (needs a real step counter /
Health Services daily aggregation) and **the next calendar event** (needs a
synced calendar account). Don't mark either "done" from an emulator screenshot.

### Hard constraint (user, 2026-07-23)

**Never change the physical layout of the main face.** Colors, glow, active
states, and overlays are fine; anything that moves or adds on-face structure is
not. Extra *screens* are allowed (e.g. the timer's pages), but the face itself
must not shift. This is why the chrono helper line and the LIGHT-mode label are
both absent despite being in the design spec.

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
