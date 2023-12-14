package com.example.trektopia.core.repository

import android.net.Uri
import android.util.Log
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.data.FirestoreDataSource
import com.example.trektopia.core.data.StorageDataSource
import com.example.trektopia.core.model.Activity
import com.example.trektopia.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class Repository(
    private val firestore: FirestoreDataSource,
    private val storage: StorageDataSource
) {
    fun getUserData(userId: String) = firestore.getUserData(userId)

    //To show notification if user haven't done any activity
    fun checkLatestActiveDate(userId: String) = firestore.checkLatestActiveDate(userId)

    fun getUserActivities(userId: String) = firestore.getUserActivities(userId)

    fun updateUserInfo(userId: String, newUser: User) =
        firestore.updateUserInfo(userId,newUser)

    fun addActivityAndUpdateProgress(userId: String, activity: Activity) =
        firestore.addActivityAndUpdateProgress(activity, userId)

    private suspend fun getProfile(userId: String) = storage.getProfile(userId)

    private suspend fun getDownloadUrl(userId: String, imageUri: Uri) =
        storage.getDownloadUrl(userId, imageUri)

    private fun uploadProfile(userId: String, imageUri: Uri) =
        storage.uploadProfile(userId, imageUri)

    private fun updatePictureUri(userId: String, imageUri: Uri) =
        firestore.updatePictureUri(userId, imageUri)

    fun updateProfilePicture(
        imageUri: Uri,
        userId: String
    ): Flow<ResultState<Unit>> = flow{
        emit(ResultState.Loading)
        try {
            val oldUri = getProfile(userId)
            val newUri = getDownloadUrl(userId, imageUri)

            updatePictureUri(userId, newUri).collect{ result ->
                when(result){
                    is ResultState.Error -> {
                        if(oldUri!=null) uploadProfile(userId, oldUri)
                    }
                    else -> emit(result)
                }
            }
        } catch (e: Exception){
            emit(ResultState.Error(e.message.toString()))
            Log.e("Repository", "updateProfilePicture: $e")
        }
    }
}