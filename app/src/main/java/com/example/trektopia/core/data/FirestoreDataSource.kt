package com.example.trektopia.core.data

import android.net.Uri
import android.util.Log
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.Activity
import com.example.trektopia.core.model.DailyStreak
import com.example.trektopia.core.model.Progress
import com.example.trektopia.core.model.abstraction.Relation
import com.example.trektopia.core.model.abstraction.Task
import com.example.trektopia.core.model.operation.UpdateProgress
import com.example.trektopia.core.model.enum.TaskType
import com.example.trektopia.core.model.User
import com.example.trektopia.core.model.UserAchievementRelation
import com.example.trektopia.core.model.UserMissionRelation
import com.example.trektopia.utils.DateHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.ktx.getField
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlin.Exception
import kotlin.NullPointerException

class FirestoreDataSource(
    private val db: FirebaseFirestore
){
    private val USERS_REF = db.collection("users")
    private val MISSIONS_REF = db.collection("missions")
    private val ACHIEVEMENTS_REF = db.collection("achievements")
    private val MISSION_RELATIONS_REF = db.collection("user_mission_relations")
    private val ACHIEVEMENT_RELATIONS_REF = db.collection("user_achievement_relations")

    fun getRelationCollectionRef(taskType: TaskType) : CollectionReference{
        return when(taskType){
            TaskType.MISSION -> MISSION_RELATIONS_REF
            TaskType.ACHIEVEMENT -> ACHIEVEMENT_RELATIONS_REF
        }
    }

    fun getTaskCollectionRef(taskType: TaskType) : CollectionReference{
        return when(taskType){
            TaskType.MISSION -> MISSIONS_REF
            TaskType.ACHIEVEMENT -> ACHIEVEMENTS_REF
        }
    }

    fun updatePictureUri(
        userId: String,
        newUri: Uri,
    ): Flow<ResultState<Unit>> = callbackFlow{
        USERS_REF.document(userId)
            .update("pictureUri",newUri)
            .addOnFailureListener{e ->
                trySend(ResultState.Error(e.message.toString()))
                Log.e("FirestoreDataSource", "updatePictureUri: $e")
                close()
            }
        awaitClose()
    }

    fun resetStreak(
        userId: String
    ):Flow<ResultState<Unit>> = callbackFlow {
        trySend(ResultState.Loading)
        db.runTransaction {transaction ->
            val userRef = USERS_REF.document(userId)
            val userSnapshot = transaction.get(userRef)

            if (!userSnapshot.exists()) {
                throw FirebaseFirestoreException("resetStreak: documents do not exist",
                    FirebaseFirestoreException.Code.NOT_FOUND)
            }

            val user = userSnapshot.toObject<User>()
                ?: throw NullPointerException("updateStreak : User is null")
            val history = user.dailyStreak.history

            if(history!=null){
                val updatedWeekly = history.toMutableList().add(Pair(false, Timestamp.now()))
                transaction.update(userRef,"dailyStreak.weeklyHistory", updatedWeekly)
            } else {
                val newWeekly = mutableListOf<Pair<Boolean,Timestamp>>().add(Pair(false, Timestamp.now()))
                transaction.update(userRef,"dailyStreak.weeklyHistory", newWeekly)
            }

            transaction.update(userRef, "dailyStreak.count", 0)
        }.addOnSuccessListener {
            trySend(ResultState.Success(Unit))
        }.addOnFailureListener{e ->
            trySend(ResultState.Error(e.message.toString()))
            Log.e("FirestoreDataSource", "resetStreak: $e")
        }.addOnCompleteListener{
            close()
        }
        awaitClose()
    }

    fun resetDailyMission(): Flow<ResultState<Unit>> = flow{
        emit(ResultState.Loading)
        try {
            val relationRefs = getMissionRelationRef()
            db.runTransaction { transaction ->
                relationRefs.forEach{ relationRef ->
                    resetMission(transaction, relationRef)
                }
            }.await()
            emit(ResultState.Success(Unit))
        }catch (e: Exception){
            emit(ResultState.Error(e.message.toString()))
            Log.e("FirestoreDataSource", "resetDailyMission: $e")
        }
    }

    private suspend fun getMissionRelationRef(): List<DocumentReference> {
        return MISSION_RELATIONS_REF.get().await()
            .documents.map { it.reference }
    }

    private fun resetMission(
        transaction: Transaction,
        relationRef: DocumentReference
    ){
        transaction.update(relationRef,"progress", Progress())
        transaction.update(relationRef,"activeDate", Timestamp.now())
    }

    fun checkLatestActiveDate(
        userId: String
    ): Flow<ResultState<LocalDate>> = callbackFlow {
        trySend(ResultState.Loading)
        USERS_REF.document(userId).get()
            .addOnSuccessListener { snapshot ->
                val dailyStreak = snapshot.getField<DailyStreak>("dailyStreak")
                val latestDate = dailyStreak?.latestActive?.let {
                    DateHelper.timeStampToLocalDate(it)
                }

                if(latestDate!=null){
                    trySend(ResultState.Success(latestDate))
                } else {
                    trySend(ResultState.Error("latest date is null"))
                    Log.e("FirestoreDataSource", "getLatestActive: latest date is null")
                }
            }.addOnFailureListener { e ->
                trySend(ResultState.Error(e.message.toString()))
                Log.e("FirestoreDataSource", "getLatestActive: $e")
            }.addOnCompleteListener {
                close()
            }
        awaitClose()
    }

    fun getLeaderboard(): Flow<ResultState<List<Pair<Int,User>>>> = callbackFlow {
        val listener = USERS_REF.orderBy("point", Query.Direction.DESCENDING).limit(20)
            .addSnapshotListener{ snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreDataSource", "getLeaderboard : $error")
                    trySend(ResultState.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                try{
                    val usersList = snapshot?.toObjects<User>()
                        ?.groupBy { it.point }
                        ?.flatMap { (_, usersWithSamePoint) ->
                            usersWithSamePoint.mapIndexed { index, user ->
                                index + 1 to user
                            }
                        } ?: throw IllegalArgumentException("Users are null")

                    trySend(ResultState.Success(usersList))
                }catch (e: Exception){
                    trySend(ResultState.Error(e.message.toString()))
                    Log.e("FirestoreDataSource", "getLeaderboard: $e")
                }
                close()
            }
        awaitClose{ listener.remove() }
    }

    fun getUserRank(userPoint: Int): Flow<ResultState<Int>> = callbackFlow{
        val listener = USERS_REF
            .whereGreaterThan("point", userPoint)
            .addSnapshotListener{snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreDataSource", "getUserRank : $error")
                    trySend(ResultState.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                try {
                    val rank = snapshot?.toObjects<User>()
                        ?.groupBy { it.point }?.size?.plus(1)
                        ?: throw IllegalArgumentException("Rank are null")

                    trySend(ResultState.Success(rank))
                }catch (e: Exception){
                    trySend(ResultState.Error(e.message.toString()))
                    Log.e("FirestoreDataSource", "getUserRank: $e")
                }
                close()
            }
        awaitClose{ listener.remove() }
    }

    fun getUserActivities(
        userId: String
    ): Flow<ResultState<List<Activity>>> = callbackFlow{
        trySend(ResultState.Loading)
        val listener = USERS_REF.document(userId).collection("activities")
            .addSnapshotListener{snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreDataSource", "getUserActivities : $error")
                    trySend(ResultState.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                try{
                    val activities = snapshot?.toObjects<Activity>()
                        ?: throw NullPointerException("Activities are null")
                    trySend(ResultState.Success(activities))
                }catch (e: Exception){
                    trySend(ResultState.Error(e.message.toString()))
                    Log.e("FirestoreDataSource", "getUserActivities: $e")
                }
                close()
            }
        awaitClose{ listener.remove() }
    }

    fun updateUserInfo(
        userId: String,
        newUser: User
    ): Flow<ResultState<Unit>> = callbackFlow{
        trySend(ResultState.Loading)
        USERS_REF.document(userId).set(newUser)
            .addOnSuccessListener {
                trySend(ResultState.Success(Unit))
            }.addOnFailureListener{ e->
                trySend(ResultState.Error(e.message.toString()))
                Log.e("FirestoreDataSource", "updateUser: $e")
            }.addOnCompleteListener{
                close()
            }
        awaitClose()
    }

     fun claimTaskReward(
        userId: String,
        relationId: String,
        reward: Int,
        taskType: TaskType
    ): Flow<ResultState<Unit>> = callbackFlow{
         trySend(ResultState.Loading)
         db.runTransaction{ transaction ->
            updatePoint(
                transaction = transaction,
                userId = userId,
                reward = reward
            )
            updateProgressStatus(
                transaction = transaction,
                relationId = relationId,
                taskType = taskType
            )
         }.addOnSuccessListener {
            trySend(ResultState.Success(Unit))
            Log.d("FirestoreDataSource", "claimReward: Success")
         }.addOnFailureListener{e ->
            trySend(ResultState.Error(e.message.toString()))
            Log.d("FirestoreDataSource", "claimReward: $e")
         }.addOnCompleteListener{
            close()
         }
         awaitClose()
    }

    private fun updatePoint(
        transaction: Transaction,
        userId: String,
        reward: Int
    ){
        val userRef = USERS_REF.document(userId)

        val userSnapshot = transaction.get(userRef)

        if (!userSnapshot.exists()) {
            throw FirebaseFirestoreException("One or more documents do not exist",
                FirebaseFirestoreException.Code.NOT_FOUND)
        }

        val currentPoint = userSnapshot.getDouble("point")
            ?: throw NullPointerException("Point is null")

        val newPoint = currentPoint+reward

        transaction.update(userRef, "point", newPoint)
    }

    private fun updateProgressStatus(
        transaction: Transaction,
        relationId: String,
        taskType: TaskType
    ) {
        val relationCollectionReference = when (taskType) {
            TaskType.MISSION -> MISSION_RELATIONS_REF
            TaskType.ACHIEVEMENT -> ACHIEVEMENT_RELATIONS_REF
        }

        val relationRef = relationCollectionReference.document(relationId)
        transaction.update(relationRef, "progress.enabled", true)
    }

    fun addActivityAndUpdateProgress(
        activity: Activity,
        userId: String
    ):Flow<ResultState<Unit>> = flow{
        emit(ResultState.Loading)
        try{
            val taskRelationRefs = getUpdateProgress(activity, userId)
            db.runTransaction { transaction ->
                taskRelationRefs.forEach { updateProgress ->
                    updateProgress.taskAndRelationRef.forEach { (taskRef, relationRef) ->
                        updateTaskProgress(transaction, taskRef, relationRef, updateProgress.addedProgress)
                    }
                }
                insertActivity(transaction, activity, userId)
                updateStreak(transaction, activity, userId)
            }.await()
            emit(ResultState.Success(Unit))
        }
        catch (e: Exception){
            emit(ResultState.Error(e.message.toString()))
            Log.e("FirestoreDataSource", e.message.toString())
        }
    }

    private fun insertActivity(
        transaction: Transaction,
        activity: Activity,
        userId: String
    ){
        val activityRef = USERS_REF.document(userId).collection("activities").document(activity.id)

        transaction.set(activityRef,activity)
    }

    private fun updateTaskProgress(
        transaction: Transaction,
        taskRef: DocumentReference,
        relationRef: DocumentReference,
        addedProgress: Double
    ) {
        val taskSnapshot = transaction.get(taskRef)
        val relationSnapshot = transaction.get(relationRef)

        if (!taskSnapshot.exists() || !relationSnapshot.exists()) {
            throw FirebaseFirestoreException("updateTaskProgress: One or more documents do not exist",
                FirebaseFirestoreException.Code.NOT_FOUND)
        }

        val requirement = taskSnapshot.getDouble("requirement")
            ?: throw NullPointerException("updateTaskProgress: Requirement is null")
        val progress = relationSnapshot.getField<Progress>("progress")
            ?: throw NullPointerException("updateTaskProgress: Progress is null")

        if(!progress.enabled){
            val current = progress.current + addedProgress
            val oldPercentage = progress.percentage
            val newPercentage = if(current/requirement<1.0) current/requirement else 1.0

            if(oldPercentage<1.0){
                val newProgress = Progress(
                    current = if(newPercentage==1.0)requirement else current,
                    percentage = newPercentage,
                    enabled = newPercentage==1.0
                )
                transaction.update(relationRef, "progress", newProgress)
            }
        }
    }

    private fun updateStreak(
        transaction: Transaction,
        activity: Activity,
        userId: String
    ){
        val userRef = USERS_REF.document(userId)
        val userSnapshot = transaction.get(userRef)

        if (!userSnapshot.exists()) {
            throw FirebaseFirestoreException("updateStreak: documents do not exist",
                FirebaseFirestoreException.Code.NOT_FOUND)
        }

        val user = userSnapshot.toObject<User>()
            ?: throw NullPointerException("updateStreak : User is null")
        val weeklyHistory = user.dailyStreak.history

        transaction.update(userRef, "dailyStreak.count",1)
        transaction.update(userRef, "dailyStreak.latestActive", activity.timeStamp)

        if(user.dailyStreak.count > user.dailyStreak.longest){
            transaction.update(userRef, "dailyStreak.longest", user.dailyStreak.count)
        }

        if(weeklyHistory!=null){
            val updatedWeekly = weeklyHistory.toMutableList().add(Pair(true,activity.timeStamp))
            transaction.update(userRef,"dailyStreak.history", updatedWeekly)
        } else {
            val newWeekly = mutableListOf<Pair<Boolean,Timestamp>>().add(Pair(true,activity.timeStamp))
            transaction.update(userRef,"dailyStreak.history", newWeekly)
        }
    }

    private suspend fun getUpdateProgress(
        activity: Activity,
        userId: String
    ): List<UpdateProgress>{
        val updateProgresses = listOf(
            "distance" to activity.distance,
            "duration" to activity.duration,
            "stepCount" to activity.stepCount.toDouble(),
            "activityCount" to 1.0,
            "streakCount" to 1.0
        ).map { (type, addedProgress) ->
            val missionAndRelationRefs = fetchRefs(type, MISSIONS_REF, MISSION_RELATIONS_REF, userId)
            val achievementAndRelationRefs = fetchRefs(type, ACHIEVEMENTS_REF, ACHIEVEMENT_RELATIONS_REF, userId)
            UpdateProgress(missionAndRelationRefs + achievementAndRelationRefs, addedProgress)
        }
        return updateProgresses
    }

    private suspend fun fetchRefs(
        type: String,
        taskRefCollection: CollectionReference,
        relationRefCollection: CollectionReference,
        userId: String
    ): List<Pair<DocumentReference,DocumentReference>> {
        val taskAndRelationRef = mutableListOf<Pair<DocumentReference, DocumentReference>>()

        val tasks = taskRefCollection.whereEqualTo("type", type).get().await()
            ?: throw NullPointerException("fetchRefs: Tasks is null")

        for (taskDoc in tasks.documents) {
            val relations = relationRefCollection
                .whereEqualTo("taskRef", taskDoc.id)
                .whereEqualTo("userRef", userId)
                .get().await()
                ?: throw NullPointerException("Relations is null")

            val relationRef = relations.documents[0].reference
            taskAndRelationRef.add(Pair(taskDoc.reference, relationRef))
        }
        return taskAndRelationRef
    }

    fun getRelationData(
        userId: String,
        relationRefCollection: CollectionReference
    ): Flow<ResultState<List<Pair<String,Relation>>>> = callbackFlow {
        trySend(ResultState.Loading)
        val listener = relationRefCollection.whereEqualTo("userRef", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreDataSource", "getRelationData : $error")
                    trySend(ResultState.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                try{
                    val relationDoc = snapshot?.documents
                        ?: throw NullPointerException("Relation snapshot is null")

                    val result = relationDoc.map{
                       val relation = it.toObject<Relation>()
                           ?: throw NullPointerException("One of relation is null")
                        Pair(it.id, relation)
                    }
                    trySend(ResultState.Success(result))
                }catch (e: Exception){
                    trySend(ResultState.Error(e.message.toString()))
                    Log.e("FirestoreDataSource", "getRelationData: $e")
                }
                close()
            }
        awaitClose { listener.remove() }
    }

    fun getTaskData(
        taskRefCollection: CollectionReference
    ): Flow<ResultState<List<Task>>> = callbackFlow {
        trySend(ResultState.Loading)
        val listener = taskRefCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreDataSource", "getTaskData : $error")
                    trySend(ResultState.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                try{
                    val task = snapshot?.toObjects<Task>()
                        ?: throw NullPointerException("Task are null")
                    trySend(ResultState.Success(task))
                }catch (e: Exception){
                    trySend(ResultState.Error(e.message.toString()))
                    Log.e("FirestoreDataSource", "getTaskData: $e")
                }
                close()
            }
        awaitClose { listener.remove() }
    }

    fun getUserData(
        userId: String
    ): Flow<ResultState<User>> = callbackFlow {
        trySend(ResultState.Loading)
        val listener = USERS_REF.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreDataSource", "getUserData: $error")
                    trySend(ResultState.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                try{
                    val user = snapshot?.toObject<User>()
                        ?: throw NullPointerException("User is null")
                    trySend(ResultState.Success(user))
                }catch (e: Exception){
                    trySend(ResultState.Error(e.message.toString()))
                    Log.e("FirestoreDataSource", "getUserData: $e")
                }
                close()
            }
        awaitClose { listener.remove() }
    }

    fun setupUser(
        userId: String,
        username: String,
        email: String,
    ): Flow<ResultState<String>> = flow{
        try{
            val taskIds = getAllTaskID()
            db.runBatch{batch ->
                insertNewUser(batch,userId,username, email)
                taskIds.forEach{(taskId, taskType) ->
                    when(taskType){
                        TaskType.MISSION -> addUserMissionRelation(
                            batch, userId, taskId)
                        TaskType.ACHIEVEMENT -> addUserAchievementRelation(
                            batch, userId, taskId)
                    }
                }
            }.await()
            emit(ResultState.Success("Success"))
        }
        catch (e: Exception){
            emit(ResultState.Error(e.message.toString()))
            Log.e("FirestoreDataSource","setupUser: $e")
        }
    }

    private fun insertNewUser(
        batch: WriteBatch,
        uid: String,
        username: String,
        email: String
    ){
        val initialUser = User(
            uid = uid,
            username = username,
            email = email,
            point = 0,
            pictureUri = null,
            dailyStreak = DailyStreak()
        )
        batch.set(USERS_REF.document(uid), initialUser)
    }

    private suspend fun getAllTaskID(): List<Pair<String, TaskType>>{
        val missions = MISSIONS_REF.get().await()
        val achievements = ACHIEVEMENTS_REF.get().await()

        val missionId = missions.documents.map {
            Pair(it.id, TaskType.MISSION)
        }
        val achievementId = achievements.documents.map {
            Pair(it.id, TaskType.ACHIEVEMENT)
        }
        return (missionId+achievementId)
    }

    private fun addUserAchievementRelation(
        batch: WriteBatch,
        userId: String,
        taskId: String,
    ){
        val relation = UserAchievementRelation(
            userRef = userId,
            taskRef = taskId,
            progress = Progress(),
            activeDate = Timestamp.now(),
        )
        val randomRef = ACHIEVEMENT_RELATIONS_REF.document().id
        batch.set(ACHIEVEMENT_RELATIONS_REF.document(randomRef), relation)
    }

    private fun addUserMissionRelation(
        batch: WriteBatch,
        userId: String,
        taskId: String,
    ){
        val relation = UserMissionRelation(
            userRef = userId,
            taskRef = taskId,
            progress = Progress(),
            activeDate = Timestamp.now(),
        )
        val randomRef = MISSION_RELATIONS_REF.document().id
        batch.set(MISSION_RELATIONS_REF.document(randomRef), relation)
    }

}
