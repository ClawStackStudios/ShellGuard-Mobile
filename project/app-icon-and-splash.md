# 🎨 ShellGuard Mobile — App Icon & Splash Screen Specification

> **Adaptive Launcher Icon, Vector Drawables, Android 12+ SplashScreen API & Edge-to-Edge Polish**  
> *Targeted for Google AI Studio Android Application Generator.*

---

## 1. Iconography Concept

ShellGuard Mobile is the **full secrets vault**. Its icon combines the **Reef Lobster Shield** with a central **Vault Keyhole and Clam Pearl**, visually distinguishing it from the companion TOTP app (which features a countdown clock ring).

- **Brand Colors**:
  - Deep Abyss Canvas: `#0F1419`
  - Shield Core Gradient: Lobster Red (`#E4048A`) → Reef Pink (`#EC4899`) → Claw Cyan (`#06B6D4`)
  - Clam Pearl Highlight: Pearl White (`#F8FAFC`) with subtle cyan glow

---

## 2. Adaptive Launcher Icon Implementation

### Background Layer (`res/drawable/ic_launcher_background.xml`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#0F1419"
        android:pathData="M0,0h108v108h-108z" />
</vector>
```

### Foreground Layer (`res/drawable/ic_launcher_foreground.xml`)

Safe zone is the inner 72dp circle within the 108dp viewport:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    
    <!-- Outer Shield Contour -->
    <path
        android:pathData="M54,20 L76,28 C76,56 54,78 54,88 C54,78 32,56 32,28 Z"
        android:strokeWidth="2.5"
        android:strokeColor="#06B6D4">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="32"
                android:startY="20"
                android:endX="76"
                android:endY="88"
                android:type="linear">
                <item android:color="#E4048A" android:offset="0.0" />
                <item android:color="#EC4899" android:offset="0.5" />
                <item android:color="#06B6D4" android:offset="1.0" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Central Keyhole / Pearl Vault Emblem -->
    <path
        android:fillColor="#F8FAFC"
        android:pathData="M54,42 C50.68,42 48,44.68 48,48 C48,50.32 49.32,52.32 51.2,53.32 L49,66 L59,66 L56.8,53.32 C58.68,52.32 60,50.32 60,48 C60,44.68 57.32,42 54,42 Z" />
</vector>
```

### Mipmap XML Declarations
- `res/mipmap-anydpi-v26/ic_launcher.xml`
- `res/mipmap-anydpi-v26/ic_launcher_round.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

---

## 3. Android 12+ Core Splash Screen Integration

### A. Dependencies
In `app/build.gradle.kts`:
```kotlin
implementation("androidx.core:core-splashscreen:1.0.1")
```

### B. Theme Definition (`res/values/themes.xml`)

```xml
<resources>
    <!-- Starting splash theme -->
    <style name="Theme.App.Starting" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">#0F1419</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash_icon</item>
        <item name="postSplashScreenTheme">@style/Theme.ShellGuard</item>
    </style>

    <!-- Main application theme -->
    <style name="Theme.ShellGuard" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
```

> [!CAUTION]
> **API <31 Gradient Inflation Invariant**: The drawable referenced by `windowSplashScreenAnimatedIcon` (`ic_splash_icon.xml`) **MUST NOT** contain `<aapt:attr name="android:fillColor"><gradient>` tags. On API 24–30, Android's system decor inflater crashes when inflating inline vector gradients during splash. Use solid color fills (`#E4048A` or `#06B6D4`) in `ic_splash_icon.xml`.

### C. `MainActivity.kt` Initialization

```kotlin
package com.clawstack.shellguard.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Inviolable Security Guardrail: Prevent screen capture and task-switcher previews
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            ShellGuardMainApp()
        }
    }
}
```
