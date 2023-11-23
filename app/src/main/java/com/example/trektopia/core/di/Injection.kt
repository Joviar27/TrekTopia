package com.example.trektopia.core.di

import com.example.trektopia.core.data.AuthDataSource
import com.example.trektopia.core.data.FirestoreDataSource
import com.example.trektopia.core.data.StorageDataSource
import com.example.trektopia.core.repository.AuthRepository
import com.example.trektopia.core.repository.GameRepository
import com.example.trektopia.core.repository.Repository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

object Injection {

    private fun provideAuthDataSource() = AuthDataSource(Firebase.auth)
    private fun provideFirestoreDataSource() = FirestoreDataSource(Firebase.firestore)
    private fun provideStorageDataSource() = StorageDataSource(Firebase.storage.reference)

    fun provideAuthRepository() =
        AuthRepository(
            provideAuthDataSource(),
            provideFirestoreDataSource()
        )

    fun provideGameRepository() =
        GameRepository(
            provideFirestoreDataSource()
        )

    fun provideRepository() =
        Repository(
            provideFirestoreDataSource(),
            provideStorageDataSource()
        )

}