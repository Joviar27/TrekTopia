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
import com.example.trektopia.databinding.FragmentHistoryBinding
import com.example.trektopia.databinding.FragmentHistoryDetailBinding
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.getStaticMapUri

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
            //TODO: Create task map route placeholder
            Glide.with(requireActivity())
                .load(activity.route.getStaticMapUri())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivActivityRoute)

            tvActivityDate.text = DateHelper.formatDateMonthYear(
                DateHelper.timeStampToLocalDate(activity.timeStamp)
            )

            //TODO: Create placeholder string format
            tvActivityDistance.text = activity.distance.toString()
            tvActivityDuration.text = activity.duration.toString()
            tvActivitySpeed.text = activity.speed.toString()
            tvActivityStep.text = activity.stepCount.toString()

        }
    }
}