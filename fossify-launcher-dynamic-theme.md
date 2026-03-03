# Dynamic Theme (Material You) — Fossify Launcher
### Agent Instructions for Android Studio (Gemini)

> **Goal:** Add full Material You / dynamic colour support to the Fossify Launcher.
> The Fossify Commons library already ships `isUsingSystemTheme` plumbing and
> `you_*` colour resources — we only need to wire them up correctly in the
> Launcher module and expose a toggle in Settings.
>
> **Package:** `org.fossify.launcher`
> **Repo:** `https://github.com/FossifyOrg/Launcher`
> **Min SDK:** 21 | **Target / Compile SDK:** 34+
> **Commons dependency:** `org.fossify:commons`

---

## 0 — Prerequisites (verify before making any changes)

Open the terminal in Android Studio and run:

```bash
grep -r "isUsingSystemTheme" app/src/
grep -r "DynamicColors"       app/src/
```

- If `isUsingSystemTheme` already appears in `MainActivity` or `App.kt` the
  feature is partially done — skip those steps.
- If `DynamicColors` already appears in `App.kt` skip **Step 2**.

---

## 1 — Add / verify the Material dependency

**File:** `app/build.gradle.kts` (or `app/build.gradle`)

Ensure the following dependency is present inside the `dependencies {}` block.
Do **not** duplicate it if it already exists.

```kotlin
// Kotlin DSL
implementation("com.google.android.material:material:1.12.0")
```

```groovy
// Groovy DSL
implementation 'com.google.android.material:material:1.12.0'
```

After editing, click **"Sync Now"** in the yellow banner or run:
```bash
./gradlew :app:dependencies | grep material
```

---

## 2 — Bootstrap DynamicColors in the Application class

**File:** `app/src/main/kotlin/org/fossify/launcher/App.kt`

If this file does not exist, create it with the full content below.
If it exists, add only the missing import and the `DynamicColors` call.

```kotlin
package org.fossify.launcher

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply Material You wallpaper-derived colours to every Activity
        // on Android 12+ devices that support dynamic colour.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
```

**If you created a new `App.kt`**, register it in the manifest (Step 3).

---

## 3 — Register App class in AndroidManifest.xml

**File:** `app/src/main/AndroidManifest.xml`

Find the `<application` tag and add (or update) the `android:name` attribute:

```xml
<application
    android:name=".App"
    ... >
```

> ⚠️ If `android:name` is already set to a different class, open that class
> and add the `DynamicColors.applyToActivitiesIfAvailable(this)` call inside
> its `onCreate()` instead of creating a new `App.kt`.

---

## 4 — Add a Material3 dynamic theme style

**File:** `app/src/main/res/values/styles.xml`
(Create the file if it does not exist; if `themes.xml` already exists use that.)

Add a new style that inherits Material3 DayNight. The parent theme handles
all `colorPrimary`, `colorSurface`, `colorOnSurface`, etc. automatically via
the Monet / dynamic-colour engine:

```xml
<resources>
    <!-- Existing styles remain untouched above this line -->

    <!--
        Material You dynamic theme.
        On Android 12+ the system injects wallpaper-derived colours.
        On older Android the Material3 defaults are used as fallback.
    -->
    <style name="AppTheme.Dynamic" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
```

---

## 5 — Declare a night-mode variant (dark theme support)

**File:** `app/src/main/res/values-night/styles.xml`
(Create this file and directory if they do not exist.)

```xml
<resources>
    <!--
        Explicitly reference the same dynamic theme for night mode.
        Material3 DayNight handles light/dark switching automatically.
    -->
    <style name="AppTheme.Dynamic" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
```

---

## 6 — Update MainActivity to bridge Material You → Fossify Config

**File:** `app/src/main/kotlin/org/fossify/launcher/activities/MainActivity.kt`

### 6a — Add imports at the top of the file (after existing imports):

```kotlin
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.app.WallpaperManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
```

### 6b — Add a private helper function inside the `MainActivity` class:

Place this **before** `onCreate()` or at the bottom of the class, inside the
class body:

```kotlin
// ─── Dynamic Theme ────────────────────────────────────────────────────────

/**
 * Reads the current Material You (Monet) colours from the Activity's theme
 * and writes them into the Fossify Commons config so that all Commons views
 * (updateTextColors, getProperPrimaryColor, etc.) use the dynamic palette.
 *
 * Safe to call on every onResume(); it is a no-op on devices that do not
 * support dynamic colour (pre-Android 12 or unsupported OEM).
 */
private fun applyDynamicThemeToConfig() {
    if (!DynamicColors.isDynamicColorAvailable()) return
    if (!config.isUsingSystemTheme) return   // user opted out → respect choice

    val primaryColor = MaterialColors.getColor(
        this,
        com.google.android.material.R.attr.colorPrimary,
        Color.parseColor("#1565C0")          // safe Material Blue fallback
    )
    val backgroundColor = MaterialColors.getColor(
        this,
        com.google.android.material.R.attr.colorSurface,
        Color.WHITE
    )
    val textColor = MaterialColors.getColor(
        this,
        com.google.android.material.R.attr.colorOnSurface,
        Color.BLACK
    )

    config.customPrimaryColor    = primaryColor
    config.customBackgroundColor = backgroundColor
    config.customTextColor       = textColor
}
```

### 6c — Add wallpaper colour change listener as a class-level property:

Add this inside the `MainActivity` class body (top-level, not inside a function):

```kotlin
private val wallpaperColorListener =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        WallpaperManager.OnColorsChangedListener { _, _ ->
            applyDynamicThemeToConfig()
            updateTextColors(binding.root)
        }
    } else null
```

### 6d — Call `applyDynamicThemeToConfig()` in `onResume()`:

Find the existing `onResume()` override. If it does not exist, create it.
Add the call **at the start** of the function body:

```kotlin
override fun onResume() {
    super.onResume()
    applyDynamicThemeToConfig()   // ← add this line
    // … existing onResume code remains below unchanged …
}
```

### 6e — Register / unregister the wallpaper listener in `onStart` / `onStop`:

```kotlin
override fun onStart() {
    super.onStart()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        wallpaperColorListener?.let {
            WallpaperManager.getInstance(this)
                .addOnColorsChangedListener(it, Handler(Looper.getMainLooper()))
        }
    }
}

override fun onStop() {
    super.onStop()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        wallpaperColorListener?.let {
            WallpaperManager.getInstance(this).removeOnColorsChangedListener(it)
        }
    }
}
```

---

## 7 — Add the Settings toggle

### 7a — Add the string resource

**File:** `app/src/main/res/values/strings.xml`

Inside `<resources>` add:

```xml
<string name="use_dynamic_theme">Use dynamic theme (Material You)</string>
<string name="use_dynamic_theme_description">Colours follow your wallpaper on Android 12+</string>
```

### 7b — Add the toggle view to the Settings layout

**File:** `app/src/main/res/layout/activity_settings.xml`

Locate the group of other theme-related `SwitchCompat` / `RelativeLayout`
items (search for `isUsingSystemTheme` or `customization`). Add the following
block **immediately after** the last theme-related item:

```xml
<!-- Dynamic Theme toggle -->
<RelativeLayout
    android:id="@+id/settings_dynamic_theme_holder"
    style="@style/SettingsHolderCheckboxStyle"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <com.google.android.material.materialswitch.MaterialSwitch
        android:id="@+id/settings_dynamic_theme"
        style="@style/SettingsCheckboxStyle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/use_dynamic_theme" />

    <TextView
        android:id="@+id/settings_dynamic_theme_label"
        style="@style/SettingsTextLabelStyle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/use_dynamic_theme_description" />
</RelativeLayout>
```

> 💡 If `SettingsHolderCheckboxStyle` / `SettingsCheckboxStyle` do not exist
> in this project's styles, copy the style names from an adjacent existing
> settings row in the same layout file.

### 7c — Wire the toggle in SettingsActivity

**File:** `app/src/main/kotlin/org/fossify/launcher/activities/SettingsActivity.kt`

Add the following import at the top:

```kotlin
import com.google.android.material.color.DynamicColors
```

Inside the `setupSettings()` function (or equivalent init block), add:

```kotlin
// Dynamic theme (Material You) toggle
binding.settingsDynamicThemeHolder.beVisibleIf(DynamicColors.isDynamicColorAvailable())
binding.settingsDynamicTheme.isChecked = config.isUsingSystemTheme
binding.settingsDynamicTheme.setOnCheckedChangeListener { _, isChecked ->
    config.isUsingSystemTheme = isChecked
    updateTextColors(binding.root)
    // Recreate so the Activity re-reads the palette
    Handler(Looper.getMainLooper()).postDelayed({ recreate() }, 200)
}
```

> `beVisibleIf` is a Fossify Commons extension. On devices that do not support
> dynamic colour the toggle row will be hidden automatically.

---

## 8 — Apply dynamic colours to HomeScreenGrid drawing

The home-screen grid draws app-icon labels and folder backgrounds using
`Paint` objects. These need to respect the dynamic palette.

**File:** `app/src/main/kotlin/org/fossify/launcher/views/HomeScreenGrid.kt`

### 8a — Inject context colours into label paint

Find the existing `Paint` initialization for app-icon text (usually named
`labelPaint`, `appLabelPaint`, or similar). Wrap the colour assignment so it
reads from `config`:

```kotlin
// BEFORE (typical existing code):
labelPaint.color = Color.WHITE

// AFTER:
labelPaint.color = context.config.customTextColor
    .takeIf { context.config.isUsingSystemTheme }
    ?: Color.WHITE
```

### 8b — Refresh paint colours on theme change

Find or create the `onConfigurationChanged()` method (or a `refreshTheme()`
helper that is called when the config changes) and add:

```kotlin
fun refreshThemeColors() {
    val textColor = if (config.isUsingSystemTheme) config.customTextColor
                    else Color.WHITE
    labelPaint.color = textColor
    // repeat for any other Paint objects that use hard-coded colours
    invalidate()
}
```

Call `homeScreenGrid.refreshThemeColors()` from `MainActivity.onResume()`
**after** `applyDynamicThemeToConfig()`.

---

## 9 — AndroidManifest permissions (already present — verify only)

These permissions should already exist. Do **not** add duplicates:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<!-- Not required for dynamic colour but often present: -->
<uses-permission android:name="android.permission.SET_WALLPAPER" />
```

---

## 10 — Verify build and behaviour

```bash
# Clean build
./gradlew :app:clean :app:assembleDebug

# Install on a connected Android 12+ device / emulator
./gradlew :app:installDebug
```

**Manual test checklist:**
- [ ] Open Settings → confirm "Use dynamic theme" toggle is visible on Android 12+
  and hidden on Android 11 and below.
- [ ] Enable the toggle → launcher colours update to match wallpaper.
- [ ] Change wallpaper → colours refresh automatically (may require brief delay).
- [ ] Disable the toggle → returns to Fossify's custom colour picker.
- [ ] Dark mode switch → colours invert correctly via Material3 DayNight.
- [ ] Rotate screen → no crash, colours preserved.

---

## File Change Summary

| File | Action |
|------|--------|
| `app/build.gradle.kts` | Add `material:1.12.0` dependency |
| `app/src/main/kotlin/…/App.kt` | Create / update — call `DynamicColors.applyToActivitiesIfAvailable` |
| `app/src/main/AndroidManifest.xml` | Set `android:name=".App"` on `<application>` |
| `app/src/main/res/values/styles.xml` | Add `AppTheme.Dynamic` style |
| `app/src/main/res/values-night/styles.xml` | Add night variant of `AppTheme.Dynamic` |
| `app/src/main/res/values/strings.xml` | Add two dynamic-theme strings |
| `app/src/main/res/layout/activity_settings.xml` | Add toggle `RelativeLayout` block |
| `app/src/main/kotlin/…/activities/MainActivity.kt` | Add imports, helper, listener, onResume/onStart/onStop hooks |
| `app/src/main/kotlin/…/activities/SettingsActivity.kt` | Wire toggle to `config.isUsingSystemTheme` |
| `app/src/main/kotlin/…/views/HomeScreenGrid.kt` | Use `config.customTextColor` for label paint |

---

## Notes for the Agent

- **Never remove existing theme logic.** All changes are additive; existing
  `isUsingSystemTheme = false` paths must remain intact as the fallback.
- If the project already has a `you_background_color` colour resource in
  `res/values/colors.xml` (from the commons dependency), you can optionally
  use `resources.getColor(R.color.you_background_color, theme)` instead of
  `MaterialColors.getColor(…)` — both are correct.
- The `config` reference in all Activity files resolves to the Fossify Commons
  extension property `val Context.config get() = Config.newInstance(applicationContext)`.
  Do **not** create a new `Config` class — it already exists in the commons
  dependency.
- If Gemini cannot find `binding.root` because view binding is not yet enabled,
  it can fall back to `findViewById(android.R.id.content)` as the root view
  passed to `updateTextColors()`.
