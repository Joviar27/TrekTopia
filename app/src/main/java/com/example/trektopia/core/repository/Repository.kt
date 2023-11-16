package com.example.trektopia.core.repository

import android.net.Uri
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.data.FirestoreDataSource
import com.example.trektopia.core.data.StorageDataSource
import com.example.trektopia.core.model.Activity
import com.example.trektopia.core.model.Relation
import com.example.trektopia.core.model.Task
import com.example.trektopia.core.model.TaskProgress
import com.example.trektopia.core.model.TaskType
import com.example.trektopia.core.model.User
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class Repository(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageDataSource: StorageDataSource
) {
    suspend fun getUserData(userId: String) = firestoreDataSource.getUserData(userId)

    private suspend fun getTaskData(taskCollectionReference: CollectionReference) =
        firestoreDataSource.getTaskData(taskCollectionReference)

    private suspend fun getRelationData(userId: String, relationCollectionReference: CollectionReference) =
        firestoreDataSource.getRelationData(userId, relationCollectionReference)

    fun resetDailyMission() = firestoreDataSource.resetDailyMission()

    fun resetStreak(userId: String) = firestoreDataSource.resetStreak(userId)

    //To show notification if user haven't done any activity
    fun checkLatestActiveDate(userId: String) = firestoreDataSource.checkLatestActiveDate(userId)

    fun getLeaderboard() = firestoreDataSource.getLeaderboard()

    //Might delete later if not effective
    fun getUserRank(userPoint: Int) = firestoreDataSource.getUserRank(userPoint)

    fun getUserActivities(userId: String) = firestoreDataSource.getUserActivities(userId)

    fun updateUserInfo(userId: String, newUser: User) =
        firestoreDataSource.updateUserInfo(userId,newUser)

    fun updateProfile(imageUri: Uri, userId: String) =
        storageDataSource.updateProfile(imageUri,userId)

    fun claimTaskReward(userId: String, relationId: String, taskId: String, taskType: TaskType) =
        firestoreDataSource.claimTaskReward(userId, relationId, taskId, taskType)

    fun addActivityAndUpdateProgress(userId: String, activity: Activity) =
        firestoreDataSource.addActivityAndUpdateProgress(activity, userId)
    suspend fun getTaskWithProgress(
        userId: String,
        taskType: TaskType
    ): Flow<ResultState<List<TaskProgress>>> {
        val taskCollectionRef = firestoreDataSource.getTaskCollectionRef(taskType)
        val relationCollectionRef = firestoreDataSource.getRelationCollectionRef(taskType)

        val relationsFlow = getRelationData(userId, relationCollectionRef)
        val taskFlow = getTaskData(taskCollectionRef)

        return relationsFlow.combine(taskFlow) { listRelation, listMission ->
            combineResult(listRelation, listMission){ relation, task ->
                mapToTaskRelation(relation,task)
            }
        }
    }

    private inline fun <R,T,M> combineResult(
        result1: ResultState<R>,
        result2: ResultState<T>,
        combine: (ResultState<R>, ResultState<T>) -> ResultState<M>
    ): ResultState<M>{
        return when {
            result1 is ResultState.Error -> ResultState.Error(result1.error)
            result2 is ResultState.Error -> ResultState.Error(result2.error)
            result1 is ResultState.Loading || result2 is ResultState.Loading -> ResultState.Loading
            else -> {
                combine(result1,result2)
            }
        }
    }

    private fun mapToTaskRelation(
        userMissions: ResultState<List<Pair<String,Relation>>>,
        allMissions: ResultState<List<Task>>
    ): ResultState<List<TaskProgress>> {
        val relations = (userMissions as ResultState.Success).data
        val taskList = (allMissions as ResultState.Success).data

        val taskIdMap = taskList.associateBy { it.id }

        val taskProgress = relations.mapNotNull { relation ->
            val task = taskIdMap[relation.second.taskRef]
            if (task != null) {
                TaskProgress(
                    task = task,
                    progress = relation.second.progress,
                    relationId = relation.first
                )
            } else null
        }

        return if(taskProgress.size < relations.size || taskProgress.size < taskList.size)
            ResultState.Error("Some taskProgress is mission")
        else ResultState.Success(taskProgress)
    }
}

/*
suspend fun getMissionsData(): Flow<List<DailyMission>> =
    firestoreDataSource.getMissionsData()
 */

/*
suspend fun getUserMissionRelation(userId: String): Flow<List<UserMissionRelation>> =
    firestoreDataSource.getUserMissionRelation(userId)
 */