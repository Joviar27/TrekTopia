package com.example.trektopia.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.core.model.operation.TaskWithProgress
import com.example.trektopia.databinding.ItemTaskBinding

class TaskAdapter (
    private val onClaim :(
        relationId: String,
        reward: Int,
    ) -> Unit
): ListAdapter<TaskWithProgress, TaskAdapter.ItemViewHolder>(DIFF_CALLBACK){

    inner class ItemViewHolder(private var binding: ItemTaskBinding) : RecyclerView.ViewHolder (binding.root){
        fun bind(taskWithProgress: TaskWithProgress){

            binding.apply {
                //TODO: Create task pic placeholder
                Glide.with(itemView.context)
                    .load(taskWithProgress.task.pictureUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivTaskPic)

                tvTaskName.text = taskWithProgress.task.name
                //TODO: Create string placeholder to display info
                tvTaskReward.text = taskWithProgress.task.reward.toString()
                tvTaskPercentage.text = taskWithProgress.progress.percentage.toString()
                tvTaskProgress.text = "${taskWithProgress.progress.current} / ${taskWithProgress.task.requirement}"

                btnTaskClaimReward.isEnabled = !taskWithProgress.progress.enabled

                btnTaskClaimReward.setOnClickListener {
                    onClaim(
                        taskWithProgress.relationId,
                        taskWithProgress.task.reward
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val taskWithProgress = getItem(position)
        if(taskWithProgress!=null){
            holder.bind(taskWithProgress)
        }
    }

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<TaskWithProgress> =
            object : DiffUtil.ItemCallback<TaskWithProgress>() {
                override fun areItemsTheSame(oldItem: TaskWithProgress, newItem: TaskWithProgress): Boolean {
                    return oldItem.relationId == newItem.relationId
                }

                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(oldItem: TaskWithProgress, newItem: TaskWithProgress): Boolean {
                    return oldItem == newItem
                }
            }
    }
}