package org.fossify.home.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getColoredDrawableWithColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.home.R
import org.fossify.home.activities.SimpleActivity
import org.fossify.home.databinding.ItemLauncherLabelBinding
import org.fossify.home.extensions.animateScale
import org.fossify.home.extensions.config
import org.fossify.home.interfaces.AllAppsListener
import org.fossify.home.models.AppLauncher

class LaunchersAdapter(
    val activity: SimpleActivity,
    val allAppsListener: AllAppsListener,
    val itemClick: (Any) -> Unit
) : ListAdapter<AppLauncher, LaunchersAdapter.ViewHolder>(AppLauncherDiffCallback()),
    RecyclerViewFastScroller.OnPopupTextUpdate {

    private var textColor = activity.getProperTextColor()
    private var iconSize = 0

    init {
        setHasStableIds(true)
        calculateIconSize()
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).getLauncherIdentifier().hashCode().toLong()
    }

    fun launchFirstApp(): Boolean {
        val launcher = currentList.firstOrNull() ?: return false
        itemClick(launcher)
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLauncherLabelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        binding.launcherIcon.layoutParams.width = iconSize
        binding.launcherIcon.layoutParams.height = iconSize
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(getItem(position))
    }

    override fun submitList(list: MutableList<AppLauncher>?) {
        calculateIconSize()
        super.submitList(list)
    }

    private fun calculateIconSize() {
        val defaultIconSize = activity.resources.getDimensionPixelSize(R.dimen.launcher_icon_size)
        iconSize = (defaultIconSize * (activity.config.iconSize / 100f) * 1.08f).toInt()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTextColor(newTextColor: Int) {
        if (newTextColor != textColor) {
            textColor = newTextColor
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(private val binding: ItemLauncherLabelBinding) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            itemView.setOnClickListener { 
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClick(getItem(position))
                }
            }
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val location = IntArray(2)
                    itemView.getLocationOnScreen(location)
                    allAppsListener.onAppLauncherLongPressed(
                        x = (location[0] + itemView.width / 2).toFloat(),
                        y = location[1].toFloat(),
                        appLauncher = getItem(position)
                    )
                }
                true
            }

            @SuppressLint("ClickableViewAccessibility")
            itemView.setOnTouchListener { _, event ->
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnTouchListener false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        binding.launcherIcon.drawable?.alpha = LAUNCHER_ALPHA_PRESSED
                        itemView.animateScale(
                            from = LAUNCHER_SCALE_NORMAL,
                            to = LAUNCHER_SCALE_PRESSED,
                            duration = LAUNCHER_SCALE_UP_DURATION
                        )
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        binding.launcherIcon.drawable?.alpha = LAUNCHER_ALPHA_NORMAL
                        itemView.animateScale(
                            from = LAUNCHER_SCALE_PRESSED,
                            to = LAUNCHER_SCALE_NORMAL,
                            duration = LAUNCHER_SCALE_DOWN_DURATION
                        )
                    }
                }
                false
            }
        }

        fun bindView(launcher: AppLauncher) {
            binding.launcherLabel.text = launcher.title
            binding.launcherLabel.beVisibleIf(activity.config.showDrawerAppLabels)
            
            // Explicitly resetting alpha and scale handles RecyclerView glitches
            binding.launcherIcon.drawable?.alpha = LAUNCHER_ALPHA_NORMAL
            itemView.scaleX = LAUNCHER_SCALE_NORMAL
            itemView.scaleY = LAUNCHER_SCALE_NORMAL
            
            if (launcher.drawable != null) {
                binding.launcherIcon.setImageDrawable(launcher.drawable)
            } else {
                val placeholderDrawable = activity.resources.getColoredDrawableWithColor(
                    drawableId = R.drawable.placeholder_drawable,
                    color = launcher.thumbnailColor
                )
                binding.launcherIcon.setImageDrawable(placeholderDrawable)
            }
        }
    }

    override fun onChange(position: Int) = currentList.getOrNull(position)?.getBubbleText() ?: ""

    companion object {
        private const val LAUNCHER_SCALE_NORMAL = 1f
        private const val LAUNCHER_SCALE_PRESSED = 1.15f
        private const val LAUNCHER_SCALE_UP_DURATION = 100L
        private const val LAUNCHER_SCALE_DOWN_DURATION = 50L
        private const val LAUNCHER_ALPHA_NORMAL = 255
        private const val LAUNCHER_ALPHA_PRESSED = 220
    }
}

private class AppLauncherDiffCallback : DiffUtil.ItemCallback<AppLauncher>() {
    override fun areItemsTheSame(oldItem: AppLauncher, newItem: AppLauncher): Boolean {
        return oldItem.getLauncherIdentifier() == newItem.getLauncherIdentifier()
    }

    override fun areContentsTheSame(oldItem: AppLauncher, newItem: AppLauncher): Boolean {
        return oldItem.title == newItem.title &&
                oldItem.order == newItem.order &&
                oldItem.thumbnailColor == newItem.thumbnailColor &&
                (oldItem.drawable == null) == (newItem.drawable == null)
    }
}
