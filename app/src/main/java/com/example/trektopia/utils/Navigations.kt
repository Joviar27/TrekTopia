package com.example.trektopia.utils

import androidx.navigation.NavController
import androidx.navigation.NavDirections

fun NavController.safeNavigate(direction: NavDirections) {
    currentDestination?.getAction(direction.actionId)?.run { navigate(direction) }
}

fun NavController.safeNavigate(destination: Int) {
    currentDestination?.getAction(destination)?.run { navigate(destination) }
}