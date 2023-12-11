package com.example.trektopia.core.data

import android.util.Log
import com.example.trektopia.core.AuthState
import com.example.trektopia.core.ResultState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

class AuthDataSource(
    private val auth: FirebaseAuth
){
    fun signUp(
        email: String,
        password: String
    ): Flow<ResultState<String>> = callbackFlow {
        trySend(ResultState.Loading)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                val user = task.result.user
                if(user != null && task.isSuccessful){
                    Log.d("AuthDataSource", "Sign Up Success")
                    trySend(ResultState.Success(user.uid))
                }
                else{
                    val exception = task.exception?.message ?: "User is Null"
                    Log.e("AuthDataSource", "SignUp: $exception")
                    trySend(ResultState.Error(exception))
                }
                close()
            }
        awaitClose()
    }

    fun signIn(
        email: String,
        password: String
    ): Flow<ResultState<String>> = callbackFlow {
        trySend(ResultState.Loading)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    Log.d("AuthDataSource", "Sign In Success")
                    trySend(ResultState.Success(user.uid))
                } else {
                    val exception = "User is Null"
                    Log.e("AuthDataSource", "SignIn: $exception")
                    trySend(ResultState.Error(exception))
                }
                close()
            }
            .addOnFailureListener { exception ->
                Log.e("AuthDataSource", "SignIn: ${exception.message}")
                trySend(ResultState.Error(exception.message ?: "Unknown error"))
                close()
            }
        awaitClose()
    }

    fun logout() = auth.signOut()

    fun getAuthState(): Flow<AuthState> = callbackFlow {
        val authStateListener = AuthStateListener { auth ->
            if(auth.currentUser != null){
                trySend(AuthState.Authenticated)
            }
            else{
                trySend(AuthState.UnAuthenticated)
            }
            close()
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    fun getUid(): Flow<String> = flow{
        try{
            val uid = auth.uid
                ?: throw FirebaseFirestoreException("UID is null",
                    FirebaseFirestoreException.Code.NOT_FOUND)
            emit(uid)
        } catch (e:Exception){
            Log.e("AuthDataSource", "getUID: $e")
        }
    }
}