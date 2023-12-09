package com.example.trektopia.ui.profile

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.databinding.FragmentFullAchievementsBinding
import com.example.trektopia.databinding.FragmentProfileBinding
import com.example.trektopia.ui.adapter.TaskAdapter
import com.example.trektopia.ui.dialog.StatusDialog
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.showToast
import com.google.android.material.divider.MaterialDividerItemDecoration

class FullAchievementsFragment : Fragment() {

    private var _binding: FragmentFullAchievementsBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: ProfileViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var statusDialog: StatusDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFullAchievementsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAchievementsRV()
        observeMissionsData()
    }

    private fun setupAchievementsRV(){
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

    private fun observeMissionsData(){
        viewModel.achievements.observe(requireActivity()){ missionsResult ->
            when(missionsResult){
                is ResultState.Loading -> Unit //showLoadingBar(true)
                is ResultState.Success -> {
                    //showLoadingBar(false)
                    taskAdapter.submitList(missionsResult.data)
                }
                is ResultState.Error -> {
                    //showLoadingBar(false)
                    resources.getString(R.string.page_failed_load).showToast(requireContext())
                }
            }
        }
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
}