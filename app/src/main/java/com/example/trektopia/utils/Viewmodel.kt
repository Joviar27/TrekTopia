package com.example.trektopia.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

inline fun <reified T : ViewModel> ViewModelStoreOwner.obtainViewModel(): T {
    val factory: ViewModelFactory = ViewModelFactory.getInstance()
    return ViewModelProvider(this, factory)[T::class.java]
}