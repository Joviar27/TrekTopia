package com.example.trektopia.ui.leaderboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.databinding.FragmentLeaderboardBinding
import com.example.trektopia.ui.adapter.UserAdapter
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.core.model.User
import com.example.trektopia.utils.getStaticMapUri
import com.example.trektopia.utils.showToast

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: LeaderboardViewModel
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserRV()
        observeLeaderboardData()
        observerUserDate()
    }

    private fun setupUserRV(){
        userAdapter = UserAdapter()
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.rvLeaderboard?.apply {
            adapter = userAdapter
            setLayoutManager(layoutManager)
        }
    }

    private fun setupUser(user: User){
        viewModel.getCurrentUserRank(user.point).observe(requireActivity()){rankResult ->
            val rank = when(rankResult){
                is ResultState.Success ->
                    rankResult.data.toString()
                else -> "0"
            }
            binding?.itemCurrentUser?.tvUserRank?.text = rank
        }

        binding?.apply {
            //TODO: Placeholder
            Glide.with(requireActivity())
                .load(user.pictureUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(itemCurrentUser.ivUserPic)

            itemCurrentUser.tvUserName.text = user.username
            itemCurrentUser.tvUserPoint.text = user.point.toString()
        }
    }

    private fun observeLeaderboardData(){
        viewModel.leaderboard.observe(requireActivity()){leaderboardResult ->
            when(leaderboardResult){
                is ResultState.Loading -> loading(true)
                is ResultState.Success ->{
                    loading(false)
                    userAdapter.submitList(leaderboardResult.data)
                }
                is ResultState.Error -> {
                    loading(false)
                    resources.getString(R.string.page_failed_load).showToast(requireContext())
                }
            }
        }
    }

    private fun observerUserDate(){
        viewModel.currentUser.observe(requireActivity()){userResult ->
            when (userResult) {
                is ResultState.Loading -> Unit
                is ResultState.Success -> {
                    setupUser(userResult.data)
                }
                is ResultState.Error -> {
                    resources.getString(R.string.page_failed_load).showToast(requireContext())
                }
            }
        }
    }

    private fun loading(isLoading: Boolean){
        binding?.pbLoading?.visibility = if(isLoading) View.VISIBLE else View.GONE
    }
}