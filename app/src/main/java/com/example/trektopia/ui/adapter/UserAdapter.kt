package com.example.trektopia.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.core.model.User
import com.example.trektopia.databinding.ItemUserBinding

class UserAdapter : ListAdapter<Pair<Int,User>, UserAdapter.ItemViewHolder>(DIFF_CALLBACK) {

    inner class ItemViewHolder(private var binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: Pair<Int,User>) {

            binding.apply {
                //TODO: Create task map route placeholder
                Glide.with(itemView.context)
                    .load(user.second.pictureUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivUserPic)

                tvUserRank.text = user.first.toString()
                tvUserName.text = user.second.username
                tvUserPoint.text = user.second.point.toString()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val user = getItem(position)
        if (user != null) {
            holder.bind(user)
        }
    }

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Pair<Int,User>> =
            object : DiffUtil.ItemCallback<Pair<Int,User>>() {
                override fun areItemsTheSame(oldItem: Pair<Int,User>, newItem: Pair<Int,User>): Boolean {
                    return oldItem.second.uid == newItem.second.uid
                }

                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(oldItem: Pair<Int,User>, newItem: Pair<Int,User>): Boolean {
                    return oldItem == newItem
                }
            }
    }
}