package com.example.trektopia.core.data

import com.example.trektopia.core.model.DailyStreak
import com.example.trektopia.core.model.User
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreDataSource(
    private val db: FirebaseFirestore
){

    private val usersRef = db.collection("users")
    private val missionsRef = db.collection("missions")
    private val achievementsRef = db.collection("achievements")
    private val userMissionRelations = db.collection("user_mission_relations")
    private val userAchievementRelations = db.collection("user_achievement_relations")

    private suspend fun getMissions(): QuerySnapshot = missionsRef.get().await()
    private suspend fun getAchievements(): QuerySnapshot = achievementsRef.get().await()

    suspend fun insertNewUser(uid: String, username: String, email: String){
        val initialStreak = DailyStreak(
            count = 0,
            latestActive = null,
            weeklyHistory = null
        )

        val initialUser = User(
            uid = uid,
            username = username,
            email = email,
            point = 0,
            pictureUri = null,
            dailyStreak = initialStreak
        )
        usersRef.document(uid).set(initialUser).await()
    }

    private suspend fun addUserMissionRelation(userId: String, missionId: String){
        val relation = hashMapOf(
            usersRef.document(userId) to "user_ref",
            achievementsRef.document(missionId) to "mission_ref"
        )
        userMissionRelations.add(relation).await()
    }

    private suspend fun addUserAchievementRelation(userId: String, achievementId: String){
        val relation = hashMapOf(
            usersRef.document(userId) to "user_ref",
            achievementsRef.document(achievementId) to "achievement_ref"
        )

        userAchievementRelations.add(relation).await()
    }

    suspend fun assignAllMissions(userId: String){
        for(mission in getMissions().documents){
            addUserMissionRelation(userId, mission.id)
        }
    }

    suspend fun assignAllAchievements(userId: String){
        for(achievement in getAchievements().documents){
            addUserAchievementRelation(userId, achievement.id)
        }
    }

    /* Backup if needed
    private suspend fun getAssignedMissions(userId: String): List<DocumentReference> {
        val relations = userMissionRelations.whereEqualTo("users_ref", usersRef.document(userId)).get().await()

        return relations.documents.map { relation ->
            relation.reference
        }
    }

    private suspend fun getAssignedAchievements(userId: String): List<DocumentReference> {
        val relations = userAchievementRelations.whereEqualTo("users_ref", usersRef.document(userId)).get().await()

        return relations.documents.map { relation ->
            relation.reference
        }
    }

    suspend fun assignNewMission(userId: String){
        val assignedMissions = getAssignedMissions(userId)
        getMissions().documents.map{ mission ->
            if(!assignedMissions.contains(mission.reference)){
                addUserMissionRelation(userId, mission.id)
            }
        }
    }

    suspend fun assignNewAchievements(userId: String){
        val assignedAchievements = getAssignedAchievements(userId)
        getAchievements().documents.map{ achievement ->
            if(!assignedAchievements.contains(achievement.reference)){
                addUserMissionRelation(userId, achievement.id)
            }
        }
    }
     */
}
