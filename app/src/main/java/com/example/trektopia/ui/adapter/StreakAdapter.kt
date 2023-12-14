package com.example.trektopia.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trektopia.R
import com.example.trektopia.core.model.operation.StreakHistory
import com.example.trektopia.databinding.ItemStreakBinding
import com.example.trektopia.utils.DateHelper

class StreakAdapter : ListAdapter<StreakHistory, StreakAdapter.ItemViewHolder>(DIFF_CALLBACK){

    inner class ItemViewHolder(private var binding: ItemStreakBinding) : RecyclerView.ViewHolder (binding.root){
        fun bind(history: StreakHistory){
            val date = DateHelper.timeStampToLocalDate(history.date)
            binding.apply {
                tvStreakDate.text = DateHelper.formatDateMonth(date)
                tvStreakDay.text = DateHelper.formatDayOfWeek(date).substring(0,2)

                val indicator = if(history.active) R.drawable.bg_streak_tertiary_8
                else R.drawable.bg_streak_outlined_8

                tvStreakDay.setBackgroundResource(indicator)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemStreakBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val history = getItem(position)
        if(history!=null){
            holder.bind(history)
        }
    }

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<StreakHistory> =
            object : DiffUtil.ItemCallback<StreakHistory>() {
                override fun areItemsTheSame(oldItem: StreakHistory, newItem: StreakHistory): Boolean {
                    return oldItem.date == newItem.date
                }

                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(oldItem: StreakHistory, newItem: StreakHistory): Boolean {
                    return oldItem == newItem
                }
            }
    }

}