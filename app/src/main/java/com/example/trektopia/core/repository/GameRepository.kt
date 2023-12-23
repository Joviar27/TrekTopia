package com.example.trektopia.core.repository

import com.example.trektopia.core.ResultState
import com.example.trektopia.core.data.FirestoreDataSource
import com.example.trektopia.core.model.Relation
import com.example.trektopia.core.model.Task
import com.example.trektopia.core.model.operation.TaskWithProgress
import com.example.trektopia.core.model.enum.TaskType
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GameRepository (
    private val firestore: FirestoreDataSource
) {

    fun resetDailyMission(userId: String) = firestore.resetDailyMission(userId)

    fun resetStreak(userId: String) = firestore.resetStreak(userId)

    fun getLeaderboard() = firestore.getLeaderboard()

    //Might delete later if not effective
    fun getUserRank(userPoint: Int) = firestore.getUserRank(userPoint)

    fun claimTaskReward(userId: String, relationId: String, reward: Int, taskType: TaskType) =
        firestore.claimTaskReward(userId, relationId, reward, taskType)

    private fun getTaskData(taskCollectionReference: CollectionReference) =
        firestore.getTaskData(taskCollectionReference)

    private fun getRelationData(userId: String, relationCollectionReference: CollectionReference) =
        firestore.getRelationData(userId, relationCollectionReference)

    private fun getTaskCollectionRef(taskType: TaskType) = firestore.getTaskCollectionRef(taskType)

    private fun getRelationCollectionRef(taskType: TaskType) = firestore.getRelationCollectionRef(taskType)

     fun getTaskWithProgress(
        userId: String,
        taskType: TaskType
    ): Flow<ResultState<List<TaskWithProgress>>> {
        val taskCollectionRef = getTaskCollectionRef(taskType)
        val relationCollectionRef = getRelationCollectionRef(taskType)

        val relationsFlow = getRelationData(userId, relationCollectionRef)
        val taskFlow = getTaskData(taskCollectionRef)

        return relationsFlow.combine(taskFlow) { listRelation, listMission ->
            combineResult(listRelation, listMission){ relation, task ->
                mapToTaskWithProgress(relation,task)
            }
        }
    }

    private inline fun <R,T,M> combineResult(
        result1: ResultState<R>,
        result2: ResultState<T>,
        combine: (ResultState<R>, ResultState<T>) -> ResultState<M>
    ): ResultState<M> {
        return when {
            result1 is ResultState.Error -> ResultState.Error(result1.error)
            result2 is ResultState.Error -> ResultState.Error(result2.error)
            result1 is ResultState.Loading || result2 is ResultState.Loading -> ResultState.Loading
            else -> {
                combine(result1,result2)
            }
        }
    }

    private fun mapToTaskWithProgress(
        userMissions: ResultState<List<Pair<String, Relation>>>,
        allMissions: ResultState<List<Task>>
    ): ResultState<List<TaskWithProgress>> {
        val relations = (userMissions as ResultState.Success).data
        val taskList = (allMissions as ResultState.Success).data

        val taskIdMap = taskList.associateBy { it.id }

        val taskProgress = relations.mapNotNull { relation ->
            val task = taskIdMap[relation.second.taskRef]
            if (task != null) {
                TaskWithProgress(
                    task = task,
                    progress = relation.second.progress,
                    activeDate = relation.second.activeDate,
                    relationId = relation.first
                )
            } else null
        }

        return if(taskProgress.size < relations.size || taskProgress.size < taskList.size)
            ResultState.Error("Some taskProgress is mission")
        else ResultState.Success(taskProgress)
    }
}