# Deploying to a real Galaxy Watch 6

Verified end-to-end on 2026-07-30 against a **Galaxy Watch 6 Classic**
(`SM_R915U`, model `heartul`, Wear OS 6 / One UI Watch). The watch has **no USB
data** (it charges over pogo pins), so everything goes over **Wi-Fi wireless
debugging**. The PC and the watch must be on the **same Wi-Fi network**.

## The two bundles

| APK | Package | Role |
|---|---|---|
| `watchface-debug.apk` | `com.nerdwatch.dial` | The always-on **WFF dial** (must be WFF on Wear OS 6) |
| `app-debug.apk` | `com.nerdwatch` | The full **Avionics app** — moon, weather, Kp, alarms, chrono/timer/flashlight, red-only. Launched by tapping the dial's time, or from the app list. |

On Wear OS 6 the active watch face **must** be WFF, so the rich interactive face
can only run as an app. The WFF dial is the always-on mirror; tapping it launches
the app. This is the hybrid architecture — see `CLAUDE.md`.

## One-time: enable debugging on the watch

1. **Settings → About watch → Software** → tap **Software version** 7×
   ("Developer mode turned on").
2. **Settings → Developer options** → enable **ADB debugging** and **Wireless
   debugging** (Debug over Wi-Fi).

## One-time (per session): pair + connect over Wi-Fi

On the watch: **Developer options → Wireless debugging → Pair new device** shows a
6-digit code and an address like `192.168.86.24:37xxx`. Keep that screen awake —
it times out fast.

```powershell
adb pair 192.168.86.24:37xxx        # enter the 6-digit code when prompted
```

After pairing, the **main** Wireless debugging screen shows a **different** port
(the connect port, e.g. `192.168.86.24:43659`). Connect to that:

```powershell
adb connect 192.168.86.24:43659
adb devices -l                      # the watch (model SM_R915U) should be listed
```

The pairing survives reboots; the **connect port changes** on reboot / timeout, so
`adb connect` (and sometimes re-pair) is the only step you repeat.

## Build + install

```powershell
# Build both bundles
.\gradlew :app:assembleDebug :watchface:assembleDebug

# Target the watch explicitly ($W) — a running emulator must not catch the install
$W = "192.168.86.24:43659"          # the watch serial from `adb devices`
adb -s $W install -r "watchface\build\outputs\apk\debug\watchface-debug.apk"
adb -s $W install -r "app\build\outputs\apk\debug\app-debug.apk"
```

## Activate the WFF dial

Sideloaded WFF faces do **not** appear in Samsung's face carousel until the watch
**reboots**. Either reboot to pick it normally (long-press face → NerdWatch), or
set it active directly with the debug broadcast (activation uses a **string
extra**, not a component):

```powershell
adb -s $W shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE `
  --es operation set-watchface --es watchFaceId com.nerdwatch.dial
# success looks like: result=1, data="Favorite Id=[..] Runtime=[2]"  (Runtime 2 = WFF)
```

## Launch the app + grant permissions

```powershell
adb -s $W shell am start -n com.nerdwatch/.MainActivity
```

First launch prompts for permissions. Grant on the wrist, or skip the dialogs:

```powershell
adb -s $W shell pm grant com.nerdwatch android.permission.ACCESS_FINE_LOCATION
adb -s $W shell pm grant com.nerdwatch android.permission.ACCESS_COARSE_LOCATION
adb -s $W shell pm grant com.nerdwatch android.permission.ACTIVITY_RECOGNITION
adb -s $W shell pm grant com.nerdwatch android.permission.POST_NOTIFICATIONS
```

## Look at it

```powershell
adb -s $W shell input keyevent KEYCODE_WAKEUP
adb -s $W shell screencap -p /sdcard/shot.png; adb -s $W pull /sdcard/shot.png
```

The display sleeps quickly; on wake it returns to the **watch face**, not the last
app, so `am start` immediately before a screencap (with a `KEYCODE_WAKEUP`) is the
way to capture the app.

## Data that only resolves on real hardware

- **Weather (TEMP + post-sunset forecast)** — needs a GPS fix; falls back to the
  `78°` stub with no forecast line until location resolves.
- **Steps** — the real hardware counter / Samsung Health aggregation; shows the
  placeholder until it reads.
- **Next calendar event** — needs a calendar account synced to the watch.
