package com.example.trektopia.core.data

import android.util.Log
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.model.Activity
import com.example.trektopia.core.model.DailyStreak
import com.example.trektopia.core.model.Progress
import com.example.trektopia.core.model.Relation
import com.example.trektopia.core.model.Task
import com.example.trektopia.core.model.TaskRelationRef
import com.example.trektopia.core.model.TaskType
import com.example.trektopia.core.model.User
import com.example.trektopia.core.model.UserAchievementRelation
import com.example.trektopia.core.model.UserMissionRelation
import com.example.trektopia.core.utils.DateHelper
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
    private val usersRef = db.collection("users")
    private val missionsRef = db.collection("missions")
    private val achievementsRef = db.collection("achievements")
    private val userMissionRelationsRef = db.collection("user_mission_relations")
    private val userAchievementRelationsRef = db.collection("user_achievement_relations")

    fun getRelationCollectionRef(taskType: TaskType) : CollectionReference{
        return when(taskType){
            TaskType.MISSION -> userMissionRelationsRef
            TaskType.ACHIEVEMENT -> userAchievementRelationsRef
        }
    }

    fun getTaskCollectionRef(taskType: TaskType) : CollectionReference{
        return when(taskType){
            TaskType.MISSION -> missionsRef
            TaskType.ACHIEVEMENT -> achievementsRef
        }
    }

    fun resetStreak(
        userId: String
    ):Flow<ResultState<Unit>> = callbackFlow {
        trySend(ResultState.Loading)
        usersRef.document(userId).update("dailyStreak.count",0 )
            .addOnSuccessListener {
                trySend(ResultState.Success(Unit))
            }.addOnFailureListener{e ->
                trySend(ResultState.Error(e.message.toString()))
                Log.e("FirestoreDataSource", "resetStreak: $e")
            }
            .addOnCompleteListener{
                close()
            }
        awaitClose()
    }

    fun resetDailyMission(): Flow<ResultState<Unit>> = flow{
        emit(ResultState.Loading)
        try {
            val relationRefs = userMissionRelationsRef.get().await()
                .documents.map { it.reference }
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

    private fun resetMission(
        transaction: Transaction,
        relationRef: DocumentReference
    ){
        transaction.update(relationRef,"progress", Progress())
        transaction.update(relationRef,"activeDate", Timestamp.now())
    }

    fun checkLatestActiveDate(
        userId: String
    ): Flow<ResultState<Boolean>> = callbackFlow {
        trySend(ResultState.Loading)
        usersRef.document(userId).get()
            .addOnSuccessListener { snapshot ->
                val dailyStreak = snapshot.getField<DailyStreak>("dailyStreak")
                val latestDate = dailyStreak?.latestActive?.let {
                    DateHelper.timeStampToLocalDate(it)
                }
                if(latestDate!=null)trySend(ResultState.Success(latestDate == LocalDate.now()))
                else {
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
        val listener = usersRef.orderBy("point", Query.Direction.DESCENDING).limit(20)
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
        val listener = db.collection("users")
            .whereGreaterThan("point", userPoint.toInt())
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
        val listener = usersRef.document(userId).collection("activities")
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
        usersRef.document(userId).set(newUser)
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
        taskId: String,
        taskType: TaskType
    ): Flow<ResultState<Unit>> = callbackFlow{
         trySend(ResultState.Loading)
         db.runTransaction{ transaction ->
            updatePoint(
                transaction = transaction,
                userId = userId,
                taskId = taskId,
                taskType = taskType
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
        taskId: String,
        taskType: TaskType
    ){
        val taskCollectionReference = when(taskType){
            TaskType.MISSION -> missionsRef
            TaskType.ACHIEVEMENT -> achievementsRef
        }

        val taskRef = taskCollectionReference.document(taskId)
        val userRef = usersRef.document(userId)

        val taskSnapshot = transaction.get(taskRef)
        val userSnapshot = transaction.get(userRef)

        if (!taskSnapshot.exists() || !userSnapshot.exists()) {
            throw FirebaseFirestoreException("One or more documents do not exist",
                FirebaseFirestoreException.Code.NOT_FOUND)
        }

        val reward = taskSnapshot.getDouble("reward")
            ?: throw NullPointerException("Reward is null")

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
            TaskType.MISSION -> userMissionRelationsRef
            TaskType.ACHIEVEMENT -> userAchievementRelationsRef
        }

        val relationRef = relationCollectionReference.document(relationId)
        transaction.update(relationRef, "progress.claimed", true)
    }

    fun addActivityAndUpdateProgress(
        activity: Activity,
        userId: String
    ):Flow<ResultState<Unit>> = flow{
        emit(ResultState.Loading)
        try{
            val taskRelationRefs = getTaskRelationRefs(activity, userId)
            db.runTransaction { transaction ->
                taskRelationRefs.forEach { taskRelationRef ->
                    taskRelationRef.taskAndRelationRef.forEach { (taskRef, relationRef) ->
                        updateTaskProgress(transaction, taskRef, relationRef, taskRelationRef.addedProgress)
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
        val activityRef = usersRef.document(userId).collection("activities").document(activity.id)
        val activitySnapshot = transaction.get(activityRef)

        if (!activitySnapshot.exists()) {
            throw FirebaseFirestoreException("insertActivity: One or more documents do not exist",
                FirebaseFirestoreException.Code.NOT_FOUND)
        }

        transaction.set(activityRef,activity)
    }

    private fun updateTaskProgress(
        transaction: Transaction,
        taskRef: DocumentReference,
        relationRef: DocumentReference,
        addedProgress: Double
    ) {
        val missionSnapshot = transaction.get(taskRef)
        val relationSnapshot = transaction.get(relationRef)

        if (!missionSnapshot.exists() || !relationSnapshot.exists()) {
            throw FirebaseFirestoreException("updateTaskProgress: One or more documents do not exist",
                FirebaseFirestoreException.Code.NOT_FOUND)
        }

        val requirement = missionSnapshot.getDouble("requirement")
            ?: throw NullPointerException("updateTaskProgress: Requirement is null")
        val progress = relationSnapshot.getField<Progress>("progress")
            ?: throw NullPointerException("updateTaskProgress: Progress is null")

        if(!progress.claimed){
            val current = progress.current + addedProgress
            val oldPercentage = progress.percentage
            val newPercentage = if(current/requirement<1) current/requirement else 1.0

            if(oldPercentage<1){
                val newProgress = Progress(
                    current = current,
                    percentage = newPercentage,
                    claimed = false
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
        val userRef = usersRef.document(userId)
        val userSnapshot = transaction.get(userRef)

        if (!userSnapshot.exists()) {
            throw FirebaseFirestoreException("updateStreak: documents do not exist",
                FirebaseFirestoreException.Code.NOT_FOUND)
        }

        val user = userSnapshot.toObject<User>()
            ?: throw NullPointerException("updateStreak : User is null")
        val weeklyHistory = user.dailyStreak.weeklyHistory

        transaction.update(userRef, "dailyStreak.count",1)
        transaction.update(userRef, "dailyStreak.latestActive", activity.timeStamp)

        LocalDate.now()

        if(weeklyHistory!=null){
            val updatedWeekly = weeklyHistory.toMutableList().add(activity.timeStamp)
            transaction.update(userRef,"dailyStreak.weeklyHistory", updatedWeekly)
        } else {
            val newWeekly = mutableListOf<Timestamp>().add(activity.timeStamp)
            transaction.update(userRef,"dailyStreak.weeklyHistory", newWeekly)
        }
    }

    private suspend fun getTaskRelationRefs(
        activity: Activity,
        userId: String
    ): List<TaskRelationRef>{
        val taskRelationRefs = listOf(
            "distance" to activity.distance,
            "duration" to activity.duration,
            "stepCount" to activity.stepCount.toDouble(),
            "activityCount" to 1.0,
            "streakCount" to 1.0
        ).map { (type, addedProgress) ->
            val missionAndRelationRefs = fetchRefs(type, missionsRef, userMissionRelationsRef, userId)
            val achievementAndRelationRefs = fetchRefs(type, achievementsRef, userAchievementRelationsRef, userId)
            TaskRelationRef(missionAndRelationRefs + achievementAndRelationRefs, addedProgress)
        }
        return taskRelationRefs
    }

    private suspend fun fetchRefs(
        type: String,
        taskRefCollection: CollectionReference,
        relationRefCollection: CollectionReference,
        userId: String
    ): MutableList<Pair<DocumentReference,DocumentReference>> {
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

    suspend fun getRelationData(
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
                        val relationId = it.id
                        val relations  = it.toObject<Relation>()
                            ?: throw NullPointerException("Relations to object is null")
                        Pair(relationId, relations)
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

    suspend fun getTaskData(
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

    suspend fun getUserData(
        userId: String
    ): Flow<ResultState<User>> = callbackFlow {
        trySend(ResultState.Loading)
        val listener = usersRef.document(userId)
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
        batch.set(usersRef.document(uid), initialUser)
    }

    private suspend fun getAllTaskID(): List<Pair<String,TaskType>>{
        val missions = missionsRef.get().await()
        val achievements = achievementsRef.get().await()

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
        val randomRef = userAchievementRelationsRef.document().id
        batch.set(userAchievementRelationsRef.document(randomRef), relation)
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
        val randomRef = userMissionRelationsRef.document().id
        batch.set(userMissionRelationsRef.document(randomRef), relation)
    }

    /* Backup if needed

    private suspend fun getAssignedMissions(userId: String): List<DocumentReference> {
        val relations = userMissionRelations.whereEqualTo("users_ref", usersRef.document(userId)).get().await()

        return relations.mapNotNull { relation ->
            relation.getString("mission_ref")?.let {
                db.document(it)
            }
        }
    }

    private suspend fun getAssignedAchievements(userId: String): List<DocumentReference> {
        val relations = userAchievementRelations.whereEqualTo("users_ref", usersRef.document(userId)).get().await()

        return relations.mapNotNull { relation ->
            relation.getString("achievement_ref")?.let {
                db.document(it)
            }
        }
    }

    suspend fun assignNewMission(userId: String){
        val assignedMissions = getAssignedMissions(userId)
        getMissions().documents.mapNotNull{ mission ->
            if(!assignedMissions.contains(mission.reference)){
                addUserMissionRelation(userId, mission.id)
            }
        }
    }

    suspend fun assignNewAchievements(userId: String){
        val assignedAchievements = getAssignedAchievements(userId)
        getAchievements().documents.mapNotNull{ achievement ->
            if(!assignedAchievements.contains(achievement.reference)){
                addUserMissionRelation(userId, achievement.id)
            }
        }
    }
    */
    /*
    suspend fun getMissionData(missionId: String): Flow<ResultState<DailyMission>> = callbackFlow {
        trySend(ResultState.Loading)
        val listener = missionsRef.document(missionId)
            .addSnapshotListener { snapshot, error ->
                if(snapshot != null && error == null){
                    snapshot.toObject<DailyMission>().let {
                        if(it!=null) trySend(ResultState.Success(it))
                    }
                }
                else{
                    val exception = error?.message ?: "Mission is Null"
                    Log.e("FirestoreDataSource", "getMissionData : $exception")
                    trySend(ResultState.Error(exception))
                    return@addSnapshotListener
                }
                close()
            }
        awaitClose { listener.remove() }
    }
    */

    /*
    suspend fun getMissionsData(): Flow<List<DailyMission>> = callbackFlow {
        val listener = missionsRef
            .addSnapshotListener { snapshot, error ->
                if(snapshot != null && error == null){
                    trySend(snapshot.toObjects())
                }
                else{
                    val exception = error?.message ?: "Missions is Null"
                    Log.e("FirestoreDataSource", "getMissionData : $exception")
                    return@addSnapshotListener
                }
                close()
            }
        awaitClose { listener.remove() }
    }
     */

        /*
    suspend fun getUserMissionRelation(userId: String): Flow<List<UserMissionRelation>> = callbackFlow {
        val listener = userMissionRelations.whereEqualTo("users_ref", userId)
            .addSnapshotListener { snapshot, error ->
                if(snapshot != null && error == null){
                    trySend(snapshot.toObjects())
                }
                else{
                    val exception = error?.message ?: "UserMissonRelation is Null"
                    Log.e("FirestoreDataSource", "getUserMissionRelation : $exception")
                    return@addSnapshotListener
                }
                close()
            }
        awaitClose{ listener.remove() }
    }
     */
}
