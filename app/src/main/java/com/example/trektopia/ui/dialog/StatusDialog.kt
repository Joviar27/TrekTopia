package com.example.trektopia.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.trektopia.databinding.DialogStatusBinding

class StatusDialog : DialogFragment() {

    private lateinit var binding: DialogStatusBinding

    companion object {
        private const val ARG_ICON = "icon"
        private const val ARG_MESSAGE = "message"

        fun newInstance(icon: Int, message: String): StatusDialog {
            val fragment = StatusDialog()
            val args = Bundle().apply {
                putInt(ARG_ICON, icon)
                putString(ARG_MESSAGE, message)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var icon = 0
        var message =""
        if(arguments!=null){
            icon = arguments?.getInt(ARG_ICON) ?: 0
            message = arguments?.getString(ARG_MESSAGE) ?: "Dialog Error"
        }

        binding = DialogStatusBinding.inflate(layoutInflater)
        val view = binding.root

        binding.apply {
            ivIcon.setBackgroundResource(icon)
            tvMessage.text = message
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

    }
}