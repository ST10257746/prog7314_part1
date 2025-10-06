package com.example.prog7314_part1.data.model

/**
 * Firebase User Model
 * Plain Kotlin data class for Firestore (no Room annotations)
 */
data class FirebaseUser(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val age: Int = 0,
    val weightKg: Double = 0.0,
    val heightCm: Double? = null,
    val profileImageUrl: String? = null,
    
    // Goals (nullable until user sets them in onboarding)
    val dailyStepGoal: Int? = null,
    val dailyCalorieGoal: Int? = null,
    val dailyWaterGoal: Int? = null,
    val weeklyWorkoutGoal: Int? = null,
    val proteinGoalG: Int? = null,
    val carbsGoalG: Int? = null,
    val fatsGoalG: Int? = null,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if user has completed goal setup
     */
    fun hasCompletedGoalSetup(): Boolean {
        return dailyStepGoal != null && 
               dailyCalorieGoal != null && 
               dailyWaterGoal != null &&
               weeklyWorkoutGoal != null
    }
    
    /**
     * Convert to Room User entity
     */
    fun toUserEntity() = com.example.prog7314_part1.data.local.entity.User(
        userId = userId,
        email = email,
        displayName = displayName,
        age = age,
        weightKg = weightKg,
        heightCm = heightCm,
        profileImageUrl = profileImageUrl,
        dailyStepGoal = dailyStepGoal,
        dailyCalorieGoal = dailyCalorieGoal,
        dailyWaterGoal = dailyWaterGoal,
        weeklyWorkoutGoal = weeklyWorkoutGoal,
        proteinGoalG = proteinGoalG,
        carbsGoalG = carbsGoalG,
        fatsGoalG = fatsGoalG,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastSyncedAt = System.currentTimeMillis()
    )
}

/**
 * Extension function to convert Room User entity to Firebase model
 */
fun com.example.prog7314_part1.data.local.entity.User.toFirebaseUser() = FirebaseUser(
    userId = userId,
    email = email,
    displayName = displayName,
    age = age,
    weightKg = weightKg,
    heightCm = heightCm,
    profileImageUrl = profileImageUrl,
    dailyStepGoal = dailyStepGoal,
    dailyCalorieGoal = dailyCalorieGoal,
    dailyWaterGoal = dailyWaterGoal,
    weeklyWorkoutGoal = weeklyWorkoutGoal,
    proteinGoalG = proteinGoalG,
    carbsGoalG = carbsGoalG,
    fatsGoalG = fatsGoalG,
    createdAt = createdAt,
    updatedAt = updatedAt
)

