package com.example.trektopia.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.User
import com.example.trektopia.databinding.FragmentProfileBinding
import com.example.trektopia.ui.adapter.TaskAdapter
import com.example.trektopia.ui.dialog.StatusDialog
import com.example.trektopia.utils.createCustomDrawable
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.safeNavigate
import com.example.trektopia.utils.showToast
import com.google.android.material.divider.MaterialDividerItemDecoration

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: ProfileViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var statusDialog: StatusDialog

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            uploadNewImage(uri)
        } else {
            resources.getString(R.string.gallery_failed).showToast(requireContext())
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launcherIntentGallery.launch("image/*")
        } else {
            resources.getString(R.string.gallery_not_permitted)
        }
    }


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

            showOriginalButtonViews()

            tvUserName.text = user.username
            tvUserEmail.text = user.email
            tvUserPoint.text = user.point.toString()

            setEditButtonClickListener(user)
            setSaveButtonClickListener(user)

            btnUpdateProfile.setOnClickListener {
                updateProfilePicture()
            }

            btnToAchievements.setOnClickListener{
                val toFull = ProfileFragmentDirections.actionProfileFragmentToFullAchievementsFragment()
                view?.findNavController()?.safeNavigate(toFull)
            }
        }
    }

    private fun updateProfilePicture() {
        if (isGalleryPermissionGranted()) {
            launcherIntentGallery.launch("image/*")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun isGalleryPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun uploadNewImage(newUri: Uri){
        viewModel.updateProfile(newUri).observe(requireActivity()){result ->
            when(result){
                is ResultState.Loading -> showLoading(
                    resources.getString(R.string.dialog_loading_update_profile)
                )
                is ResultState.Success -> showSuccess(
                    resources.getString(R.string.dialog_success_update_profile)
                )
                is ResultState.Error -> showFailed(
                    resources.getString(R.string.dialog_fail_update_profile)
                )
            }
        }
    }

    private fun setEditButtonClickListener(user: User) {
        binding?.apply {
            btnEditUsername.setOnClickListener {
                btnEditUsername.visibility = View.INVISIBLE
                tvUserName.visibility = View.INVISIBLE

                btnSaveUsername.visibility = View.VISIBLE
                edtUserName.visibility = View.VISIBLE

                edtUserName.hint = user.username
            }
        }
    }

    private fun setSaveButtonClickListener(user: User) {
        binding?.btnSaveUsername?.setOnClickListener {
            val newUsername = binding?.edtUserName?.text.toString()
            if(newUsername.isNotEmpty()){
                val newUser = user.copy(username = newUsername)
                viewModel.updateUserInfo(newUser).observe(requireActivity()) { updateResult ->
                    handleUpdateResult(updateResult)
                }
            }
            showOriginalButtonViews()
        }
    }

    private fun handleUpdateResult(updateResult: ResultState<Unit>) {
        when(updateResult){
            is ResultState.Loading -> showLoading(
                resources.getString(R.string.dialog_loading_update)
            )
            is ResultState.Success -> showSuccess(
                resources.getString(R.string.dialog_success_update)
            )
            is ResultState.Error -> showFailed(
                resources.getString(R.string.dialog_fail_update)
            )
        }
    }

    private fun showOriginalButtonViews() {
        binding?.apply {
            btnEditUsername.visibility = View.VISIBLE
            tvUserName.visibility = View.VISIBLE

            btnSaveUsername.visibility = View.INVISIBLE
            edtUserName.visibility = View.INVISIBLE

            edtUserName.text.clear()
        }
    }

    private fun setupAchievementsRV(){
        taskAdapter = TaskAdapter{relationId, reward ->
            claimReward(relationId,reward)
        }
        taskAdapter.setLimit()

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

    private fun showLoadingBar(isLoading: Boolean){
        binding?.pbLoading?.visibility = if(isLoading) View.VISIBLE else View.GONE
    }

}