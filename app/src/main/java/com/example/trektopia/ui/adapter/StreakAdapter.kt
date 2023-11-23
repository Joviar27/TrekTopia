package com.example.trektopia.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trektopia.R
import com.example.trektopia.databinding.ItemStreakBinding
import com.example.trektopia.utils.DateHelper
import com.google.firebase.Timestamp

class StreakAdapter : ListAdapter<Pair<Boolean,Timestamp>, StreakAdapter.ItemViewHolder>(DIFF_CALLBACK){

    inner class ItemViewHolder(private var binding: ItemStreakBinding) : RecyclerView.ViewHolder (binding.root){
        fun bind(history: Pair<Boolean,Timestamp>){
            val date = DateHelper.timeStampToLocalDate(history.second)
            binding.apply {
                tvSteakDate.text = DateHelper.formatDateMonth(date)
                tvStreakDay.text = DateHelper.formatDayOfWeek(date)

                val indicator = if(history.first){
                    //TODO("Set indicator if there's activity")
                    ContextCompat.getDrawable(itemView.context, R.drawable.ic_arrow_back_24)

                } else{
                    //TODO("Set indicator if there's activity")
                    ContextCompat.getDrawable(itemView.context, R.drawable.ic_home_24)
                }
                ivStreakIndicator.setImageDrawable(indicator)
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
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Pair<Boolean,Timestamp>> =
            object : DiffUtil.ItemCallback<Pair<Boolean,Timestamp>>() {
                override fun areItemsTheSame(oldItem: Pair<Boolean,Timestamp>, newItem: Pair<Boolean,Timestamp>): Boolean {
                    return oldItem.second == newItem.second
                }

                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(oldItem: Pair<Boolean,Timestamp>, newItem: Pair<Boolean,Timestamp>): Boolean {
                    return oldItem == newItem
                }
            }
    }

}