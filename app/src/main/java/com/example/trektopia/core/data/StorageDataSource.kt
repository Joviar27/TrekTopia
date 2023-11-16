package com.example.trektopia.core.data

import android.net.Uri
import android.util.Log
import com.example.trektopia.core.ResultState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class StorageDataSource(
    private val storage: StorageReference,
    private val db: FirebaseFirestore
) {
    fun updateProfile(
        imageUri : Uri,
        userId : String
    ): Flow<ResultState<Unit>> =  flow{
        emit(ResultState.Loading)
        try {
            val profileRef = storage.child(userId)
            val oldUri = profileRef.downloadUrl.await()

            val uploadTask = profileRef.putFile(imageUri)
            val newUri = uploadTask.continueWithTask{ task ->
                if(!task.isSuccessful){
                    task.exception?.let { throw it }
                }
                profileRef.downloadUrl
            }.await()

            updatePictureUri(userId, profileRef, newUri, oldUri)
            emit(ResultState.Success(Unit))
        } catch (e: Exception){
            emit(ResultState.Error(e.message.toString()))
            Log.e("StorageDataSource", "updateProfile: $e")
        }
    }

    private fun updatePictureUri(
        userId: String,
        profileRef: StorageReference,
        newUri: Uri,
        oldUri: Uri
    ){
        db.collection("users").document(userId).update("pictureUri",newUri)
            .addOnFailureListener{e ->
                profileRef.putFile(oldUri)
                Log.e("StorageDataSource", "updatePictureUri: $e")
            }
    }

}