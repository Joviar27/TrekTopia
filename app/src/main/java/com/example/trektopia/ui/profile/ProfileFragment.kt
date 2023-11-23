package com.example.trektopia.ui.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.User
import com.example.trektopia.databinding.FragmentProfileBinding
import com.example.trektopia.ui.adapter.TaskAdapter
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.obtainViewModel
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: ProfileViewModel
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAchievementsRV()
        observeUserData()
        observeMissionsData()
    }

    private fun setupUserView(user: User) {
        binding?.let { bind ->
            with(bind) {
                //TODO: Add placeholder
                Glide.with(requireActivity())
                    .load(user.pictureUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivUserPic)

                showOriginalButtonViews()

                tvUserName.text = user.username
                tvUserPoint.text = user.point.toString()

                setEditButtonClickListener(user)
                setSaveButtonClickListener(user)

                btnLogout.setOnClickListener {
                    viewModel.logout()
                }
            }
        }
    }

    private fun updateProfilePicture(){
        TODO("Pick image from gallery")
    }

    private fun setEditButtonClickListener(user: User) {
        binding?.apply {
            btnEditUsername.setOnClickListener {
                btnEditUsername.visibility = View.GONE
                tvUserName.visibility = View.GONE

                btnSaveEdit.visibility = View.VISIBLE
                edtUserName.visibility = View.VISIBLE

                edtUserName.hint = user.username
            }
        }
    }

    private fun setSaveButtonClickListener(user: User) {
        binding?.apply {
            btnSaveEdit.setOnClickListener {
                val newUsername = edtUserName.text.toString()
                val newUser = user.copy(username = newUsername)

                viewModel.updateUserInfo(newUser).observe(requireActivity()) { updateResult ->
                    handleUpdateResult(updateResult)
                }
            }
        }
    }

    private fun handleUpdateResult(updateResult: ResultState<Unit>) {
        when (updateResult) {
            is ResultState.Loading -> TODO("Manage Loading")
            is ResultState.Success -> showOriginalButtonViews()
            is ResultState.Error -> TODO("Manage Error")
        }
    }

    private fun showOriginalButtonViews() {
        binding?.apply {
            btnEditUsername.visibility = View.VISIBLE
            tvUserName.visibility = View.VISIBLE

            btnSaveEdit.visibility = View.GONE
            edtUserName.visibility = View.GONE
        }
    }

    private fun setupAchievementsRV(){
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
        viewModel.achievements.observe(requireActivity()){ missionsResult ->
            when(missionsResult){
                is ResultState.Loading -> loadingAchievements(true)
                is ResultState.Success -> {
                    loadingAchievements(false)
                    taskAdapter.submitList(missionsResult.data)
                }
                is ResultState.Error -> {
                    loadingAchievements(false)
                    TODO("Handle error")
                }
            }
        }
    }

    private fun loadingUser(isLoading: Boolean){
        TODO("Manage user loading")
    }

    private fun loadingAchievements(isLoading: Boolean){
        TODO("Manage missions loading")
    }

}