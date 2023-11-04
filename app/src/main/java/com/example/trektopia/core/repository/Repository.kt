package com.example.trektopia.core.repository

import android.util.Log
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class Repository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

}