package com.example.trektopia.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

inline fun <reified T : ViewModel> ViewModelStoreOwner.obtainViewModel(context: Context): T {
    val factory: ViewModelFactory = ViewModelFactory.getInstance(context)
    return ViewModelProvider(this, factory)[T::class.java]
}