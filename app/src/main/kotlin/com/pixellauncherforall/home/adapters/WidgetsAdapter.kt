package com.pixellauncherforall.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.getProperTextColor
import com.pixellauncherforall.home.R
import com.pixellauncherforall.home.activities.SimpleActivity
import com.pixellauncherforall.home.databinding.ItemWidgetListItemsHolderBinding
import com.pixellauncherforall.home.databinding.ItemWidgetListSectionBinding
import com.pixellauncherforall.home.databinding.ItemWidgetPreviewBinding
import com.pixellauncherforall.home.extensions.config
import com.pixellauncherforall.home.helpers.WIDGET_LIST_ITEMS_HOLDER
import com.pixellauncherforall.home.helpers.WIDGET_LIST_SECTION
import com.pixellauncherforall.home.interfaces.WidgetsFragmentListener
import com.pixellauncherforall.home.models.WidgetsListItem
import com.pixellauncherforall.home.models.WidgetsListItemsHolder
import com.pixellauncherforall.home.models.WidgetsListSection

class WidgetsAdapter(
    val activity: SimpleActivity,
    var widgetListItems: ArrayList<WidgetsListItem>,
    val widgetsFragmentListener: WidgetsFragmentListener,
    val itemClick: () -> Unit
) : RecyclerView.Adapter<WidgetsAdapter.ViewHolder>() {

    private var textColor = activity.getProperTextColor()
    private var iconSize = 0

    init {
        calculateIconSize()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = when (viewType) {
            WIDGET_LIST_SECTION -> ItemWidgetListSectionBinding.inflate(inflater, parent, false)
            else -> ItemWidgetListItemsHolderBinding.inflate(inflater, parent, false)
        }

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val widgetListItem = widgetListItems[position]
        holder.bindView(widgetListItems[position]) { itemView, layoutPosition ->
            when (widgetListItem) {
                is WidgetsListSection -> setupListSection(itemView, widgetListItem)
                is WidgetsListItemsHolder -> setupListItemsHolder(itemView, widgetListItem)
            }
        }
    }

    override fun getItemCount() = widgetListItems.size

    override fun getItemViewType(position: Int) = when {
        widgetListItems[position] is WidgetsListSection -> WIDGET_LIST_SECTION
        else -> WIDGET_LIST_ITEMS_HOLDER
    }

    private fun calculateIconSize() {
        val defaultIconSize = activity.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
        iconSize = (defaultIconSize * (activity.config.iconSize / 100f)).toInt()
    }

    private fun setupListSection(view: View, section: WidgetsListSection) {
        ItemWidgetListSectionBinding.bind(view).apply {
            widgetAppTitle.text = section.appTitle
            widgetAppTitle.setTextColor(textColor)
            widgetAppIcon.setImageDrawable(section.appIcon)
            widgetAppIcon.layoutParams.width = iconSize
            widgetAppIcon.layoutParams.height = iconSize
        }
    }

    private fun setupListItemsHolder(view: View, listItem: WidgetsListItemsHolder) {
        val binding = ItemWidgetListItemsHolderBinding.bind(view)
        val childCount = binding.widgetListItemsHolder.childCount
        
        // Ensure child count matches required widgets
        if (childCount < listItem.widgets.size) {
            val inflateCount = listItem.widgets.size - childCount
            for (i in 0 until inflateCount) {
                val widgetPreview = ItemWidgetPreviewBinding.inflate(LayoutInflater.from(activity))
                binding.widgetListItemsHolder.addView(widgetPreview.root)
            }
        }

        // Bind data properly by hiding unused views at the tail end
        for (i in 0 until childCount.coerceAtLeast(listItem.widgets.size)) {
            val childView = binding.widgetListItemsHolder.getChildAt(i)
            if (i >= listItem.widgets.size) {
                childView.visibility = View.GONE
                continue
            }
            childView.visibility = View.VISIBLE
            
            val widget = listItem.widgets[i]
            val widgetPreview = ItemWidgetPreviewBinding.bind(childView)

            val imageSize = activity.resources.getDimension(R.dimen.widget_preview_size).toInt()
            val endMargin = if (i == listItem.widgets.size - 1) {
                activity.resources.getDimension(org.fossify.commons.R.dimen.medium_margin).toInt()
            } else {
                0
            }

            widgetPreview.widgetTitle.text = widget.widgetTitle
            widgetPreview.widgetSize.text = if (widget.isShortcut) {
                activity.getString(org.fossify.commons.R.string.shortcut)
            } else {
                "${widget.widthCells} x ${widget.heightCells}"
            }

            // Since it's a LinearLayout inside the MaterialCardView now,
            // we use MarginLayoutParams for maximum compatibility and set margins.
            (widgetPreview.root.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                marginStart = activity.resources.getDimension(org.fossify.commons.R.dimen.activity_margin).toInt()
                marginEnd = endMargin
            }

            widgetPreview.widgetImage.layoutParams.apply {
                width = imageSize
                height = imageSize
            }

            if (widget.widgetPreviewImage != null) {
                widgetPreview.widgetImage.setImageDrawable(widget.widgetPreviewImage)
            } else {
                widgetPreview.widgetImage.setImageDrawable(null)
            }

            widgetPreview.root.setOnClickListener { itemClick() }

            widgetPreview.root.setOnLongClickListener { view ->
                widgetsFragmentListener.onWidgetLongPressed(widget)
                true
            }
        }
    }

    fun updateItems(newItems: ArrayList<WidgetsListItem>) {
        val oldSum = widgetListItems.sumOf { it.getHashToCompare() }
        val newSum = newItems.sumOf { it.getHashToCompare() }
        if (oldSum != newSum) {
            widgetListItems = newItems
            notifyDataSetChanged()
        }
    }

    fun updateTextColor(newTextColor: Int) {
        if (newTextColor != textColor) {
            textColor = newTextColor
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(widgetListItem: WidgetsListItem, callback: (itemView: View, adapterPosition: Int) -> Unit) {
            itemView.apply {
                callback(this, adapterPosition)
            }
        }
    }
}
