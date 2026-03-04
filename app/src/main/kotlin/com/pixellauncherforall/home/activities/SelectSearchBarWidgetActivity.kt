package com.pixellauncherforall.home.activities

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ensureBackgroundThread
import com.pixellauncherforall.home.R
import com.pixellauncherforall.home.databinding.ActivitySelectSearchBarWidgetBinding
import com.pixellauncherforall.home.databinding.ItemWidgetListItemsHolderBinding
import com.pixellauncherforall.home.databinding.ItemWidgetListSectionBinding
import com.pixellauncherforall.home.databinding.ItemWidgetPreviewBinding
import com.pixellauncherforall.home.extensions.getInitialCellSize
import com.pixellauncherforall.home.helpers.REQUEST_ALLOW_BINDING_WIDGET
import com.pixellauncherforall.home.helpers.REQUEST_CONFIGURE_WIDGET
import com.pixellauncherforall.home.helpers.WIDGET_HOST_ID
import com.pixellauncherforall.home.models.AppWidget
import java.util.ArrayList

sealed class WidgetListItem {
    data class SectionHeader(val appTitle: String, val appIcon: Drawable?) : WidgetListItem()
    data class WidgetCarousel(val widgets: List<AppWidget>) : WidgetListItem()
}

class SelectSearchBarWidgetActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySelectSearchBarWidgetBinding::inflate)
    private var mAppWidgetId = -1
    private var mSelectedWidgetProvider: ComponentName? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(padBottomSystem = listOf(binding.selectWidgetList))

        binding.selectWidgetToolbar.setNavigationOnClickListener {
            finish()
        }
        binding.selectWidgetToolbar.title = getString(R.string.search_bar_widget)

        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        
        if (savedInstanceState != null) {
            mAppWidgetId = savedInstanceState.getInt("app_widget_id", -1)
            mSelectedWidgetProvider = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelable("selected_provider", ComponentName::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelable("selected_provider") as? ComponentName
            }
        }

        getWidgets()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("app_widget_id", mAppWidgetId)
        outState.putParcelable("selected_provider", mSelectedWidgetProvider)
    }

    private fun getWidgets() {
        ensureBackgroundThread {
            val appWidgets = ArrayList<AppWidget>()
            val manager = AppWidgetManager.getInstance(this@SelectSearchBarWidgetActivity)
            val packageManager = packageManager
            val infoList = manager.installedProviders
            for (info in infoList) {
                val cellSize = getInitialCellSize(info, info.minWidth, info.minHeight)
                if (cellSize.height <= 2 && cellSize.width >= 4) {
                    val appPackageName = info.provider.packageName
                    val appTitle = try {
                        val appInfo = packageManager.getApplicationInfo(appPackageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        ""
                    }
                    
                    val appIcon = try {
                        packageManager.getApplicationIcon(appPackageName)
                    } catch (e: Exception) {
                        null
                    }

                    val widgetTitle = info.loadLabel(packageManager) ?: ""
                    
                    var widgetPreviewImage = info.loadPreviewImage(this@SelectSearchBarWidgetActivity, resources.displayMetrics.densityDpi)
                    if (widgetPreviewImage == null && info.previewImage != 0) {
                        try {
                            widgetPreviewImage = ContextCompat.getDrawable(this@SelectSearchBarWidgetActivity, info.previewImage)
                        } catch (e: Exception) { }
                    }
                    
                    if (widgetPreviewImage == null) {
                        widgetPreviewImage = appIcon
                    }
                    
                    val className = info.provider.className
                    
                    appWidgets.add(
                        AppWidget(
                            appPackageName = appPackageName,
                            appTitle = appTitle,
                            appIcon = appIcon,
                            widgetTitle = widgetTitle,
                            widgetPreviewImage = widgetPreviewImage,
                            widthCells = cellSize.width,
                            heightCells = cellSize.height,
                            isShortcut = false,
                            className = className,
                            providerInfo = info,
                            activityInfo = null
                        )
                    )
                }
            }

            val groupedWidgets = appWidgets.groupBy { it.appTitle }
            val sortedAppTitles = groupedWidgets.keys.sorted()

            val listItems = ArrayList<WidgetListItem>()
            for (appTitle in sortedAppTitles) {
                val widgetsForApp = groupedWidgets[appTitle] ?: continue
                val sortedWidgetsForApp = widgetsForApp.sortedBy { it.widgetTitle }
                listItems.add(WidgetListItem.SectionHeader(appTitle, sortedWidgetsForApp.firstOrNull()?.appIcon))
                listItems.add(WidgetListItem.WidgetCarousel(sortedWidgetsForApp))
            }

            runOnUiThread {
                binding.selectWidgetList.adapter = SelectWidgetAdapter(this@SelectSearchBarWidgetActivity, listItems) { widget ->
                    val provider = widget.providerInfo?.provider
                    if (mAppWidgetId != -1 && provider != null) {
                        mSelectedWidgetProvider = provider
                        val success = manager.bindAppWidgetIdIfAllowed(mAppWidgetId, provider)
                        if (success) {
                            handleWidgetSelected(provider)
                        } else {
                            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                            bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                            bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                            this@SelectSearchBarWidgetActivity.startActivityForResult(bindIntent, REQUEST_ALLOW_BINDING_WIDGET)
                        }
                    }
                }
            }
        }
    }

    private fun handleWidgetSelected(provider: ComponentName) {
        val manager = AppWidgetManager.getInstance(this@SelectSearchBarWidgetActivity)
        val info = manager.installedProviders.firstOrNull { it.provider == provider }
        if (info?.configure != null) {
            val host = AppWidgetHost(this@SelectSearchBarWidgetActivity, WIDGET_HOST_ID)
            host.startAppWidgetConfigureActivityForResult(
                this@SelectSearchBarWidgetActivity,
                mAppWidgetId,
                0,
                REQUEST_CONFIGURE_WIDGET,
                null
            )
        } else {
            val resultIntent = Intent()
            resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            if (resultCode == Activity.RESULT_OK) {
                val resultIntent = Intent()
                resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        } else if (requestCode == REQUEST_ALLOW_BINDING_WIDGET) {
            if (resultCode == Activity.RESULT_OK) {
                mSelectedWidgetProvider?.let { handleWidgetSelected(it) }
            }
        }
    }

    private class SelectWidgetAdapter(
        val context: Context,
        val items: List<WidgetListItem>,
        val itemClick: (AppWidget) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val textColor = context.getProperTextColor()

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is WidgetListItem.SectionHeader -> 0
                is WidgetListItem.WidgetCarousel -> 1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(context)
            return if (viewType == 0) {
                HeaderViewHolder(ItemWidgetListSectionBinding.inflate(inflater, parent, false))
            } else {
                WidgetHolderViewHolder(ItemWidgetListItemsHolderBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            if (holder is HeaderViewHolder && item is WidgetListItem.SectionHeader) {
                holder.binding.widgetAppTitle.text = item.appTitle
                holder.binding.widgetAppTitle.setTextColor(textColor)
                holder.binding.widgetAppIcon.setImageDrawable(item.appIcon)
            } else if (holder is WidgetHolderViewHolder && item is WidgetListItem.WidgetCarousel) {
                holder.binding.widgetListItemsHolder.removeAllViews()
                holder.binding.widgetListItemsScrollView.scrollX = 0
                
                for (widget in item.widgets) {
                    val widgetPreview = ItemWidgetPreviewBinding.inflate(LayoutInflater.from(context), holder.binding.widgetListItemsHolder, false)
                    holder.binding.widgetListItemsHolder.addView(widgetPreview.root)
                    
                    widgetPreview.widgetTitle.text = widget.widgetTitle
                    widgetPreview.widgetSize.text = "${widget.widthCells} x ${widget.heightCells}"
                    
                    Glide.with(context)
                        .load(widget.widgetPreviewImage)
                        .into(widgetPreview.widgetImage)

                    widgetPreview.root.setOnClickListener {
                        itemClick(widget)
                    }
                }
            }
        }

        override fun getItemCount() = items.size

        class HeaderViewHolder(val binding: ItemWidgetListSectionBinding) : RecyclerView.ViewHolder(binding.root)
        class WidgetHolderViewHolder(val binding: ItemWidgetListItemsHolderBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
