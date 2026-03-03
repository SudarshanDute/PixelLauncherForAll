# Achieving Wallpaper Blur Behind the App Drawer

This guide details how to implement a real-time wallpaper blur effect that activates when the app drawer is open. The implementation is designed for Android 12+ and leverages `RenderEffect` for efficient, real-time blurring.

---

## 1. Core Components & Concepts

### Key Files:
1.  **`WallpaperBlurManager.kt`**: A singleton object responsible for low-level window flag manipulation and applying the blur radius.
2.  **`WallpaperBlurEffect.kt`**: A Jetpack Compose `SideEffect` that connects the blur manager to the Compose render cycle.
3.  **`BlurState.kt`**: A state holder class that manages the animation and value of the blur radius.

### Core Concept: `SideEffect` vs. `LaunchedEffect`
The implementation critically relies on Compose's `SideEffect`.

-   **`SideEffect`**: Runs *synchronously* on the main thread after every successful recomposition. This is essential for applying window properties like blur, which must be done synchronously to avoid race conditions and visual artifacts (like the screen flashing or "needs lock/unlock" errors).
-   **`LaunchedEffect`**: Runs in a coroutine (asynchronously). It is unsuitable for this task because the blur commands would not be synchronized with the view system's drawing pass.

---

## 2. Step-by-Step Implementation

### Step 1: Prepare the Activity Window

In your `MainActivity.kt`, you must prepare the window to allow content to draw behind the system bars and to show the wallpaper. This is done using `WallpaperBlurManager.prepareWindow()`.

**`MainActivity.kt`**
```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.pixelclone.WallpaperBlurManager // 1. Import the manager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Prepare the window BEFORE setContent
        WallpaperBlurManager.prepareWindow(this) 

        setContent {
            // Your app's composable content
        }
    }
}
```

**What `prepareWindow` does:**
-   Sets `FLAG_SHOW_WALLPAPER` to make the wallpaper visible behind the activity.
-   Sets `FLAG_LAYOUT_NO_LIMITS` to allow the app to draw into the screen's cutout areas.
-   Calls `WindowCompat.setDecorFitsSystemWindows(this, false)` to let content draw behind system bars.
-   Sets the window background to transparent so the wallpaper can show through.

### Step 2: Manage Blur State

The `BlurState` class uses an `Animatable` to drive the blur radius between `0f` (no blur) and `80f` (maximum blur). It provides simple methods to animate the blur in and out.

**`BlurState.kt`**
```kotlin
class BlurState(private val scope: CoroutineScope) {
    val blurRadius = Animatable(0f)

    // Animates blur from 0f to 80f
    fun blurIn() {
        scope.launch {
            blurRadius.animateTo(
                targetValue = 80f,
                animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
            )
        }
    }

    // Animates blur from 80f to 0f
    fun blurOut(onFinished: (() -> Unit)? = null) {
        scope.launch {
            blurRadius.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
            )
            onFinished?.invoke()
        }
    }
}
```

### Step 3: Apply the Blur in Compose

The `WallpaperBlurEffect` composable is the bridge between your UI state and the `WallpaperBlurManager`. You place this effect at a high level in your composable tree.

**`WallpaperBlurEffect.kt`**
```kotlin
@Composable
fun WallpaperBlurEffect(blurRadius: Float) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // Runs synchronously on the main thread after every recomposition
    SideEffect {
        WallpaperBlurManager.setBlurRadius(activity, blurRadius.toInt())
    }
}
```

### Step 4: Putting It All Together

In your main UI, you'll create an instance of `BlurState` and call its `blurIn()` and `blurOut()` methods based on the app drawer's visibility. The `WallpaperBlurEffect` will automatically react to changes in the `blurRadius` value.

**Example: `HomeScreen.kt`**
```kotlin
@Composable
fun HomeScreen() {
    // 1. Remember a CoroutineScope
    val scope = rememberCoroutineScope()
    
    // 2. Create and remember the BlurState
    val blurState = rememberBlurState(scope = scope)

    // 3. This state determines if the drawer is open or closed
    var isDrawerOpen by remember { mutableStateOf(false) }

    // 4. Call the blur effect. It will listen to blurState.blurRadius
    WallpaperBlurEffect(blurRadius = blurState.blurRadius.value)

    Box(modifier = Modifier.fillMaxSize()) {
        // Your home screen content (icons, widgets, etc.)

        if (isDrawerOpen) {
            // When the drawer is opened, trigger the blur-in animation
            LaunchedEffect(Unit) {
                blurState.blurIn()
            }
            AppDrawer(
                onBackPressed = {
                    // When closing, trigger the blur-out animation
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

## 3. The `WallpaperBlurManager` Explained

This object handles the direct interactions with the Android `Window`.

**`WallpaperBlurManager.kt`**
```kotlin
object WallpaperBlurManager {

    fun prepareWindow(activity: Activity) {
        // ... (sets window flags)
    }

    /**
     * Applies the blur. MUST be called on the main thread.
     */
    fun setBlurRadius(activity: Activity, radius: Int) {
        // Only runs on Android 12 (API 31) and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        activity.window.apply {
            // Sets the blur radius for what's BEHIND the window (the wallpaper)
            setBackgroundBlurRadius(radius)
            
            // Toggles the flag that enables the blur-behind effect
            if (radius > 0) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            } else {
                clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
    }
}
```

By following these steps, you can achieve a smooth, animated wallpaper blur that seamlessly integrates with your app's UI state.
