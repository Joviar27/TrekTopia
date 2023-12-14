package com.example.trektopia.core.data

import android.net.Uri
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class StorageDataSource(
    private val storage: StorageReference,
) {

    suspend fun getDownloadUrl(
        userId: String,
        imageUri: Uri
    ):Uri {
        val newUri = uploadProfile(userId, imageUri).continueWithTask{ task ->
            if(!task.isSuccessful){
                task.exception?.let { throw it }
            }
            storage.child(userId).downloadUrl
        }.await()
        return newUri
    }

    suspend fun getProfile(userId: String): Uri? {
        return try {
            storage.child(userId).downloadUrl.await()
        } catch (e: Exception) {
            null
        }
    }

    fun uploadProfile(userId: String, uri: Uri) =
        storage.child(userId).putFile(uri)

}