package com.example.trektopia.ui.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.R
import com.example.trektopia.core.model.Activity
import com.example.trektopia.databinding.FragmentHistoryDetailBinding
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.completeStaticMapUri

class HistoryDetailFragment : Fragment() {
    private var _binding: FragmentHistoryDetailBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryDetailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activityDetail = HistoryDetailFragmentArgs.fromBundle(arguments as Bundle).activity
        setupView(activityDetail)
    }

    private fun setupView(activity: Activity){
        binding?.apply {
            Glide.with(requireActivity())
                .load(activity.route.completeStaticMapUri(requireContext()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivDetailRoute)

            tvDetailDate.text = DateHelper.formatDateMonthYear(
                DateHelper.timeStampToLocalDate(activity.timeStamp)
            )

            tvDetailDay.text = DateHelper.formatDayOfWeek(
                DateHelper.timeStampToLocalDate(activity.timeStamp)
            )

            tvDetailTime.text  = resources.getString(
                R.string.start_stop_format,
                DateHelper.formatTime(
                    DateHelper.timeStampToLocalDateTime(activity.startTime)
                ),
                DateHelper.formatTime(
                    DateHelper.timeStampToLocalDateTime(activity.timeStamp)
                )
            )

            detailDistance.tvLiveInfo.text = String.format("%.1f", activity.distance)
            detailDistance.tvLiveType.text = resources.getString(R.string.km)

            tvDetailDuration.text = DateHelper.formatElapsedTime(activity.duration)

            detailSpeed.tvLiveInfo.text = String.format("%.1f", activity.speed)
            detailSpeed.tvLiveType.text = resources.getString(R.string.km_h)

            detailSteps.tvLiveInfo.text = String.format("%.1f", activity.stepCount.div(100.0))
            detailSteps.tvLiveType.text = resources.getString(R.string.live_steps)

        }
    }
}