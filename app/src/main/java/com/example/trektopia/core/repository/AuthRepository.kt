package com.example.trektopia.core.repository

import android.util.Log
import com.example.trektopia.core.AuthState
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.data.AuthDataSource
import com.example.trektopia.core.data.FirestoreDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.Exception

class AuthRepository(
    private val authDataSource: AuthDataSource,
    private val firestoreDataSource: FirestoreDataSource
) {

    fun signIn(email: String, password: String): Flow<ResultState<String>> = flow{
        emit(ResultState.Loading)
        authDataSource.signIn(email, password)
    }

    suspend fun getAuthState(): Flow<AuthState> = authDataSource.getAuthState()

    suspend fun getUid(): Flow<String?> = authDataSource.getUid()

    fun logout() = authDataSource.logout()


    suspend fun signUp(username: String, email: String, password: String): Flow<ResultState<String>> = flow{
        emit(ResultState.Loading)
        authDataSource.signUp(email,password).map { result ->
            when(result) {
                is ResultState.Success -> {
                    try {
                        firestoreDataSource.insertNewUser(uid = result.data, username, email)

                        firestoreDataSource.assignAllMissions(userId = result.data)

                        firestoreDataSource.assignAllAchievements(userId = result.data)

                        emit(result)
                    } catch (e: Exception) {
                        Log.d("AuthRepository", "signUp: ${e.message.toString()}")
                        emit(ResultState.Error(e.message.toString()))
                    }
                }
                else -> emit(result)
            }
        }
    }

    /* Backup if needed
    suspend fun signIn2(email:String, password: String): Flow<ResultState<String>> = flow{
        emit(ResultState.Loading)
        authDataSource.signIn(email, password).map {result ->
            when(result) {
                is ResultState.Success -> {
                    try{
                        firestoreDataSource.assignNewMission(userId = result.data)
                        firestoreDataSource.assignNewAchievements(userId = result.data)
                        emit(result)
                    }
                    catch (e: Exception){
                        Log.d("AuthRepository", "createUserInfo: ${e.message.toString()}")
                        emit(ResultState.Error(e.message.toString()))
                    }
                }
                else -> emit(result)
            }
        }
    }
     */

}