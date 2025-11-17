package com.example.prog7314_part1.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.User
import com.example.prog7314_part1.data.model.AuthState
import com.example.prog7314_part1.data.model.Result
import com.example.prog7314_part1.data.network.model.UpdateUserRequest
import com.example.prog7314_part1.data.network.model.UserDto
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * ApiUserRepository
 * User repository that uses REST API instead of direct Firestore
 * 
 * This is an example implementation showing how to integrate the REST API.
 * You can replace the existing UserRepository with this, or merge the logic.
 */
class ApiUserRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val auth = FirebaseAuth.getInstance()
    private val networkRepo = NetworkRepository(context)
    
    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                trySend(AuthState.Authenticated)
            } else {
                trySend(AuthState.Unauthenticated)
            }
        }
        
        auth.addAuthStateListener(authStateListener)
        
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }
    
    /**
     * Register new user using REST API
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        age: Int,
        weightKg: Double
    ): Result<User> {
        return try {
            android.util.Log.d("ApiUserRepository", "🔑 Step 1: Creating Firebase Auth user for: $email")
            // Step 1: Create Firebase Auth account (still needed for authentication)
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create auth account")
            android.util.Log.d("ApiUserRepository", "✅ Firebase Auth user created: ${firebaseUser.uid}")

            // Step 2: Call REST API to create user profile
            android.util.Log.d("ApiUserRepository", "🌐 Step 2: Calling API to create user profile...")
            when (val apiResult = networkRepo.registerUser(
                email = email,
                displayName = displayName,
                age = age,
                weightKg = weightKg
            )) {
                is Result.Success -> {
                    android.util.Log.d("ApiUserRepository", "✅ API registration successful!")
                    val userDto = apiResult.data
                    
                    // Step 3: Save to local Room database
                    android.util.Log.d("ApiUserRepository", "💾 Step 3: Saving to local database...")
                    val localUser = userDto.toUser()
                    userDao.insertUser(localUser)
                    android.util.Log.d("ApiUserRepository", "✅ Registration complete!")
                    
                    // Register FCM token for this user
                    try {
                        val fcmToken = FirebaseMessaging.getInstance().token.await()
                        networkRepo.registerFcmToken(fcmToken)
                    } catch (e: Exception) {
                        android.util.Log.w("ApiUserRepository", "⚠️ Failed to register FCM token: ${'$'}{e.message}")
                    }
                    
                    // Send welcome notification via backend/FCM
                    networkRepo.sendNotification(
                        title = "Welcome to FitTrackr",
                        body = "Your account has been created successfully!"
                    )

                    Result.Success(localUser)
                }
                is Result.Error -> {
                    // Rollback auth if API call failed
                    android.util.Log.e("ApiUserRepository", "❌ API registration failed: ${apiResult.message}")
                    android.util.Log.w("ApiUserRepository", "🔄 Rolling back Firebase Auth user...")
                    auth.currentUser?.delete()
                    apiResult
                }
                else -> Result.Error(Exception("Unexpected result"), "Registration failed")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ApiUserRepository", "💥 Exception during registration: ${e.message}", e)
            Result.Error(e, e.message ?: "Registration failed")
        }
    }
    
    /**
     * Login user using REST API
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Step 1: Authenticate with Firebase
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Login failed")
            
            // Step 2: Fetch user data from REST API
            when (val apiResult = networkRepo.loginUser()) {
                is Result.Success -> {
                    val userDto = apiResult.data
                    
                    // Step 3: Save to local Room database
                    val localUser = userDto.toUser()
                    userDao.insertUser(localUser)
                    
                    // Register FCM token for this user
                    try {
                        val fcmToken = FirebaseMessaging.getInstance().token.await()
                        networkRepo.registerFcmToken(fcmToken)
                    } catch (e: Exception) {
                        android.util.Log.w("ApiUserRepository", "⚠️ Failed to register FCM token on login: ${'$'}{e.message}")
                    }
                    
                    // Send login notification via backend/FCM
                    networkRepo.sendNotification(
                        title = "Login successful",
                        body = "Welcome back, ${localUser.displayName}!"
                    )

                    Result.Success(localUser)
                }
                is Result.Error -> apiResult
                else -> Result.Error(Exception("Unexpected result"), "Login failed")
            }
            
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Login failed")
        }
    }
    
    /**
     * Update user profile using REST API
     */
    suspend fun updateUser(user: User): Result<User> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            // Prepare updates map
            val updates = UpdateUserRequest(
                displayName = user.displayName,
                age = user.age,
                weightKg = user.weightKg,
                heightCm = user.heightCm,
                dailyStepGoal = user.dailyStepGoal,
                dailyCalorieGoal = user.dailyCalorieGoal,
                dailyWaterGoal = user.dailyWaterGoal,
                weeklyWorkoutGoal = user.weeklyWorkoutGoal,
                proteinGoalG = user.proteinGoalG,
                carbsGoalG = user.carbsGoalG,
                fatsGoalG = user.fatsGoalG
            )
            
            // Call REST API
            when (val apiResult = networkRepo.updateUserProfile(userId, updates)) {
                is Result.Success -> {
                    val updatedUserDto = apiResult.data
                    
                    // Update local database
                    val updatedUser = updatedUserDto.toUser()
                    userDao.updateUser(updatedUser)
                    
                    Result.Success(updatedUser)
                }
                is Result.Error -> apiResult
                else -> Result.Error(Exception("Unexpected result"), "Update failed")
            }
            
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Update failed")
        }
    }
    
    /**
     * Observe current user from local database
     */
    fun getCurrentUserFlow(): Flow<User?> {
        return userDao.getCurrentUserFlow()
    }
    
    /**
     * Logout user
     */
    suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            userDao.deleteAllUsers()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Logout failed")
        }
    }
    
    /**
     * Sync user data from API to local database
     */
    suspend fun syncUserData(): Result<User> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            when (val apiResult = networkRepo.getUserProfile(userId)) {
                is Result.Success -> {
                    val userDto = apiResult.data
                    val localUser = userDto.toUser()
                    userDao.insertUser(localUser)
                    Result.Success(localUser)
                }
                is Result.Error -> apiResult
                else -> Result.Error(Exception("Unexpected result"), "Sync failed")
            }
            
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Sync failed")
        }
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Get current user from Room database as Flow
     */
    fun getCurrentUser(): Flow<User?> = userDao.getCurrentUserFlow()
    
    /**
     * Get current user (suspend)
     */
    suspend fun getCurrentUserSuspend(): User? = userDao.getCurrentUser()
    
    /**
     * Get Google Sign-In client
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("110296443399-dvphird83f5konelr49lcbj45mhkdguo.apps.googleusercontent.com")
            .requestEmail()
            .build()
        
        return GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Sign in with Google account
     */
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            // Get credential from Google account
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            
            // Sign in to Firebase with credential
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Google sign-in failed")
            
            val userId = firebaseUser.uid
            val email = firebaseUser.email ?: throw Exception("Email not available")
            val displayName = firebaseUser.displayName ?: email.substringBefore("@")
            
            // Check if user exists via API
            val userExists = try {
                when (networkRepo.getUserProfile(userId)) {
                    is Result.Success -> true
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
            
            val user: User
            if (userExists) {
                when (val apiResult = networkRepo.getUserProfile(userId)) {
                    is Result.Success -> user = apiResult.data.toUser()
                    else -> throw Exception("Failed to fetch user data")
                }
            } else {
                when (val createResult = networkRepo.registerUser(
                    email = email,
                    displayName = displayName,
                    age = 0,
                    weightKg = 0.0,
                    profileImageUrl = firebaseUser.photoUrl?.toString()
                )) {
                    is Result.Success -> {
                        user = createResult.data.toUser()
                    }
                    else -> {
                        user = User(
                            userId = userId,
                            email = email,
                            displayName = displayName,
                            age = 0,
                            weightKg = 0.0,
                            profileImageUrl = firebaseUser.photoUrl?.toString()
                        )
                    }
                }
            }
            
            userDao.insertUser(user)

            try {
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                networkRepo.registerFcmToken(fcmToken)
            } catch (e: Exception) {
                android.util.Log.w("ApiUserRepository", "⚠️ Failed to register FCM token on Google sign-in: ${e.message}")
            }

            networkRepo.sendNotification(
                title = "Login successful",
                body = "Welcome back, ${user.displayName}!"
            )
            
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e, "Google sign-in failed: ${e.message}")
        }
    }
    
    /**
     * Complete Google Sign-In profile
     */
    suspend fun completeGoogleProfile(displayName: String, age: Int, weight: Double): Result<User> {
        return try {
            if (displayName.isBlank()) {
                return Result.Error(Exception("Name is required"), "Please enter your name")
            }
            
            if (age < 13 || age > 100) {
                return Result.Error(Exception("Invalid age"), "Age must be between 13 and 100")
            }
            
            if (weight < 30 || weight > 200) {
                return Result.Error(Exception("Invalid weight"), "Weight must be between 30 and 200 kg")
            }
            
            val currentUser = getCurrentUserSuspend() 
                ?: return Result.Error(Exception("No user logged in"), "Please sign in first")
            
            val updatedUser = currentUser.copy(
                displayName = displayName,
                age = age,
                weightKg = weight,
                updatedAt = System.currentTimeMillis()
            )
            
            // Update via API
            updateUser(updatedUser)
        } catch (e: Exception) {
            Result.Error(e, "Failed to complete profile: ${e.message}")
        }
    }
    
    /**
     * Complete user profile setup (goals + height)
     */
    suspend fun completeProfileSetup(
        heightCm: Double?,
        dailyStepGoal: Int,
        dailyCalorieGoal: Int,
        dailyWaterGoal: Int,
        weeklyWorkoutGoal: Int,
        proteinGoalG: Int,
        carbsGoalG: Int,
        fatsGoalG: Int
    ): Result<Unit> {
        return try {
            val firebaseUser = auth.currentUser ?: throw Exception("No user logged in")
            val currentUserId = firebaseUser.uid
            
            val roomUser = getCurrentUserSuspend() ?: throw Exception("No user found")
            
            val updatedUser = roomUser.copy(
                userId = currentUserId,
                heightCm = heightCm,
                dailyStepGoal = dailyStepGoal,
                dailyCalorieGoal = dailyCalorieGoal,
                dailyWaterGoal = dailyWaterGoal,
                weeklyWorkoutGoal = weeklyWorkoutGoal,
                proteinGoalG = proteinGoalG,
                carbsGoalG = carbsGoalG,
                fatsGoalG = fatsGoalG,
                updatedAt = System.currentTimeMillis()
            )
            
            // Update in Room
            userDao.updateUser(updatedUser)
            
            // Update via API
            val updates = UpdateUserRequest(
                displayName = updatedUser.displayName,
                age = updatedUser.age,
                weightKg = updatedUser.weightKg,
                heightCm = heightCm,
                dailyStepGoal = dailyStepGoal,
                dailyCalorieGoal = dailyCalorieGoal,
                dailyWaterGoal = dailyWaterGoal,
                weeklyWorkoutGoal = weeklyWorkoutGoal,
                proteinGoalG = proteinGoalG,
                carbsGoalG = carbsGoalG,
                fatsGoalG = fatsGoalG
            )
            when (networkRepo.updateUserProfile(currentUserId, updates)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> Result.Error(Exception("API update failed"), "Failed to sync with server")
                else -> Result.Error(Exception("Unexpected result"), "Update failed")
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to complete profile setup")
        }
    }
    
    /**
     * Upload profile image - convert to Base64
     */
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        return try {
            auth.currentUser ?: throw Exception("No user logged in")
            
            val inputStream: InputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Failed to open image")
            
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            val resizedBitmap = resizeImage(originalBitmap, MAX_IMAGE_SIZE)
            val base64Image = bitmapToBase64(resizedBitmap)
            
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }
            originalBitmap.recycle()
            
            if (base64Image.length > 1_000_000) {
                throw Exception("Image too large. Please choose a smaller image.")
            }
            
            Result.Success(base64Image)
        } catch (e: Exception) {
            Result.Error(e, "Failed to process image: ${e.message}")
        }
    }
    
    /**
     * Update user profile image
     */
    suspend fun updateProfileImage(imageBase64: String): Result<Unit> {
        return try {
            val currentUser = getCurrentUserSuspend() 
                ?: throw Exception("No user data found")
            
            val updatedUser = currentUser.copy(profileImageUrl = imageBase64)
            userDao.updateUser(updatedUser)
            
            // Update via API
            val updates = UpdateUserRequest(profileImageUrl = imageBase64)
            when (networkRepo.updateUserProfile(currentUser.userId, updates)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> Result.Error(Exception("API update failed"), "Failed to sync with server")
                else -> Result.Error(Exception("Unexpected result"), "Update failed")
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to update profile image")
        }
    }
    
    /**
     * Delete profile image
     */
    suspend fun deleteProfileImage(): Result<Unit> {
        return try {
            val currentUser = getCurrentUserSuspend() 
                ?: throw Exception("No user data found")
            
            val updatedUser = currentUser.copy(profileImageUrl = null)
            userDao.updateUser(updatedUser)
            
            // Update via API
            val updates = UpdateUserRequest(profileImageUrl = null)
            when (networkRepo.updateUserProfile(currentUser.userId, updates)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> Result.Error(Exception("API update failed"), "Failed to sync with server")
                else -> Result.Error(Exception("Unexpected result"), "Update failed")
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete profile image")
        }
    }
    
    // Helper methods
    private fun resizeImage(image: Bitmap, maxSize: Int): Bitmap {
        val width = image.width
        val height = image.height
        
        if (width <= maxSize && height <= maxSize) {
            return image
        }
        
        val ratio: Float = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(image, newWidth, newHeight, true)
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    companion object {
        private const val MAX_IMAGE_SIZE = 800 // pixels
        private const val IMAGE_QUALITY = 85 // 0-100
    }
}

/**
 * Extension function to convert UserDto to User entity
 */
private fun UserDto.toUser() = User(
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

