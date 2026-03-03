# Wallpaper Blur for Android Launcher (Jetpack Compose) — AI Agent Implementation Guide

## Overview
This guide tells the AI agent exactly what to do inside Android Studio to implement wallpaper blur for an Android launcher app drawer built with **Jetpack Compose**. The blur must activate as soon as the app drawer opens, with a smooth animated transition.

---

## Target Environment
- **Min SDK**: 31 (Android 12)
- **Target SDK**: 35+ (Android 15/16)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Important**: On Android 15/16, `WallpaperManager.getDrawable()` is blocked for third-party launchers. Do NOT use it. Use the Window blur API exclusively.

---

## Root Cause of "Blur Only Applies After Lock/Unlock or Back Button"

This happens because of **two mistakes** that are easy to make:

1. **Using `LaunchedEffect` to apply window blur** — `LaunchedEffect` runs inside a coroutine which is asynchronous. Window attribute changes must happen **synchronously on the main thread**. Use `SideEffect` instead, which runs synchronously after every recomposition.

2. **Missing `FLAG_SHOW_WALLPAPER` as a programmatic flag** — Setting `windowShowWallpaper` only in the XML theme is sometimes not enough for launcher activities. It must also be set programmatically on the window in code.

---

## Step 1 — Update `AndroidManifest.xml`

```xml
<activity
    android:name=".MainActivity"
    android:theme="@style/LauncherTheme"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

---

## Step 2 — Update `res/values/themes.xml`

```xml
<style name="LauncherTheme" parent="Theme.AppCompat.NoActionBar">
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:windowShowWallpaper">true</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```

---

## Step 3 — Create `WallpaperBlurManager.kt`

Create at:
```
app/src/main/java/<your_package>/utils/WallpaperBlurManager.kt
```

```kotlin
package <your_package>.utils

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat

object WallpaperBlurManager {

    /**
     * Call this in MainActivity.onCreate() BEFORE setContent {}.
     * Sets up all required window flags for wallpaper + blur to work correctly.
     */
    fun prepareWindow(activity: Activity) {
        activity.window.apply {
            // Critical: must be set programmatically, not just in theme XML
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            // Allow content to draw behind system bars
            WindowCompat.setDecorFitsSystemWindows(this, false)

            // Transparent background so wallpaper shows through
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    /**
     * Apply blur at a specific radius.
     * IMPORTANT: This must be called on the MAIN THREAD synchronously.
     * In Compose, call this from SideEffect{} — never from LaunchedEffect{}.
     *
     * @param radius 0 = no blur, 80 = heavy blur
     */
    fun setBlurRadius(activity: Activity, radius: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        activity.window.apply {
            // Set background blur (blurs what's directly behind the window)
            setBackgroundBlurRadius(radius)

            // Set blur-behind (blurs the wallpaper behind the entire window)
            val params = attributes
            params.blurBehindRadius = radius
            attributes = params

            // Toggle the blur-behind flag based on radius
            if (radius > 0) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            } else {
                clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }

            // Force the compositor to pick up the change immediately
            decorView.invalidate()
        }
    }
}
```

> **Agent note**: Replace `<your_package>` with the actual package name from `AndroidManifest.xml`.

---

## Step 4 — Create `BlurState.kt`

Create at:
```
app/src/main/java/<your_package>/ui/state/BlurState.kt
```

```kotlin
package <your_package>.ui.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BlurState(private val scope: CoroutineScope) {

    val blurRadius = Animatable(0f)

    fun blurIn() {
        scope.launch {
            blurRadius.animateTo(
                targetValue = 80f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }

    fun blurOut(onFinished: (() -> Unit)? = null) {
        scope.launch {
            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            onFinished?.invoke()
        }
    }
}

@Composable
fun rememberBlurState(scope: CoroutineScope): BlurState {
    return remember(scope) { BlurState(scope) }
}
```

---

## Step 5 — Create `BlurEffect.kt`

Create at:
```
app/src/main/java/<your_package>/ui/effects/BlurEffect.kt
```

```kotlin
package <your_package>.ui.effects

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import <your_package>.utils.WallpaperBlurManager

/**
 * Composable side effect that applies wallpaper blur on every recomposition.
 *
 * CRITICAL: Uses SideEffect (NOT LaunchedEffect).
 * SideEffect runs synchronously on the main thread after every successful recomposition.
 * LaunchedEffect is async (coroutine) and causes the "needs lock/unlock" bug.
 *
 * @param blurRadius current animated blur radius (0f–80f), driven by BlurState.blurRadius.value
 */
@Composable
fun WallpaperBlurEffect(blurRadius: Float) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // SideEffect runs synchronously on main thread — this is the correct approach
    SideEffect {
        WallpaperBlurManager.setBlurRadius(activity, blurRadius.toInt())
    }
}
```

---

## Step 6 — Wire Into MainActivity and Root Composable

### `MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Must be called BEFORE setContent
        WallpaperBlurManager.prepareWindow(this)

        setContent {
            LauncherTheme {
                LauncherRoot()
            }
        }
    }
}
```

### `LauncherRoot.kt`

```kotlin
@Composable
fun LauncherRoot() {
    val scope = rememberCoroutineScope()
    val blurState = rememberBlurState(scope)
    var isDrawerOpen by remember { mutableStateOf(false) }

    // This is the ONLY place blur is applied — SideEffect inside WallpaperBlurEffect
    // handles the main thread sync. Do not apply blur anywhere else.
    WallpaperBlurEffect(blurRadius = blurState.blurRadius.value)

    Box(modifier = Modifier.fillMaxSize()) {

        HomeScreen(
            onDrawerOpen = {
                isDrawerOpen = true
                blurState.blurIn()
            }
        )

        if (isDrawerOpen) {
            AppDrawerScreen(
                onDismiss = {
                    blurState.blurOut {
                        isDrawerOpen = false
                    }
                }
            )
        }
    }
}
```

---

## Step 7 — App Drawer Composable

```kotlin
@Composable
fun AppDrawerScreen(onDismiss: () -> Unit) {

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Semi-transparent overlay on top of the blur — do NOT use solid Color.Black
            .background(Color(0x55000000))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        // Your app grid/list here
    }
}
```

---

## Step 8 — Verify `build.gradle`

```groovy
android {
    defaultConfig {
        minSdk 31
        targetSdk 35
    }
}
```

---

## What NOT To Do

| Do NOT | Why |
|---|---|
| Use `LaunchedEffect` to call `setBlurRadius` | Async coroutine — window sees the change only after a redraw cycle, causing the lock/unlock bug |
| Skip `FLAG_SHOW_WALLPAPER` programmatic flag | Theme XML alone is unreliable for launcher activities — set it in code too |
| Use `Modifier.blur()` on a composable | Blurs the composable's own pixels, not the wallpaper behind the window |
| Use `WallpaperManager.getDrawable()` | Blocked on Android 15/16 for third-party launchers |
| Set opaque background on root composable | Hides the wallpaper entirely |
| Call `prepareWindow()` after `setContent {}` | Window flags must be set before Compose attaches its content |

---

## Why `SideEffect` vs `LaunchedEffect`

| | `SideEffect` | `LaunchedEffect` |
|---|---|---|
| Runs on | Main thread, synchronously | Coroutine, asynchronously |
| Timing | After every recomposition | After first composition or key change |
| Window ops | ✅ Correct | ❌ Causes deferred update bug |
| Use case | Syncing Compose state → non-Compose APIs | Starting async work |

---

## Files Modified

| File | Action |
|---|---|
| `AndroidManifest.xml` | Verify `LauncherTheme` on main activity |
| `res/values/themes.xml` | Add `LauncherTheme` with translucent + wallpaper flags |
| `utils/WallpaperBlurManager.kt` | **Create** — window blur helper, includes `decorView.invalidate()` |
| `ui/state/BlurState.kt` | **Create** — Animatable state holder |
| `ui/effects/BlurEffect.kt` | **Create** — `SideEffect`-based bridge (NOT LaunchedEffect) |
| `MainActivity.kt` | Call `prepareWindow()` before `setContent`, use `LauncherRoot` |
| `LauncherRoot.kt` | Wire `blurState`, `WallpaperBlurEffect`, open/close callbacks |
| `AppDrawerScreen.kt` | Semi-transparent background, `BackHandler` |
| `app/build.gradle` | Verify `minSdk 31` |

---

## Expected Result

- App drawer opens → wallpaper blurs in immediately and smoothly over ~300ms
- App drawer closes → blur fades out smoothly over ~300ms
- No lock/unlock or back button needed to trigger the blur
- Works on Android 12, 13, 14, 15, and 16
- Works with static and live wallpapers
- No wallpaper permission required
