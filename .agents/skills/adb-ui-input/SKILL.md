---
name: adb-ui-input
description: Cheatsheet for driving Android device UI over ADB — text input quirks, keyboard handling, screenshot verification loop. Hardened through ShellGuard Android on-device testing (Pixel, TLS ADB).
license: MIT
metadata:
  author: ClawStack Studios
  keywords: [adb, input, screencap, ui-automation, android-testing]
---

# ADB UI Input & Verification Cheatsheet

## Connection
```bash
ADB="/config/Android/Sdk/platform-tools/adb -s adb-<DEVICE-ID>._adb-tls-connect._tcp"
$ADB exec-out screencap -p > /tmp/screen.png   # screenshot for verification
```

## Core Invariants
1. **Screenshot-verify EVERY input before acting on assumed state.** The field contents you assume are wrong until the screenshot says otherwise (dot-count = truth for password fields).
2. **`input text` APPENDS** to the focused field — it never replaces content.
3. **Clearing a field:** tap field → `input keyevent 123` (cursor to end) → `input keyevent 67` (DEL) ×N → **screenshot to confirm 0 chars** → then type.
4. **The IME keyboard covers the bottom third of the screen.** Dismiss it with `input keyevent 4` (BACK) before tapping bottom-anchored buttons (Unlock / Confirm / FAB).
5. `input keyevent 111` (ESC) closes dialogs/popups only; use `keyevent 4` for the keyboard.
6. **Prefer tapping the visible submit button** over `keyevent 66` (Enter) — Enter behavior varies per field type (some insert a newline instead of submitting).
7. **Every `adb install -r` re-locks a PIN-protected vault** (backgrounding invariant / FLAG_SECURE). Budget an unlock step after every reinstall.
8. Slow animations / first-launch: add `sleep 2–5` after navigation taps before screencap, or the screenshot races the transition.
9. **Never guess tap coordinates from screenshots.** Dynamic Compose layouts and IME scrolling shift views. Extract exact hardware bounds via `uiautomator`:
   ```bash
   $ADB shell uiautomator dump /sdcard/window_dump.xml
   $ADB shell cat /sdcard/window_dump.xml | python3 -c "
   import sys, xml.etree.ElementTree as ET
   for n in ET.fromstring(sys.stdin.read()).iter('node'):
       t, d, b = n.attrib.get('text',''), n.attrib.get('content-desc',''), n.attrib.get('bounds','')
       if any(k in (t+d).lower() for k in ['target_keyword']):
           print(f'{t} | {d} | {b}')
   "
   # Parse bounds [x1,y1][x2,y2] and tap center: x=(x1+x2)//2, y=(y1+y2)//2
   ```
10. **`input text` shell escaping**: Characters like `(`, `)`, `&`, `;`, `<`, `>`, `*`, `|` trigger `sh: syntax error` in Android ash shell. Escape special characters or replace spaces with `%s` (`input text "My%sAccount"`).
11. **System & App Dark Theme Toggling**:
    ```bash
    # OS level night mode:
    $ADB shell cmd uimode night yes  # or 'no'
    # App-level override when persisted to SharedPreferences:
    $ADB shell "run-as com.clawstack.shellguard.totp sed -i 's/pref_theme_mode\">LIGHT/pref_theme_mode\">DARK/' /data/data/com.clawstack.shellguard.totp/shared_prefs/shellguard_auth_prefs.xml"
    $ADB shell am force-stop com.clawstack.shellguard.totp && $ADB shell am start -n com.clawstack.shellguard.totp/.MainActivity
    ```

## Recipe: Reliable Field Entry
```bash
$ADB shell input tap <field-x> <field-y>; sleep 1
$ADB shell input keyevent 123
for i in $(seq 1 15); do $ADB shell input keyevent 67; done
# screenshot -> verify empty
$ADB shell input text 1234
$ADB shell input keyevent 4      # dismiss keyboard
$ADB shell input tap <submit-btn>
# screenshot -> verify result
```

## 12. Dual-Device / Multi-Form-Factor Verification (Phone + Tablet)
When multiple devices (e.g. Google Pixel + Nexus 7 tablet) are attached:
1. **Device Discovery & Variable Setup**:
   ```bash
   # List active endpoints:
   /config/Android/Sdk/platform-tools/adb devices -l
   # Bind serial variables:
   PIXEL="/config/Android/Sdk/platform-tools/adb -s $(/config/Android/Sdk/platform-tools/adb devices | grep -E "FA|sailfish|pixel" | awk '{print $1}')"
   TABLET="/config/Android/Sdk/platform-tools/adb -s $(/config/Android/Sdk/platform-tools/adb devices | grep -E "0a3c85db|nexus|tablet" | awk '{print $1}')"
   ```
2. **Display Metric Inspection**:
   ```bash
   $PIXEL shell wm size      # e.g. 1080x1920 (phone)
   $TABLET shell wm size     # e.g. 1200x1920 (tablet)
   ```
3. **Comparative Dual-Device Screencaps**:
   Always capture and compare UI states across both screens after layout changes to verify responsive spacing, dialog sizing, and navigation padding:
   ```bash
   $PIXEL exec-out screencap -p > /tmp/pixel_screen.png
   $TABLET exec-out screencap -p > /tmp/tablet_screen.png
   ```


