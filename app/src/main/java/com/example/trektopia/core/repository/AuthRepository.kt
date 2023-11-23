package com.example.trektopia.core.repository

import com.example.trektopia.core.AuthState
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.data.AuthDataSource
import com.example.trektopia.core.data.FirestoreDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val auth: AuthDataSource,
    private val firestore: FirestoreDataSource
) {
    fun signIn(email: String, password: String) =
        auth.signIn(email, password)

    fun getAuthState() = auth.getAuthState()

    fun getUid(): Flow<String> = auth.getUid()

    fun logout() = auth.logout()

    fun signUp(
        username: String,
        email: String,
        password: String
    ): Flow<ResultState<String>> = flow{
        auth.signUp(email,password).map { result ->
            when(result) {
                is ResultState.Success -> {
                    val setupResult = firestore.setupUser(result.data, username, email)
                    emitAll(setupResult)
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