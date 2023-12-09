package com.example.trektopia.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.User
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.databinding.FragmentHomeBinding
import com.example.trektopia.ui.adapter.StreakAdapter
import com.example.trektopia.ui.adapter.TaskAdapter
import com.example.trektopia.ui.dialog.StatusDialog
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.showToast
import com.google.android.material.divider.MaterialDividerItemDecoration
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
    private lateinit var statusDialog: StatusDialog

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
            Glide.with(requireActivity())
                .load(user.pictureUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.color.secondary_container)
                .into(binding!!.ivUserPic)

            tvUserName.text = user.username
            tvUserPoint.text = resources.getString(R.string.point, user.point.toString())
            layoutStreak.tvUserStreakCount.text = resources.getString(
                R.string.current_streak,
                user.dailyStreak.count.toString()
            )
            layoutStreak.tvUserStreakLongest.text = resources.getString(
                R.string.longest_streak,
                user.dailyStreak.longest.toString()
            )
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

        binding?.layoutStreak?.rvUserStreakHistory?.apply {
            adapter = streakAdapter
            setLayoutManager(layoutManager)
        }
    }

    private fun setupMissionsRV(){
        taskAdapter = TaskAdapter{relationId, reward ->
            claimReward(relationId,reward)
        }

        val layoutManager = LinearLayoutManager(requireContext())

        val materialDividerItemDecoration =
            MaterialDividerItemDecoration(requireContext(), layoutManager.orientation)
        materialDividerItemDecoration.dividerColor =
            ContextCompat.getColor(requireContext(), R.color.secondary)
        materialDividerItemDecoration.dividerThickness =
            resources.getDimension(R.dimen.divider_width).toInt()

        binding?.rvTask?.apply {
            adapter = taskAdapter
            setLayoutManager(layoutManager)
            addItemDecoration(materialDividerItemDecoration)
        }
    }

    private fun claimReward(relationId: String, reward: Int){
        viewModel.claimTaskReward(relationId, reward).observe(requireActivity()){claimResult ->
            when(claimResult){
                is ResultState.Loading -> showLoading(
                    resources.getString(R.string.dialog_loading_claim)
                )
                is ResultState.Error -> showSuccess(
                    resources.getString(R.string.dialog_fail_claim)
                )
                is ResultState.Success -> showLoading(
                    resources.getString(R.string.dialog_success_claim)
                )
            }
        }
    }

    private fun observeUserData(){
        viewModel.user.observe(requireActivity()){ userResult ->
            when(userResult){
                is ResultState.Loading -> Unit //showLoadingBar(true)
                is ResultState.Success -> {
                    //showLoadingBar(false)
                    setupUserView(userResult.data)
                }
                is ResultState.Error -> {
                    //showLoadingBar(false)
                    resources.getString(R.string.page_failed_load).showToast(requireContext())
                }
            }
        }
    }

    private fun observeMissionsData(){
        viewModel.missions.observe(requireActivity()){ missionsResult ->
            when(missionsResult){
                is ResultState.Loading -> Unit //showLoadingBar(true)
                is ResultState.Success -> {
                    //showLoadingBar(false)
                    taskAdapter.submitList(missionsResult.data)
                    setupCountdown(missionsResult.data[0].activeDate
                        ?: missionsResult.data[1].activeDate!!
                    )
                }
                is ResultState.Error -> {
                    //showLoadingBar(false)
                    resources.getString(R.string.page_failed_load).showToast(requireContext())
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

    private fun showLoading(message: String){
        statusDialog = StatusDialog.newInstance(
            R.drawable.ic_loading,
            message,
        )
        statusDialog.show(childFragmentManager, "LoadingStatusDialog")
    }

    private fun showSuccess(message: String){
        statusDialog = StatusDialog.newInstance(
            R.drawable.ic_success,
            message,
        )
        statusDialog.show(childFragmentManager, "SuccessStatusDialog")

        Handler(Looper.getMainLooper()).postDelayed({
            statusDialog.dismiss()
        }, 1000L)
    }

    private fun showFailed(message: String){
        statusDialog = StatusDialog.newInstance(
            R.drawable.ic_error,
            message,
        )
        statusDialog.show(childFragmentManager, "SuccessStatusDialog")

        Handler(Looper.getMainLooper()).postDelayed({
            statusDialog.dismiss()
        }, 1000L)
    }

    private fun showLoadingBar(isLoading: Boolean){
        binding?.pbLoading?.visibility = if(isLoading) View.VISIBLE else View.GONE
    }
}