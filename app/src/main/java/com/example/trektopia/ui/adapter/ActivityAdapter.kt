package com.example.trektopia.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.core.model.Activity
import com.example.trektopia.databinding.ItemActivityBinding
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.getStaticMapUri

class ActivityAdapter (
    private val onClick :(activity: Activity) -> Unit
): ListAdapter<Activity, ActivityAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    inner class ItemViewHolder(private var binding: ItemActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(activity: Activity) {

            binding.apply {
                //TODO: Create task map route placeholder
                Glide.with(itemView.context)
                    .load(activity.route.getStaticMapUri())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivActivityRoute)

                tvActivityDate.text = DateHelper.formatDateMonthYear(
                    DateHelper.timeStampToLocalDate(activity.timeStamp)
                )

                //TODO: Create placeholder string format
                tvActivityDistance.text = activity.distance.toString()
                tvActivityDuration.text = activity.duration.toString()
                tvActivitySpeed.text = activity.speed.toString()
                tvActivityStep.text = activity.stepCount.toString()

                itemView.setOnClickListener {
                    onClick(activity)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val activity = getItem(position)
        if (activity != null) {
            holder.bind(activity)
        }
    }

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Activity> =
            object : DiffUtil.ItemCallback<Activity>() {
                override fun areItemsTheSame(oldItem: Activity, newItem: Activity): Boolean {
                    return oldItem.id == newItem.id
                }

                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(oldItem: Activity, newItem: Activity): Boolean {
                    return oldItem == newItem
                }
            }
    }
}