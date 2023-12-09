package com.example.trektopia.ui.register

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.utils.isValidEmail
import com.example.trektopia.utils.obtainViewModel
import com.example.trektopia.utils.safeNavigate
import com.example.trektopia.utils.showToast
import com.example.trektopia.databinding.FragmentRegisterBinding
import com.example.trektopia.ui.dialog.StatusDialog

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding

    private var _viewModel: RegisterViewModel? = null
    private val viewModel get() = _viewModel

    private var _statusDialog: StatusDialog? = null
    private val statusDialog get() = _statusDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _viewModel = this.obtainViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
    }

    private fun setupView() {
        binding?.apply {
            btnRegister.setOnClickListener {
                val username = edtUsername.text.toString()
                val email = edtEmail.text.toString()
                val password = edtPassword.text.toString()

                clearError()
                validateInputs(username, email, password)
            }
            btnToLogin.setOnClickListener {
                navigateToLogin()
            }
        }

        _statusDialog = StatusDialog.newInstance(
            R.drawable.ic_loading,
            resources.getString(R.string.dialog_loading_login),
        )
    }

    private fun validateInputs(username:String, email: String, password: String) {
        binding?.apply {
            when {
                username.isEmpty() -> edtUsername.error =
                    resources.getString(R.string.err_required)
                email.isEmpty() -> edtEmail.error =
                    resources.getString(R.string.err_required)
                !email.isValidEmail() -> edtEmail.error =
                    resources.getString(R.string.err_invalid_email)
                password.isEmpty() -> edtPassword.error =
                    resources.getString(R.string.err_required)
                password.length < 8 -> edtPassword.error =
                    resources.getString(R.string.err_invalid_pass_1)
                password.contains(" ") -> edtPassword.error =
                    resources.getString(R.string.err_invalid_pass_2)

                else -> signUp(username, email, password)
            }
        }
    }

    private fun clearError(){
        binding?.apply {
            edtEmail.error = null
            edtPassword.error = null
        }
    }

    private fun signUp(username: String, email: String, password: String) {
        viewModel?.register(username, email, password)?.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResultState.Loading -> showLoading(true)
                is ResultState.Error -> {
                    showLoading(false)
                    resources.getString(R.string.signup_failed)
                        .showToast(requireContext())
                }
                is ResultState.Success -> {
                    showLoading(false)
                    resources.getString(R.string.signin_succcess)
                        .showToast(requireContext())
                }
            }
        }
    }

    private fun navigateToLogin(){
        val direction =
            RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
        view?.findNavController()?.safeNavigate(direction)
    }

    private fun showLoading(isLoading : Boolean){
        if(isLoading) statusDialog?.show(childFragmentManager, "LoadingStatusDialog")
        else statusDialog?.dismiss()
    }
}