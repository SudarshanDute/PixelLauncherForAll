package org.fossify.home.activities

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ensureBackgroundThread
import androidx.core.content.ContextCompat
import org.fossify.home.R
import org.fossify.home.databinding.ActivitySelectSearchBarWidgetBinding
import org.fossify.home.databinding.ItemWidgetPreviewBinding
import org.fossify.home.extensions.getInitialCellSize
import org.fossify.home.helpers.REQUEST_CONFIGURE_WIDGET
import org.fossify.home.helpers.WIDGET_HOST_ID
import org.fossify.home.models.AppWidget
import org.fossify.home.databinding.ItemWidgetListSectionBinding
import android.graphics.drawable.Drawable
import org.fossify.home.databinding.ItemWidgetListItemsHolderBinding
import java.util.ArrayList

sealed class WidgetListItem {
    class SectionHeader(val appTitle: String, val appIcon: Drawable?) : WidgetListItem()
    class WidgetCarousel(val widgets: List<AppWidget>) : WidgetListItem()
}

class SelectSearchBarWidgetActivity : SimpleActivity() {
    private val binding by viewBinding(ActivitySelectSearchBarWidgetBinding::inflate)
    private var mAppWidgetId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(padBottomSystem = listOf(binding.selectWidgetList))

        binding.selectWidgetToolbar.setNavigationOnClickListener {
            finish()
        }
        binding.selectWidgetToolbar.title = getString(R.string.search_bar_widget)

        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        getWidgets()
    }

    private fun getWidgets() {
        ensureBackgroundThread {
            val appWidgets = ArrayList<AppWidget>()
            val manager = AppWidgetManager.getInstance(this)
            val packageManager = packageManager
            val infoList = manager.installedProviders
            for (info in infoList) {
                val cellSize = getInitialCellSize(info, info.minWidth, info.minHeight)
                // Filter for widgets with height at most 2 and width at least 4
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
                    
                    // Improved visibility: load preview, then icon, then app icon fallback
                    var widgetPreviewImage = info.loadPreviewImage(this, resources.displayMetrics.densityDpi)
                    if (widgetPreviewImage == null && info.previewImage != 0 && appPackageName == packageName) {
                        widgetPreviewImage = ContextCompat.getDrawable(this, info.previewImage)
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

            // Group widgets by app title
            val groupedWidgets = appWidgets.groupBy { it.appTitle }
            val sortedAppTitles = groupedWidgets.keys.sorted()

            val listItems = ArrayList<WidgetListItem>()
            for (appTitle in sortedAppTitles) {
                val widgetsForApp = groupedWidgets[appTitle] ?: continue
                // Sort widgets within the app alphabetically
                val sortedWidgetsForApp = widgetsForApp.sortedBy { it.widgetTitle }
                
                // Add header for the app (using the icon from the first widget)
                listItems.add(WidgetListItem.SectionHeader(appTitle, sortedWidgetsForApp.firstOrNull()?.appIcon))
                
                // Add the horizontal carousel holder for the widgets
                listItems.add(WidgetListItem.WidgetCarousel(sortedWidgetsForApp))
            }

            runOnUiThread {
                binding.selectWidgetList.adapter = SelectWidgetAdapter(this, listItems) { widget ->
                    val resultIntent = Intent()
                    if (mAppWidgetId != -1 && widget.providerInfo != null) {
                        val success = manager.bindAppWidgetIdIfAllowed(mAppWidgetId, widget.providerInfo.provider)
                        if (success) {
                            if (widget.providerInfo.configure != null) {
                                val host = AppWidgetHost(this@SelectSearchBarWidgetActivity, WIDGET_HOST_ID)
                                host.startAppWidgetConfigureActivityForResult(
                                    this@SelectSearchBarWidgetActivity,
                                    mAppWidgetId,
                                    0,
                                    REQUEST_CONFIGURE_WIDGET,
                                    null
                                )
                            } else {
                                resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }
                        } else {
                            // If direct binding fails, return provider so Settings can try system binding
                            resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, widget.providerInfo.provider)
                            resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            if (resultCode == RESULT_OK) {
                val resultIntent = Intent()
                resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private class SelectWidgetAdapter(
        val context: Context,
        val items: List<WidgetListItem>,
        val itemClick: (AppWidget) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val TYPE_HEADER = 0
            private const val TYPE_WIDGET = 1
        }

        private val textColor = context.getProperTextColor()

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is WidgetListItem.SectionHeader -> TYPE_HEADER
                is WidgetListItem.WidgetCarousel -> TYPE_WIDGET
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_HEADER) {
                val binding = ItemWidgetListSectionBinding.inflate(LayoutInflater.from(context), parent, false)
                HeaderViewHolder(binding)
            } else {
                val binding = ItemWidgetListItemsHolderBinding.inflate(LayoutInflater.from(context), parent, false)
                WidgetHolderViewHolder(binding)
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
                
                item.widgets.forEachIndexed { index, widget ->
                    val widgetPreview = ItemWidgetPreviewBinding.inflate(LayoutInflater.from(context))
                    holder.binding.widgetListItemsHolder.addView(widgetPreview.root)
                    
                    widgetPreview.widgetTitle.text = widget.widgetTitle
                    val sizeText = "${widget.widthCells} x ${widget.heightCells}"
                    widgetPreview.widgetSize.text = sizeText
                    
                    Glide.with(context)
                        .load(widget.widgetPreviewImage)
                        .error(widget.appIcon)
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
