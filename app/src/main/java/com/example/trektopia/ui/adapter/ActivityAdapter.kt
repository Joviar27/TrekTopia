package com.example.trektopia.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.trektopia.R
import com.example.trektopia.core.model.Activity
import com.example.trektopia.databinding.ItemActivityBinding
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.completeStaticMapUri

class ActivityAdapter (
    private val onClick :(activity: Activity) -> Unit
): ListAdapter<Activity, ActivityAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    inner class ItemViewHolder(private var binding: ItemActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(activity: Activity) {
            binding.apply {
                Glide.with(itemView.context).asBitmap()
                    .load(activity.route.completeStaticMapUri(itemView.context))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.color.white)
                    .into(object : CustomTarget<Bitmap>(){
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            binding.apply {
                                ivActivityRoute.setImageBitmap(resource)
                                ivActivityRoute.visibility = View.VISIBLE
                                pbAvtivityRoute.visibility= View.GONE
                            }
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                            binding.ivActivityRoute.setImageDrawable(placeholder)
                        }
                    })

                tvActivityDate.text = DateHelper.formatDateMonthYear(
                    DateHelper.timeStampToLocalDate(activity.timeStamp)
                )

                distanceInfo.tvActivityInfo.text = activity.distance.toString()
                distanceInfo.tvInfoType.text = itemView.context.resources.getString(R.string.km)

                tvRecapDuration.text = DateHelper.formatElapsedTime(activity.duration)

                speedInfo.tvActivityInfo.text = activity.speed.toString()
                speedInfo.tvInfoType.text = itemView.context.resources.getString(R.string.km_h)

                stepInfo.tvActivityInfo.text = itemView.context.resources.getString(
                    R.string.step_count,
                    activity.stepCount.div(1000).toString()
                )
                stepInfo.tvInfoType.text = itemView.context.resources.getString(R.string.steps)

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