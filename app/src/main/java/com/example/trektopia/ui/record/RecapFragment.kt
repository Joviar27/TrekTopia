package com.example.trektopia.ui.record

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.Activity
import com.example.trektopia.databinding.FragmentRecapBinding
import com.example.trektopia.utils.DateHelper
import com.example.trektopia.utils.getStaticMapUri
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.safeNavigate

class RecapFragment : Fragment() {
    private var _binding: FragmentRecapBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: RecordViewModel

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

            btnSaveRecord.setOnClickListener {
                viewModel.saveRecord(activity).observe(requireActivity()){result ->
                    when(result){
                        is ResultState.Loading -> loading(true)
                        is ResultState.Success ->{
                            loading(false)
                            val toHistory = RecapFragmentDirections.actionRecapFragmentToHistoryFragment()
                            view?.findNavController()?.safeNavigate(toHistory)
                        }
                        is ResultState.Error -> TODO("Handle error")
                    }
                }
            }

            btnDeleteRecord.setOnClickListener {
                val toHome = RecapFragmentDirections.actionRecapFragmentToHomeFragment()
                view?.findNavController()?.safeNavigate(toHome)
            }
        }
    }

    fun loading(isLoading: Boolean){
        TODO("Manage loading")
    }
}