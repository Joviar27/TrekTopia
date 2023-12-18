package com.example.trektopia.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.User
import com.example.trektopia.databinding.FragmentHomeBinding
import com.example.trektopia.service.AlarmReceiver
import com.example.trektopia.ui.DividerItemDecorator
import com.example.trektopia.ui.adapter.StreakAdapter
import com.example.trektopia.ui.adapter.TaskAdapter
import com.example.trektopia.ui.dialog.StatusDialog
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.createCustomDrawable
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.showToast
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

    private lateinit var loadingStatusDialog: StatusDialog
    private lateinit var failedStatusDialog: StatusDialog
    private lateinit var successStatusDialog: StatusDialog

    private lateinit var receiver: AlarmReceiver

    private val requestSinglePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupDailyReminder()
        } else {
            resources.getString(R.string.permssion_notif_denied).showToast(requireContext())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel(requireContext())

        receiver = AlarmReceiver()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                setupDailyReminder()
            } else {
                requestSinglePermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        } else {
            setupDailyReminder()
        }
        setupDailyReset()
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
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
            if(user.pictureUri==null){
                val custom = user.username[0]
                    .uppercaseChar()
                    .createCustomDrawable(requireContext())
                ivUserPic.setImageDrawable(custom)
            } else{
                Glide.with(requireActivity())
                    .load(user.pictureUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.color.secondary)
                    .into(ivUserPic)
            }

            tvUserName.text = resources.getString(R.string.greeting, user.username)
            tvUserPoint.text = resources.getString(R.string.point, user.point.toString())
            layoutStreak.tvUserStreakCount.text = resources.getString(
                R.string.current_streak,
                user.dailyStreak.count.toString()
            )
            layoutStreak.tvUserStreakLongest.text = resources.getString(
                R.string.longest_streak,
                user.dailyStreak.longest.toString()
            )
            streakAdapter.submitList(user.dailyStreak.history?.reversed())
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

        val dividerItemDecoration: ItemDecoration = DividerItemDecorator(
            ContextCompat.getDrawable(requireContext(), R.drawable.divider)
        )

        binding?.rvTask?.apply {
            adapter = taskAdapter
            setLayoutManager(layoutManager)
            addItemDecoration(dividerItemDecoration)
        }
    }

    private fun setupDailyReminder(){
        val reminderStarted = viewModel.getNotifStatus()

        if (!reminderStarted) {
            receiver.setReminderAlarm(
                requireActivity(),
                7,
                AlarmReceiver.SHOW_NOTIF_ACTION,
                AlarmReceiver.REMINDER_ALARM_ID_1
            )
            receiver.setReminderAlarm(
                requireActivity(),
                16,
                AlarmReceiver.SHOW_NOTIF_ACTION,
                AlarmReceiver.REMINDER_ALARM_ID_2
            )

            viewModel.setNotifStatus(true)
        }
    }

    private fun setupDailyReset(){
        val resetStarted = viewModel.getResetStatus()

        if (!resetStarted) {
            receiver.setReminderAlarm(
                requireContext(),
                0,
                AlarmReceiver.DAILY_RESET_ACTION,
                AlarmReceiver.RESET_ALARM_ID
            )

            viewModel.setResetStatus(true)
        }
    }

    private fun claimReward(relationId: String, reward: Int){
        viewModel.claimTaskReward(relationId, reward).observe(requireActivity()){claimResult ->
            when(claimResult){
                is ResultState.Loading -> showLoading()
                is ResultState.Error ->{
                    showFailed()
                    dismissLoading()
                }
                is ResultState.Success ->{
                    showSuccess(reward)
                    dismissLoading()

                }
            }
        }
    }

    private fun observeUserData(){
        viewModel.user.observe(requireActivity()){ userResult ->
            when(userResult){
                is ResultState.Loading -> Unit
                is ResultState.Success -> {
                    setupUserView(userResult.data)
                }
                is ResultState.Error -> {
                    resources.getString(R.string.page_failed_load).showToast(requireContext())
                }
            }
        }
    }

    private fun observeMissionsData(){
        viewModel.missions.observe(requireActivity()){ missionsResult ->
            when(missionsResult){
                is ResultState.Loading -> Unit
                is ResultState.Success -> {
                    taskAdapter.submitList(missionsResult.data)
                    if(missionsResult.data.isNotEmpty()){
                        setupCountdown(missionsResult.data[0].activeDate
                            ?: missionsResult.data[1].activeDate!!
                        )
                    }
                }
                is ResultState.Error -> {
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
            resources.getString(
                R.string.countdown,
                missionHourCountdown(missionActiveDate)
            )
    }

    private fun missionHourCountdown(missionActiveDate: LocalDate): Int {
        val endOfMissionActive = LocalDateTime.of(missionActiveDate, LocalTime.MAX)
        val remainingMillis = ChronoUnit.MILLIS.between(LocalTime.now(), endOfMissionActive)
        return (remainingMillis / 3600000).toInt()
    }

    private fun showLoading(){
        loadingStatusDialog = StatusDialog.newInstance(
            R.drawable.ic_loading,
            resources.getString(R.string.dialog_loading_claim),
        )
        loadingStatusDialog.show(childFragmentManager, "LoadingStatusDialog")
    }

    private fun dismissLoading(){
        loadingStatusDialog.dismiss()
    }

    private fun showSuccess(reward: Int){
        successStatusDialog =  StatusDialog.newInstance(
            R.drawable.ic_success,
            resources.getString(R.string.dialog_success_claim, reward.toString()),
        )
        successStatusDialog.show(childFragmentManager, "SuccessStatusDialog")

        Handler(Looper.getMainLooper()).postDelayed({
            successStatusDialog.dismiss()
        }, 2000L)
    }

    private fun showFailed(){
        failedStatusDialog = StatusDialog.newInstance(
            R.drawable.ic_error,
            resources.getString(R.string.dialog_fail_claim),
        )
        failedStatusDialog.show(childFragmentManager, "SuccessStatusDialog")

        Handler(Looper.getMainLooper()).postDelayed({
            failedStatusDialog.dismiss()
        }, 2000L)
    }
}