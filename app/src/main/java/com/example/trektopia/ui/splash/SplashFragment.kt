package com.example.trektopia.ui.splash

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.trektopia.R
import com.example.trektopia.core.AuthState
import com.example.trektopia.databinding.FragmentSplashBinding
import com.example.trektopia.ui.main.AuthViewModel
import com.example.trektopia.utils.obtainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = this.obtainViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAuthState()
    }

    private fun observeAuthState(){
        viewModel.authState.observe(requireActivity()){ authState ->
            val destination = when(authState){
                is AuthState.Authenticated -> {
                    R.id.homeFragment
                }
                is AuthState.UnAuthenticated ->{
                    R.id.loginFragment
                }
            }
            lifecycleScope.launch {
                delay(1500)
                val navController = view?.findNavController()
                navController?.popBackStack()
                navController?.navigate(destination)
            }
        }
    }
}