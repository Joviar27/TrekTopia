package com.example.trektopia.ui.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trektopia.core.ResultState
import com.example.trektopia.databinding.FragmentHistoryBinding
import com.example.trektopia.ui.adapter.ActivityAdapter
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.safeNavigate

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: HistoryViewModel
    private lateinit var activityAdapter: ActivityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActivityRV()
        observeData()
    }

    private fun setupActivityRV(){
        activityAdapter = ActivityAdapter{activity ->
            val toDetail = HistoryFragmentDirections.actionHistoryFragmentToHistoryDetailFragment(activity)
            toDetail.activity = activity
            view?.findNavController()?.safeNavigate(toDetail)
        }
        val layoutManager = LinearLayoutManager(requireContext())
        binding?.rvActivity?.apply {
            adapter = activityAdapter
            setLayoutManager(layoutManager)
        }
    }

    private fun observeData(){
        viewModel.activities.observe(requireActivity()){result ->
            when(result){
                is ResultState.Loading -> loading(true)
                is ResultState.Success ->{
                    loading(false)
                    activityAdapter.submitList(result.data)
                }
                is ResultState.Error ->{
                    loading(false)
                    TODO("Handle error")
                }
            }
        }
    }

    private fun loading(isLoading: Boolean){
        TODO("Manage loading")
    }
}