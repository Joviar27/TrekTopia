package com.example.trektopia.ui.record

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
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.Activity
import com.example.trektopia.databinding.FragmentRecapBinding
import com.example.trektopia.ui.dialog.StatusDialog
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.getStaticMapUri
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.safeNavigate

class RecapFragment : Fragment() {
    private var _binding: FragmentRecapBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: RecordViewModel
    private lateinit var statusDialog: StatusDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel()
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
            //TODO: Create task map route placeholder
            Glide.with(requireActivity())
                .load(activity.route.getStaticMapUri(requireContext()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivRecapRoute)

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

            recapDistance.tvLiveInfo.text = activity.distance.toString()
            recapDistance.tvLiveType.text = resources.getString(R.string.km)

            tvRecapDuration.text = DateHelper.formatElapsedTime(activity.duration)

            recapSpeed.tvLiveInfo.text = activity.speed.toString()
            recapSpeed.tvLiveType.text = resources.getString(R.string.km_h)

            recapSteps.tvLiveInfo.text = activity.stepCount.toString()
            recapSteps.tvLiveType.text = resources.getString(R.string.live_steps)

            btnSaveRecord.setOnClickListener {
                viewModel.saveRecord(activity).observe(requireActivity()){result ->
                    when(result){
                        is ResultState.Loading -> showLoading()
                        is ResultState.Success ->{
                            statusDialog.dismiss()
                            val toHistory = RecapFragmentDirections.actionRecapFragmentToHistoryFragment()
                            view?.findNavController()?.safeNavigate(toHistory)
                        }
                        is ResultState.Error -> showFailed()
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
        statusDialog = StatusDialog.newInstance(
            R.drawable.ic_loading,
            resources.getString(R.string.dialog_loading_activity),
        )
        statusDialog.show(childFragmentManager, "LoadingStatusDialog")
    }

    private fun showFailed(){
        statusDialog = StatusDialog.newInstance(
            R.drawable.ic_error,
            resources.getString(R.string.dialog_fail_activity),
        )
        statusDialog.show(childFragmentManager, "SuccessStatusDialog")

        Handler(Looper.getMainLooper()).postDelayed({
            statusDialog.dismiss()
        }, 1000L)
    }
}