package com.pixellauncherforall.home.helpers

import android.content.Context
import org.fossify.commons.helpers.BaseConfig
import com.pixellauncherforall.home.R

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var wasHomeScreenInit: Boolean
        get() = prefs.getBoolean(WAS_HOME_SCREEN_INIT, false)
        set(wasHomeScreenInit) = prefs.edit().putBoolean(WAS_HOME_SCREEN_INIT, wasHomeScreenInit).apply()

    var homeColumnCount: Int
        get() = prefs.getInt(HOME_COLUMN_COUNT, COLUMN_COUNT)
        set(homeColumnCount) = prefs.edit().putInt(HOME_COLUMN_COUNT, homeColumnCount).apply()

    var homeRowCount: Int
        get() = prefs.getInt(HOME_ROW_COUNT, ROW_COUNT)
        set(homeRowCount) = prefs.edit().putInt(HOME_ROW_COUNT, homeRowCount).apply()

    var drawerColumnCount: Int
        get() = prefs.getInt(DRAWER_COLUMN_COUNT, context.resources.getInteger(R.integer.portrait_column_count))
        set(drawerColumnCount) = prefs.edit().putInt(DRAWER_COLUMN_COUNT, drawerColumnCount).apply()

    var showSearchBar: Boolean
        get() = prefs.getBoolean(SHOW_SEARCH_BAR, true)
        set(showSearchBar) = prefs.edit().putBoolean(SHOW_SEARCH_BAR, showSearchBar).apply()

    var closeAppDrawer: Boolean
        get() = prefs.getBoolean(CLOSE_APP_DRAWER, false)
        set(closeAppDrawer) = prefs.edit().putBoolean(CLOSE_APP_DRAWER, closeAppDrawer).apply()

    var autoShowKeyboardInAppDrawer: Boolean
        get() = prefs.getBoolean(AUTO_SHOW_KEYBOARD_IN_APP_DRAWER, false)
        set(autoShowKeyboardInAppDrawer) = prefs.edit()
            .putBoolean(AUTO_SHOW_KEYBOARD_IN_APP_DRAWER, autoShowKeyboardInAppDrawer).apply()

    var showDrawerAppLabels: Boolean
        get() = prefs.getBoolean(SHOW_DRAWER_APP_LABELS, true)
        set(showDrawerAppLabels) = prefs.edit().putBoolean(SHOW_DRAWER_APP_LABELS, showDrawerAppLabels).apply()

    var showHomeAppLabels: Boolean
        get() = prefs.getBoolean(SHOW_HOME_APP_LABELS, true)
        set(showHomeAppLabels) = prefs.edit().putBoolean(SHOW_HOME_APP_LABELS, showHomeAppLabels).apply()

    var showDock: Boolean
        get() = prefs.getBoolean(SHOW_DOCK, true)
        set(showDock) = prefs.edit().putBoolean(SHOW_DOCK, showDock).apply()

    var showDockLabels: Boolean
        get() = prefs.getBoolean(SHOW_DOCK_LABELS, false)
        set(showDockLabels) = prefs.edit().putBoolean(SHOW_DOCK_LABELS, showDockLabels).apply()

    var showSearchBarBelowDock: Boolean
        get() = prefs.getBoolean(SHOW_SEARCH_BAR_BELOW_DOCK, true)
        set(showSearchBarBelowDock) = prefs.edit().putBoolean(SHOW_SEARCH_BAR_BELOW_DOCK, showSearchBarBelowDock).apply()

    var searchBarWidgetId: Int
        get() = prefs.getInt(SEARCH_BAR_WIDGET_ID, -1)
        set(searchBarWidgetId) = prefs.edit().putInt(SEARCH_BAR_WIDGET_ID, searchBarWidgetId).apply()

    var iconSize: Int
        get() = prefs.getInt(ICON_SIZE, 100)
        set(value) {
            prefs.edit().putInt(ICON_SIZE, value).apply()
        }

    var isUsingSystemTheme: Boolean
        get() = prefs.getBoolean(IS_USING_SYSTEM_THEME, false)
        set(isUsingSystemTheme) = prefs.edit().putBoolean(IS_USING_SYSTEM_THEME, isUsingSystemTheme).apply()

    var iconPack: String
        get() = prefs.getString(ICON_PACK, "") ?: ""
        set(iconPack) = prefs.edit().putString(ICON_PACK, iconPack).apply()
}
