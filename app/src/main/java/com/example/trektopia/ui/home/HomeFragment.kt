package com.example.trektopia.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.User
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.databinding.FragmentHomeBinding
import com.example.trektopia.ui.adapter.StreakAdapter
import com.example.trektopia.ui.adapter.TaskAdapter
import com.example.trektopia.utils.DateHelper
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: HomeViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var streakAdapter: StreakAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStreakRV()
        setupMissionsRV()
        observeUserData()
        observeMissionsData()
    }

    private fun setupUserView(user: User){
        binding?.apply {
            //TODO: Add placeholder
            Glide.with(requireActivity())
                .load(user.pictureUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding!!.ivUserPic)

            tvUserName.text = user.username
            tvUserPoint.text = user.point.toString()
            tvUserStreakCount.text = user.dailyStreak.count.toString()
            tvUserStreakLongest.text = user.dailyStreak.longest.toString()
            streakAdapter.submitList(user.dailyStreak.history)
        }
    }

    private fun setupStreakRV(){
        streakAdapter = StreakAdapter()
        val layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            true
        )
        binding?.rvUserStreakHistory?.apply {
            adapter = streakAdapter
            setLayoutManager(layoutManager)
        }
    }

    private fun setupMissionsRV(){
        taskAdapter = TaskAdapter{relationId, reward ->
            claimReward(relationId,reward)
        }
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.rvTask?.apply {
            adapter = taskAdapter
            setLayoutManager(layoutManager)
        }
    }

    private fun claimReward(relationId: String, reward: Int){
        viewModel.claimTaskReward(relationId, reward).observe(requireActivity()){claimResult ->
            when(claimResult){
                is ResultState.Success -> TODO("Give success message")
                is ResultState.Error -> TODO("Give error message")
                else -> Unit
            }
        }
    }

    private fun observeUserData(){
        viewModel.user.observe(requireActivity()){ userResult ->
            when(userResult){
                is ResultState.Loading -> loadingUser(true)
                is ResultState.Success -> {
                    loadingUser(false)
                    setupUserView(userResult.data)
                }
                is ResultState.Error -> {
                    loadingUser(false)
                    TODO("Handle error")
                }
            }
        }
    }

    private fun observeMissionsData(){
        viewModel.missions.observe(requireActivity()){ missionsResult ->
            when(missionsResult){
                is ResultState.Loading -> loadingMissions(true)
                is ResultState.Success -> {
                    loadingMissions(false)
                    taskAdapter.submitList(missionsResult.data)
                    setupCountdown(missionsResult.data[0].activeDate
                        ?: missionsResult.data[1].activeDate!!
                    )
                }
                is ResultState.Error -> {
                    loadingMissions(false)
                    TODO("Handle error")
                }
            }
        }
    }

    private fun setupCountdown(missionActiveTimestamp: Timestamp){
        val missionActiveDate = DateHelper.timeStampToLocalDate(
            missionActiveTimestamp
        )
        binding?.tvTaskCountdown?.text =
            missionHourCountdown(missionActiveDate).toString()
    }

    private fun missionHourCountdown(missionActiveDate: LocalDate): Int {
        val endOfMissionActive = LocalDateTime.of(missionActiveDate, LocalTime.MAX)
        val remainingMillis = ChronoUnit.MILLIS.between(LocalDate.now(), endOfMissionActive)
        return (remainingMillis / 3600000).toInt()
    }

    private fun loadingUser(isLoading: Boolean){
        TODO("Manage user loading")
    }

    private fun loadingMissions(isLoading: Boolean){
        TODO("Manage missions loading")
    }

}