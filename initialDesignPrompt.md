# Implement "Avionics Mk II" watch face — Samsung Galaxy Watch (Wear OS)

You are implementing a custom watch face/app for a Samsung Galaxy Watch (Wear OS, round display, assume 450×450 px). The project is already scaffolded and blank. Build the complete face described below. Prefer Kotlin + Jetpack Compose for Wear OS unless the project indicates otherwise. All UI is drawn by us — no system watch-face template.

## 1. Design language

Aircraft-cockpit / avionics HUD: dark warm-black panel, amber phosphor readouts, thin ruled divider lines, stencil labels, glowing numerals. High contrast, angular, technical. No rounded pill buttons, no emoji, no icons — text labels only.

### Fonts (bundle in app)
- **Rajdhani** (weights 600, 700) — all numerals: time, chrono, timer, data values. Google Fonts, OFL.
- **Michroma** (regular) — all labels/stencil text, letter-spaced, uppercase. Google Fonts, OFL.

### Color tokens — normal (dark) mode
| Token | Value | Use |
|---|---|---|
| `bg` | vertical gradient `#14100A → #0B0805` | face background |
| `fg` | `#FFE9C4` | primary text, big time digits |
| `dim` | `#8A7550` | secondary labels |
| `line` | `#3A2E18` | hairline rules, button borders |
| `chip` | `#FFB000` @ 7% alpha | button/chip fills |
| `accent` | `#FFB000` | seconds, active states, brackets, T-countdown, arc |
| `warn` | `#FF4D5E` | battery < 20% |
| glow | numerals get soft glow: blur 18px @ accent 40% + blur 3px @ accent 67% |

### Color tokens — LIGHT (flashlight) mode
Whole palette swaps; **screen must not sleep** while active (`FLAG_KEEP_SCREEN_ON` / ambient disabled):
- bg `#FFFFFF`, fg `#B4B9BF`, dim `#CED3D8`, line `#E0E4E8`, chip `#F3F5F7`
- accent = accent blended 30% into white (≈ `#FFE7B3`), time digits `#A8ADB3`, no glow
- Everything stays ≥ 50% white so the face acts as a light. Show tiny centered label at top: `ALWAYS-ON · NO SLEEP` (Michroma 8px, tracking 3px, dim).

### Tunable constants (put in one settings/constants object — user-adjustable later)
`accent = #FFB000`, `timeFontPx = 110`, `dataFontPx = 34`, `dataAreaWidthPct = 100`, `metaFontPx = 16`, `metaMarginBottomPx = 5`.

## 2. Main face layout (450px round; content column padded 46px sides, 46px top, 108px bottom, vertically centered, 10px gaps)

Top → bottom:
1. **Battery** (centered, tap → battery sub-app): percentage only, e.g. `63%` — Michroma `metaFontPx`, flanked by 2px accent vertical bars (left+right borders, 10px horizontal padding). Turns `warn` red when < 20%.
2. **Date** (centered, 5px below = `metaMarginBottomPx`): `TUE · JUL 21` — Michroma `metaFontPx`, tracking 2px, fg. Block has `metaMarginBottomPx` bottom margin.
3. Hairline rule (1px, `line`, full column width).
4. **Time row** (centered): `HH:MM` 24h — Rajdhani 700, `timeFontPx`, line-height 0.82, fg with glow. **Critical: fixed-width glyph cells** — each digit occupies exactly `0.52em`, each `:`/`.` exactly `0.28em`, glyph centered in its cell, so the display NEVER shifts as digits change. To the right (baseline-aligned, 10px gap): seconds `:SS` in a bordered box (1px `line`), Rajdhani 600 at `0.3 × timeFontPx`, accent, box width locked to 1.7em, text centered.
5. Hairline rule.
6. **Data row** (width = `dataAreaWidthPct` of column, centered; two cells): 
   - Left, tap → steps sub-app: label `STEPS` (Michroma 14px, tracking 2px, dim) above value `8,432` (Rajdhani 700 `dataFontPx`, fg). Cell has 2px accent **left** border, 10px left padding, left-aligned.
   - Right, mirrored: `TEMP` / `78°`, 2px accent **right** border, right-aligned.
7. **Next-event chip** (same width, tap → calendar sub-app): bordered (1px `line`) + chip fill, 7×12px padding; left `NEXT · <EVENT NAME>` (Michroma 14px, dim), right `T-2H 37M` (Rajdhani 700 22px, accent). Shows the next calendar event within 24h; countdown format `xH yyM` (or `yyM` under 1h, `NOW` at 0). Single line, never wraps.

### Bottom utility buttons (conformal)
Row pinned to the bottom edge: 352px wide centered, 98px tall, 8px gaps, three equal buttons — **CHRON · LIGHT · TIMER** (Michroma 10px, tracking 2px). Geometry: flat tops, 1px `line` border with no bottom border; bottoms flare into the bezel and are clipped by the round display so they conform to the circle. Corner radii: left button `0 0 10px 90px`, middle `0 0 14px 14px`, right `0 0 90px 10px`. Labels sit near the top (10px padding) and are nudged 23px inboard on the two side buttons so the circle never clips them.
- Active states: CHRON border+label turn accent while chrono runs; LIGHT label becomes `LIGHT·ON` and turns accent while light mode on; TIMER turns accent **and pulses** (brightness 1 → 2.1 → 1, 1.4s ease-in-out loop) while a timer runs, with the remaining time `h:mm:ss` (Rajdhani 600 13px, accent) directly under the TIMER label.

## 3. Behaviors

### Clock
`HH:MM` (24h) big; seconds tick in the side box. Update ~5Hz when idle is fine; 60Hz while chrono runs.

### Chronometer (CHRON button)
- Repurposes the main time display — no page change. While engaged, digits show `MM:SS` big + `.hh` (hundredths) in the seconds box, digits in accent color instead of fg.
- Tap = start/stop (accumulates). Long-press = reset to zero and return display to clock. No lap function.
- While chrono is engaged, show helper line under the time: `STOPWATCH · HOLD BTN TO RESET` (9px Michroma, tracking 3px, accent).

### Long-press system (used by CHRON reset and timer exit)
- Threshold **550ms**, action fires **immediately at threshold** (not on release). Release before threshold = tap.
- Progress arc: accent stroke, 6px wide, round caps, radius ≈ 3px inside the display edge, sweeping the **top half** of the face — starts at 9 o'clock, grows clockwise across 12 to 3 o'clock, length = progress × half-circumference. Only rendered after the press has lasted **100ms** (taps never flash it), and only when the hold has meaning (e.g. no arc for CHRON hold when chrono is already zeroed/never started).

### LIGHT button
Toggles light mode (palette above) + keep-screen-on. Tap again to exit.

### TIMER button
- No active timer → preset page. Active timer → the running-timer page (timer is remembered; it keeps running wherever you navigate).
- **Preset page**: title `TIMER · MINUTES` (Michroma 10px, tracking 4px, dim); grid of 10 buttons — 1, 2, 3, 5, 10, 15, 20, 30, 45, 60 — 80×62px, Rajdhani 700 26px, 4 per row, 12px gaps; below, a large `‹ BACK` button (Michroma 14px, tracking 3px, 13×42px padding, 1px border + chip fill) returning to the face.
- **Running page**: label `TIMER` small at top; remaining time big (Rajdhani 700 ~123px, accent + glow, whole seconds, `M:SS`, width locked to 2.6em so it never shifts); under it `HOLD TIME TO EXIT` (8px Michroma dim); row of four adjust buttons `-5 · -1 · +1 · +5` (minutes, 74×58px, Rajdhani 700 25px, clamp so remaining never drops below 1s); large `‹ FACE` button below returns to the main face **without stopping the timer**.
- Long-press on the big remaining time = cancel timer and return to the preset page (uses the long-press arc).
- At 0: full-face accent flash (opacity 0.12 ↔ 0.55, 0.8s loop) with `TIME UP` + `TAP TO DISMISS`; appears over whatever screen is showing; tap dismisses to the main face. Add vibration on the watch.

### Data taps → sub-apps
Battery, STEPS, TEMP, and the NEXT chip each open the corresponding app (battery/settings, health/steps, weather, calendar) via launch intents. Hit targets ≥ 44px.

### Battery warning
When battery < 20%: face-wide pulsing red vignette — inset glow (~70px blur, 14px spread, `warn` @ 55% alpha) around the full circle, opacity 0.35 ↔ 0.95 @ 1.2s — visible on every screen, non-interactive. Battery readout turns `warn`.

## 4. Data sources
- Time/date: system clock. Battery: BatteryManager. Steps: Health Services / Samsung Health daily step count. Temp: weather provider of your choice (stub with 78° if no key). Calendar: next event within 24h via Wear calendar sync (name + start time).

## 5. Quality bar
- No layout shift anywhere as numbers tick (fixed-width glyph cells, width-locked boxes).
- 60fps while chrono/arc animates; drop to low-rate updates when idle; honor ambient mode (except in light mode, which disables sleep).
- All tap/hold targets ≥ 44px. Text never clipped by the round bezel — test at the sizes given.
- Keep the six tunable constants in one place, typed and documented.
