package com.example.trektopia.core.data

import android.util.Log
import com.example.trektopia.core.AuthState
import com.example.trektopia.core.ResultState
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.tasks.await

class AuthDataSource(
    private val auth: FirebaseAuth
){

    suspend fun signUp(email: String, password: String): Flow<ResultState<String>> = callbackFlow {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthRepository", "Sign Up Success")
                    task.result.user.let { user ->
                        if(user!=null) trySend(ResultState.Success(user.uid))
                    }
                } else {
                    Log.d("AuthRepository", "SignUp: ${task.exception?.message.toString()}")
                    trySend(ResultState.Error(task.exception?.message.toString()))
                }
                close()
            }
        awaitClose()
    }

    suspend fun signIn(email: String, password: String): Flow<ResultState<String>> = callbackFlow {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    Log.d("AuthRepository", "Sign In Success")
                    task.result.user.let { user ->
                        if(user!=null) trySend(ResultState.Success(user.uid))
                    }
                } else {
                    Log.d("AuthRepository", "SignIn: ${task.exception?.message.toString()}")
                    trySend(ResultState.Error(task.exception?.message.toString()))
                }
                close()
            }
        awaitClose()
    }

    fun logout() = auth.signOut()

    suspend fun getAuthState(): Flow<AuthState> = callbackFlow {
        val authStateListener = AuthStateListener { auth ->
            if(auth.currentUser != null){
                trySend(AuthState.Authenticated)
            }
            else{
                trySend(AuthState.UnAuthenticated)
            }
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    suspend fun getUid(): Flow<String?> = flow{
        emit(auth.currentUser?.uid)
    }

}