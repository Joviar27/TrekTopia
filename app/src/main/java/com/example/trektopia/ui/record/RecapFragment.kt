package com.example.trektopia.ui.record

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.Activity
import com.example.trektopia.databinding.FragmentRecapBinding
import com.example.trektopia.ui.dialog.StatusDialog
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.completeStaticMapUri
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.safeNavigate

class RecapFragment : Fragment() {
    private var _binding: FragmentRecapBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: RecordViewModel

    private lateinit var loadingStatusDialog: StatusDialog
    private lateinit var failedStatusDialog: StatusDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecapBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activityDetail = RecapFragmentArgs.fromBundle(arguments as Bundle).activity
        setupView(activityDetail)
    }

    private fun setupView(activity: Activity){
        binding?.apply {
            Glide.with(requireActivity()).asBitmap()
                .load(activity.route.completeStaticMapUri(requireContext()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        binding.apply {
                            ivRecapRoute.setImageBitmap(resource)
                            pbRecapRoute.visibility = View.GONE
                        }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        ivRecapRoute.setImageDrawable(placeholder)
                    }
                })

            tvRecapDate.text = DateHelper.formatDateMonthYear(
                DateHelper.timeStampToLocalDate(activity.timeStamp)
            )

            tvRecapDay.text = DateHelper.formatDayOfWeek(
                DateHelper.timeStampToLocalDate(activity.timeStamp)
            )

            tvRecapTime.text  = resources.getString(
                R.string.start_stop_format,
                DateHelper.formatTime(
                    DateHelper.timeStampToLocalDateTime(activity.startTime)
                ),
                DateHelper.formatTime(
                    DateHelper.timeStampToLocalDateTime(activity.timeStamp)
                )
            )

            recapDistance.tvLiveInfo.text = String.format("%.1f", activity.distance)
            recapDistance.tvLiveType.text = resources.getString(R.string.km)

            tvRecapDuration.text = DateHelper.formatElapsedTime(activity.duration)

            recapSpeed.tvLiveInfo.text = String.format("%.1f", activity.speed)
            recapSpeed.tvLiveType.text = resources.getString(R.string.km_h)

            recapSteps.tvLiveInfo.text = String.format("%.1f", (activity.stepCount)/100.0)
            recapSteps.tvLiveType.text = resources.getString(R.string.live_steps)

            btnSaveRecord.setOnClickListener {
                viewModel.saveRecord(activity).observe(requireActivity()){result ->
                    when(result){
                        is ResultState.Loading -> showLoading()
                        is ResultState.Success ->{
                            dismissLoading()
                            val toHistory = RecapFragmentDirections.actionRecapFragmentToHistoryFragment()
                            view?.findNavController()?.safeNavigate(toHistory)
                        }
                        is ResultState.Error -> {
                            showFailed()
                            dismissLoading()
                        }
                    }
                }
            }

            btnDeleteRecord.setOnClickListener {
                val toHome = RecapFragmentDirections.actionRecapFragmentToHomeFragment()
                view?.findNavController()?.safeNavigate(toHome)
            }
        }
    }

    private fun showLoading(){
        loadingStatusDialog = StatusDialog.newInstance(
            R.drawable.ic_loading,
            resources.getString(R.string.dialog_loading_activity),
        )
        loadingStatusDialog.show(childFragmentManager, "LoadingStatusDialog")
    }

    private fun dismissLoading(){
        loadingStatusDialog.dismiss()
    }

    private fun showFailed(){
        failedStatusDialog = StatusDialog.newInstance(
            R.drawable.ic_error,
            resources.getString(R.string.dialog_fail_activity),
        )
        failedStatusDialog.show(childFragmentManager, "SuccessStatusDialog")

        Handler(Looper.getMainLooper()).postDelayed({
            failedStatusDialog.dismiss()
        }, 1000L)
    }
}