package com.example.trektopia.utils

fun String.isValidEmail() =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()