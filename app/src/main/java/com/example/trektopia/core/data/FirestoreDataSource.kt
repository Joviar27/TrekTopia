package com.example.trektopia.core.data

import android.net.Uri
import android.util.Log
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.Activity
import com.example.trektopia.core.model.DailyStreak
import com.example.trektopia.core.model.Progress
import com.example.trektopia.core.model.Relation
import com.example.trektopia.core.model.Task
import com.example.trektopia.core.model.operation.UpdateProgress
import com.example.trektopia.core.model.enum.TaskType
import com.example.trektopia.core.model.User
import com.example.trektopia.core.model.operation.DataToUpdate
import com.example.trektopia.core.model.operation.StreakHistory
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
import java.util.Calendar
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
        trySend(ResultState.Loading)
        USERS_REF.document(userId)
            .update("pictureUri",newUri)
            .addOnSuccessListener {
                trySend(ResultState.Success(Unit))
            }
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
                ?: throw NullPointerException("resetStreak : User is null")
            val weeklyHistory = user.dailyStreak.history

            val currentTimestamp = Timestamp.now()

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = currentTimestamp.seconds * 1000
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val previousDayTimestamp = Timestamp(calendar.time)

            weeklyHistory?.let {
                val updatedWeekly = it.toMutableList()
                updatedWeekly.add(StreakHistory(previousDayTimestamp, false))
                transaction.update(userRef, "dailyStreak.history", updatedWeekly)
            } ?: run {
                val newWeekly = mutableListOf(StreakHistory(previousDayTimestamp, false))
                transaction.update(userRef, "dailyStreak.history", newWeekly)
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

    fun resetDailyMission(userId: String): Flow<ResultState<Unit>> = flow{
        emit(ResultState.Loading)
        try {
            val relationRefs = getMissionRelationRef(userId)
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

    private suspend fun getMissionRelationRef(userId: String): List<DocumentReference> {
        return MISSION_RELATIONS_REF.whereEqualTo("userRef", userId).get().await()
            .documents.map { it.reference }
    }

    private fun resetMission(
        transaction: Transaction,
        relationRef: DocumentReference
    ){
        transaction.update(relationRef,"progress", Progress())
        transaction.update(relationRef,"activeDate", Timestamp.now())
    }

    fun isLatestActiveOnDay(
        userId: String,
        checkCurrentDay: Boolean
    ): Flow<ResultState<Boolean>> = callbackFlow {
        trySend(ResultState.Loading)
        USERS_REF.document(userId).get()
            .addOnSuccessListener { snapshot ->
                val latestActive = snapshot.getTimestamp("dailyStreak.latestActive")

                if (latestActive != null) {
                    val result = if (checkCurrentDay) {
                        DateHelper.isTimestampInCurrentDay(latestActive)
                    } else {
                        DateHelper.isTimeStampPreviousDay(latestActive)
                    }
                    trySend(ResultState.Success(result))
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
        val listener = USERS_REF.orderBy("point", Query.Direction.DESCENDING).limit(30)
            .addSnapshotListener{ snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreDataSource", "getLeaderboard : $error")
                    trySend(ResultState.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                try{
                    val groupedByPoint = snapshot?.toObjects<User>()
                        ?.groupBy { it.point }
                        ?:throw IllegalArgumentException("Users are null")

                    var rank = 1
                    val usersList = mutableListOf<Pair<Int,User>>()
                    groupedByPoint.values.forEach { group ->
                        group.forEach { user ->
                            usersList.add(rank to user)
                        }
                        rank += group.size
                    }
                    trySend(ResultState.Success(usersList))
                }catch (e: Exception){
                    trySend(ResultState.Error(e.message.toString()))
                    Log.e("FirestoreDataSource", "getLeaderboard: $e")
                }
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
            .orderBy("timeStamp", Query.Direction.DESCENDING)
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
        transaction.update(relationRef, "progress.enabled", false)
    }

    fun addActivityAndUpdateProgress(
        activity: Activity,
        userId: String
    ): Flow<ResultState<Unit>> = flow {
        emit(ResultState.Loading)
        try {
            val taskRelationRefs = getUpdateProgress(activity, userId)
            val dataToUpdate  = mutableListOf<DataToUpdate>()

            db.runTransaction { transaction ->
                taskRelationRefs.forEach { updateProgress ->
                    updateProgress.taskAndRelationRef.forEach { (taskRef, relationRef) ->
                        val (requirement, progress) = retrieveTaskData(transaction, taskRef, relationRef)
                        dataToUpdate.add(
                            DataToUpdate(
                                relationRef = relationRef,
                                addedProgress = updateProgress.addedProgress,
                                progress = progress,
                                requirement = requirement
                            )
                        )
                    }
                }

                val (userRef, user) = retrieveUser(transaction, userId)
                updateStreak(transaction, activity, userRef, user)

                dataToUpdate.forEach{dataToUpdate ->
                    updateTaskProgress(
                        transaction = transaction,
                        relationRef = dataToUpdate.relationRef,
                        requirement = dataToUpdate.requirement,
                        progress = dataToUpdate.progress,
                        addedProgress = dataToUpdate.addedProgress
                    )
                }

                insertActivity(transaction, activity, userId)
            }.await()

            emit(ResultState.Success(Unit))
        } catch (e: Exception) {
            emit(ResultState.Error(e.toString()))
            Log.e("FirestoreDataSource", e.toString())
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

    private fun retrieveTaskData(
        transaction: Transaction,
        taskRef: DocumentReference,
        relationRef: DocumentReference
    ): Pair<Double, Progress> {
        val taskSnapshot = transaction.get(taskRef)
        val relationSnapshot = transaction.get(relationRef)

        if (!taskSnapshot.exists() || !relationSnapshot.exists()) {
            throw FirebaseFirestoreException("retrieveTaskData: One or more documents do not exist",
                FirebaseFirestoreException.Code.NOT_FOUND)
        }

        val requirement = taskSnapshot.getDouble("requirement")
            ?: throw NullPointerException("retrieveTaskData: Requirement is null")
        val progress = relationSnapshot.getField<Progress>("progress")
            ?: throw NullPointerException("retrieveTaskData: Progress is null")

        return requirement to progress
    }

    private fun updateTaskProgress(
        transaction: Transaction,
        relationRef: DocumentReference,
        requirement: Double,
        progress: Progress,
        addedProgress: Double
    ) {
        if (!progress.enabled) {
            val current = progress.current + addedProgress
            val oldPercentage = progress.percentage
            val newPercentage = if (current / requirement < 1.0) current / requirement else 1.0

            if (oldPercentage < 1.0) {
                val newProgress = Progress(
                    current = if (newPercentage == 1.0) requirement else current,
                    percentage = newPercentage,
                    enabled = newPercentage == 1.0
                )
                transaction.update(relationRef, "progress", newProgress)
            }
        }
    }

    private fun retrieveUser(
        transaction: Transaction,
        userId: String
    ):Pair<DocumentReference,User>{
        val userRef = USERS_REF.document(userId)
        val userSnapshot = transaction.get(userRef)

        if (!userSnapshot.exists()) {
            throw FirebaseFirestoreException("updateStreak: documents do not exist",
                FirebaseFirestoreException.Code.NOT_FOUND)
        }

        val user = userSnapshot.toObject<User>()
            ?: throw NullPointerException("updateStreak : User is null")

        return userRef to user
    }

    private fun updateStreak(
        transaction: Transaction,
        activity: Activity,
        userRef: DocumentReference,
        user: User
    ) {
        val count = user.dailyStreak.count
        val longest = user.dailyStreak.longest
        val weeklyHistory = user.dailyStreak.history
        val latestActive = user.dailyStreak.latestActive

        if (latestActive == null || !DateHelper.isTimestampInCurrentDay(latestActive)) {
            transaction.update(userRef, "dailyStreak.count", count + 1)
            if (count+1 > longest) {
                transaction.update(userRef, "dailyStreak.longest", count+1)
            }
        }

        transaction.update(userRef, "dailyStreak.latestActive", activity.timeStamp)

        val updatedWeekly = (weeklyHistory?.toMutableList() ?: mutableListOf())
            .apply { add(StreakHistory(activity.timeStamp, true)) }

        if (latestActive != null && !DateHelper.isTimestampInCurrentDay(latestActive)) {
            transaction.update(userRef, "dailyStreak.history", updatedWeekly)
        } else if (weeklyHistory == null) {
            transaction.update(userRef, "dailyStreak.history", updatedWeekly)
        }
    }

    private suspend fun getUpdateProgress(
        activity: Activity,
        userId: String
    ): List<UpdateProgress>{
        val updateProgresses = listOf(
            "distance" to activity.distance,
            "duration" to DateHelper.millisToMinutes(activity.duration),
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
    ): Flow<ResultState<List<Pair<String, Relation>>>> = callbackFlow {
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
        val relation = Relation(
            userRef = userId,
            taskRef = taskId,
            progress = Progress(),
            activeDate = null,
        )
        val randomRef = ACHIEVEMENT_RELATIONS_REF.document().id
        batch.set(ACHIEVEMENT_RELATIONS_REF.document(randomRef), relation)
    }

    private fun addUserMissionRelation(
        batch: WriteBatch,
        userId: String,
        taskId: String,
    ) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        val endOfDayTimestamp = Timestamp(calendar.time)

        val relation = Relation(
            userRef = userId,
            taskRef = taskId,
            progress = Progress(),
            activeDate = endOfDayTimestamp,
        )

        val randomRef = MISSION_RELATIONS_REF.document().id
        batch.set(MISSION_RELATIONS_REF.document(randomRef), relation)
    }
}
