package com.pixellauncherforall.home.activities

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.launchMoreAppsFromUsIntent
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.isTiramisuPlus
import org.fossify.commons.helpers.isSPlus
import org.fossify.commons.models.FAQItem
import org.fossify.commons.models.RadioItem
import com.pixellauncherforall.home.BuildConfig
import com.pixellauncherforall.home.R
import com.pixellauncherforall.home.databinding.ActivitySettingsBinding
import com.pixellauncherforall.home.extensions.config
import com.pixellauncherforall.home.helpers.MAX_COLUMN_COUNT
import com.pixellauncherforall.home.helpers.MAX_ROW_COUNT
import com.pixellauncherforall.home.helpers.MIN_COLUMN_COUNT
import com.pixellauncherforall.home.helpers.MIN_ROW_COUNT
import com.pixellauncherforall.home.helpers.REQUEST_PICK_SEARCH_BAR_WIDGET
import com.pixellauncherforall.home.helpers.WIDGET_HOST_ID
import com.pixellauncherforall.home.receivers.LockDeviceAdminReceiver
import java.util.Locale

class SettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.settingsNestedScrollview))
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsAppbar)
        setupOptionsMenu()
        applyMaterialYouColors()
    }

    private fun applyMaterialYouColors() {
        if (isSPlus()) {
            val accent1 = getColor(android.R.color.system_accent1_500)
            val backgroundColor = getColor(android.R.color.system_neutral1_900)
            val containerColor = getColor(android.R.color.system_neutral2_800)

            binding.root.setBackgroundColor(backgroundColor)
            binding.settingsAppbar.setBackgroundColor(backgroundColor)
            binding.settingsCollapsingToolbar.setBackgroundColor(backgroundColor)
            binding.settingsCollapsingToolbar.setContentScrimColor(backgroundColor)

            binding.settingsGeneralSettingsLabel.setTextColor(accent1)
            binding.settingsDrawerSettingsLabel.setTextColor(accent1)
            binding.settingsHomeScreenLabel.setTextColor(accent1)
            binding.settingsDockLabel.setTextColor(accent1)
            binding.settingsIconSizeValue.setTextColor(accent1)

            binding.settingsGeneralSettingsHolder.setCardBackgroundColor(containerColor)
            binding.settingsDrawerSettingsHolder.setCardBackgroundColor(containerColor)
            binding.settingsHomeScreenSettingsHolder.setCardBackgroundColor(containerColor)
            binding.settingsDockSettingsHolder.setCardBackgroundColor(containerColor)

            val switchThumbColorChecked = getColor(android.R.color.system_neutral1_50)
            val switchTrackColorChecked = getColor(android.R.color.system_neutral1_400)
            
            val switchThumbColorUnchecked = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline, Color.GRAY)
            val switchTrackColorUnchecked = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHighest, Color.DKGRAY)

            val thumbCsl = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(switchThumbColorChecked, switchThumbColorUnchecked)
            )

            val trackCsl = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(switchTrackColorChecked, switchTrackColorUnchecked)
            )

            listOf(
                binding.settingsUseEnglish,
                binding.settingsDoubleTapToLock,
                binding.settingsShowSearchBar,
                binding.settingsOpenKeyboardOnAppDrawer,
                binding.settingsCloseAppDrawerOnOtherApp,
                binding.settingsShowDrawerAppLabels,
                binding.settingsShowHomeAppLabels,
                binding.settingsShowDock,
                binding.settingsShowDockLabels,
                binding.settingsShowSearchBarBelowDock
            ).forEach {
                it.thumbTintList = thumbCsl
                it.trackTintList = trackCsl
            }

            val sliderInactiveTrackColor = getColor(android.R.color.system_neutral2_700)
            
            val iconSizeSlider = binding.settingsIconSizeSlider
            iconSizeSlider.thumbTintList = ColorStateList.valueOf(switchThumbColorChecked)
            iconSizeSlider.trackActiveTintList = ColorStateList.valueOf(switchTrackColorChecked)
            iconSizeSlider.trackInactiveTintList = ColorStateList.valueOf(sliderInactiveTrackColor)
            iconSizeSlider.tickActiveTintList = ColorStateList.valueOf(backgroundColor)
            iconSizeSlider.tickInactiveTintList = ColorStateList.valueOf(switchTrackColorChecked)
        } else {
            val backgroundColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.BLACK)
            binding.root.setBackgroundColor(backgroundColor)
        }
    }



    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.settingsAppbar, NavigationIcon.Arrow)
        refreshMenuItems()

        setupUseEnglish()
        setupDoubleTapToLock()
        setupCloseAppDrawerOnOtherAppOpen()
        setupOpenKeyboardOnAppDrawer()
        setupDrawerColumnCount()
        setupDrawerSearchBar()
        setupShowDrawerAppLabels()
        setupHomeRowCount()
        setupHomeColumnCount()
        setupShowHomeAppLabels()
        setupShowDock()
        setupShowDockLabels()
        setupShowSearchBarBelowDock()
        setupSearchBarWidget()
        setupIconSize()
        setupLanguage()
        setupManageHiddenIcons()
        setupIconPack()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_PICK_SEARCH_BAR_WIDGET && resultCode == RESULT_OK && resultData != null) {
            val appWidgetId = resultData.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (appWidgetId != -1) {
                config.searchBarWidgetId = appWidgetId
                setupSearchBarWidget()
            }
        }
    }

    private fun setupOptionsMenu() {
        binding.settingsToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.about -> launchAbout()
                R.id.more_apps_from_us -> launchMoreAppsFromUsIntent()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun refreshMenuItems() {
        binding.settingsToolbar.menu.apply {
            findItem(R.id.more_apps_from_us).isVisible =
                !resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)
        }
    }

    private fun setupUseEnglish() {
        binding.settingsUseEnglishHolder.beVisibleIf(
            beVisible = (config.wasUseEnglishToggled || Locale.getDefault().language != "en")
                    && !isTiramisuPlus()
        )

        binding.settingsUseEnglish.isChecked = config.useEnglish
        binding.settingsUseEnglishHolder.setOnClickListener {
            binding.settingsUseEnglish.toggle()
            config.useEnglish = binding.settingsUseEnglish.isChecked
            recreate()
        }
    }

    private fun setupDoubleTapToLock() {
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        binding.settingsDoubleTapToLock.isChecked = devicePolicyManager.isAdminActive(
            ComponentName(this, LockDeviceAdminReceiver::class.java)
        )

        binding.settingsDoubleTapToLockHolder.setOnClickListener {
            val isLockDeviceAdminActive = devicePolicyManager.isAdminActive(
                ComponentName(this, LockDeviceAdminReceiver::class.java)
            )
            if (isLockDeviceAdminActive) {
                devicePolicyManager.removeActiveAdmin(
                    ComponentName(this, LockDeviceAdminReceiver::class.java)
                )
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(this, LockDeviceAdminReceiver::class.java)
                )
                intent.putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.lock_device_admin_hint)
                )
                startActivity(intent)
            }
        }
    }

    private fun setupOpenKeyboardOnAppDrawer() {
        binding.settingsOpenKeyboardOnAppDrawerHolder.beVisibleIf(config.showSearchBar)
        binding.settingsOpenKeyboardOnAppDrawer.isChecked = config.autoShowKeyboardInAppDrawer
        binding.settingsOpenKeyboardOnAppDrawerHolder.setOnClickListener {
            binding.settingsOpenKeyboardOnAppDrawer.toggle()
            config.autoShowKeyboardInAppDrawer = binding.settingsOpenKeyboardOnAppDrawer.isChecked
        }
    }

    private fun setupCloseAppDrawerOnOtherAppOpen() {
        binding.settingsCloseAppDrawerOnOtherApp.isChecked = config.closeAppDrawer
        binding.settingsCloseAppDrawerOnOtherAppHolder.setOnClickListener {
            binding.settingsCloseAppDrawerOnOtherApp.toggle()
            config.closeAppDrawer = binding.settingsCloseAppDrawerOnOtherApp.isChecked
        }
    }

    private fun setupDrawerColumnCount() {
        val currentColumnCount = config.drawerColumnCount
        binding.settingsDrawerColumnCount.text = currentColumnCount.toString()
        binding.settingsDrawerColumnCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in 1..MAX_COLUMN_COUNT) {
                items.add(
                    RadioItem(
                        id = i,
                        title = resources.getQuantityString(
                            org.fossify.commons.R.plurals.column_counts, i, i
                        )
                    )
                )
            }

            RadioGroupDialog(this, items, currentColumnCount) {
                val newColumnCount = it as Int
                if (currentColumnCount != newColumnCount) {
                    config.drawerColumnCount = newColumnCount
                    setupDrawerColumnCount()
                }
            }
        }
    }

    private fun setupDrawerSearchBar() {
        val showSearchBar = config.showSearchBar
        binding.settingsShowSearchBar.isChecked = showSearchBar
        binding.settingsDrawerSearchHolder.setOnClickListener {
            binding.settingsShowSearchBar.toggle()
            config.showSearchBar = binding.settingsShowSearchBar.isChecked
            binding.settingsOpenKeyboardOnAppDrawerHolder.beVisibleIf(config.showSearchBar)
        }
    }

    private fun setupShowDrawerAppLabels() {
        binding.settingsShowDrawerAppLabels.isChecked = config.showDrawerAppLabels
        binding.settingsShowDrawerAppLabelsHolder.setOnClickListener {
            binding.settingsShowDrawerAppLabels.toggle()
            config.showDrawerAppLabels = binding.settingsShowDrawerAppLabels.isChecked
        }
    }

    private fun setupHomeRowCount() {
        val currentRowCount = config.homeRowCount
        binding.settingsHomeScreenRowCount.text = currentRowCount.toString()
        binding.settingsHomeScreenRowCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in MIN_ROW_COUNT..MAX_ROW_COUNT) {
                items.add(
                    RadioItem(
                        id = i,
                        title = resources.getQuantityString(
                            org.fossify.commons.R.plurals.row_counts, i, i
                        )
                    )
                )
            }

            RadioGroupDialog(this, items, currentRowCount) {
                val newRowCount = it as Int
                if (currentRowCount != newRowCount) {
                    config.homeRowCount = newRowCount
                    setupHomeRowCount()
                }
            }
        }
    }

    private fun setupHomeColumnCount() {
        val currentColumnCount = config.homeColumnCount
        binding.settingsHomeScreenColumnCount.text = currentColumnCount.toString()
        binding.settingsHomeScreenColumnCountHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            for (i in MIN_COLUMN_COUNT..MAX_COLUMN_COUNT) {
                items.add(
                    RadioItem(
                        id = i,
                        title = resources.getQuantityString(
                            org.fossify.commons.R.plurals.column_counts, i, i
                        )
                    )
                )
            }

            RadioGroupDialog(this, items, currentColumnCount) {
                val newColumnCount = it as Int
                if (currentColumnCount != newColumnCount) {
                    config.homeColumnCount = newColumnCount
                    setupHomeColumnCount()
                }
            }
        }
    }

    private fun setupShowHomeAppLabels() {
        binding.settingsShowHomeAppLabels.isChecked = config.showHomeAppLabels
        binding.settingsShowHomeAppLabelsHolder.setOnClickListener {
            binding.settingsShowHomeAppLabels.toggle()
            config.showHomeAppLabels = binding.settingsShowHomeAppLabels.isChecked
        }
    }

    private fun setupShowDock() {
        binding.settingsShowDock.isChecked = config.showDock
        binding.settingsShowDockHolder.setOnClickListener {
            binding.settingsShowDock.toggle()
            config.showDock = binding.settingsShowDock.isChecked
        }
    }

    private fun setupShowDockLabels() {
        binding.settingsShowDockLabels.isChecked = config.showDockLabels
        binding.settingsShowDockLabelsHolder.setOnClickListener {
            binding.settingsShowDockLabels.toggle()
            config.showDockLabels = binding.settingsShowDockLabels.isChecked
        }
    }

    private fun setupShowSearchBarBelowDock() {
        binding.settingsShowSearchBarBelowDock.isChecked = config.showSearchBarBelowDock
        binding.settingsShowSearchBarBelowDockHolder.setOnClickListener {
            binding.settingsShowSearchBarBelowDock.toggle()
            config.showSearchBarBelowDock = binding.settingsShowSearchBarBelowDock.isChecked
            setupSearchBarWidget()
        }
    }

    private fun setupSearchBarWidget() {
        val showSearchBarBelowDock = config.showSearchBarBelowDock
        binding.settingsSearchBarWidgetHolder.beVisibleIf(showSearchBarBelowDock)
        binding.settingsSearchBarWidgetDivider.beVisibleIf(showSearchBarBelowDock)
        if (showSearchBarBelowDock) {
            val widgetId = config.searchBarWidgetId
            if (widgetId != -1) {
                val appWidgetManager = AppWidgetManager.getInstance(this)
                val info = appWidgetManager.getAppWidgetInfo(widgetId)
                binding.settingsSearchBarWidget.text = info?.loadLabel(packageManager) ?: getString(R.string.none)
            } else {
                binding.settingsSearchBarWidget.text = getString(R.string.none)
            }

            binding.settingsSearchBarWidgetHolder.setOnClickListener {
                val appWidgetHost = AppWidgetHost(this, WIDGET_HOST_ID)
                val appWidgetId = appWidgetHost.allocateAppWidgetId()
                val intent = Intent(this, SelectSearchBarWidgetActivity::class.java)
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                startActivityForResult(intent, REQUEST_PICK_SEARCH_BAR_WIDGET)
            }
        }
    }

    private fun setupIconSize() {
        val currentIconSize = config.iconSize
        binding.settingsIconSizeValue.text = getString(R.string.percent_format, currentIconSize)
        binding.settingsIconSizeSlider.value = currentIconSize.toFloat()
        binding.settingsIconSizeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val newSize = value.toInt()
                binding.settingsIconSizeValue.text = getString(R.string.percent_format, newSize)
                config.iconSize = newSize
            }
        }
    }

    @SuppressLint("NewApi")
    private fun setupLanguage() {
        binding.settingsLanguage.text = Locale.getDefault().displayLanguage
        binding.settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        binding.settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupManageHiddenIcons() {
        binding.settingsManageHiddenIconsHolder.setOnClickListener {
            startActivity(Intent(this, HiddenIconsActivity::class.java))
        }
    }

    private fun setupIconPack() {
        val currentIconPack = config.iconPack
        if (currentIconPack.isEmpty()) {
            binding.settingsIconPack.text = getString(R.string.default_icon_pack)
        } else {
            try {
                val appInfo = packageManager.getApplicationInfo(currentIconPack, 0)
                binding.settingsIconPack.text = packageManager.getApplicationLabel(appInfo)
            } catch (e: Exception) {
                binding.settingsIconPack.text = getString(R.string.default_icon_pack)
                config.iconPack = ""
            }
        }

        binding.settingsIconPackHolder.setOnClickListener {
            val iconPackIntents = listOf(
                Intent("com.novalauncher.THEME"),
                Intent("org.adw.launcher.THEMES.icons"),
                Intent("com.gau.go.launcherex.theme"),
                Intent("com.fede.launcher.THEME_ICONPACK"),
                Intent("com.anddoes.launcher.THEME"),
                Intent("com.teslacoilsw.launcher.THEME")
            )

            val resolveInfos = mutableListOf<android.content.pm.ResolveInfo>()
            for (intent in iconPackIntents) {
                resolveInfos.addAll(packageManager.queryIntentActivities(intent, 0))
            }

            val items = ArrayList<RadioItem>()
            items.add(RadioItem(0, getString(R.string.default_icon_pack), ""))

            var currentId = 0
            val uniqueResolveInfos = resolveInfos.distinctBy { it.activityInfo.packageName }
            uniqueResolveInfos.forEachIndexed { index, resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(packageManager).toString()
                val id = index + 1
                items.add(RadioItem(id, label, packageName))
                if (packageName == currentIconPack) {
                    currentId = id
                }
            }

            RadioGroupDialog(this, items, currentId) {
                val selectedPack = it as String
                if (selectedPack != currentIconPack) {
                    config.iconPack = selectedPack
                    setupIconPack()
                }
            }
        }
    }

    private fun launchAbout() {
        val licenses = 0L
        val faqItems = ArrayList<FAQItem>()

        if (!resources.getBoolean(org.fossify.commons.R.bool.hide_google_relations)) {
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_2_title_commons,
                    text = org.fossify.commons.R.string.faq_2_text_commons
                )
            )
            faqItems.add(
                FAQItem(
                    title = org.fossify.commons.R.string.faq_6_title_commons,
                    text = org.fossify.commons.R.string.faq_6_text_commons
                )
            )
        }

        startAboutActivity(
            appNameId = R.string.app_name,
            licenseMask = licenses,
            versionName = BuildConfig.VERSION_NAME,
            faqItems = faqItems,
            showFAQBeforeMail = true
        )
    }
}
