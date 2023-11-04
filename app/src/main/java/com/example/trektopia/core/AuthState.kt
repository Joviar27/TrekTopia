package com.example.trektopia.core

sealed class AuthState private constructor(){
    //data class Authenticated<out T>(val data : T) : AuthState<T>()
    object Authenticated : AuthState()
    object UnAuthenticated : AuthState()
}
