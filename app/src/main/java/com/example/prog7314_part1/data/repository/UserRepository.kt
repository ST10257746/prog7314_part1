package com.example.prog7314_part1.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.prog7314_part1.data.local.AppDatabase
import com.example.prog7314_part1.data.local.entity.User
import com.example.prog7314_part1.data.model.AuthState
import com.example.prog7314_part1.data.model.FirebaseUser
import com.example.prog7314_part1.data.model.Result
import com.example.prog7314_part1.data.model.toFirebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser as FirebaseAuthUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * UserRepository
 * Handles all user-related operations including authentication and data sync
 */
class UserRepository(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "FitTrackrPrefs"
        private const val KEY_USER_ID = "userId"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_LAST_LOGIN = "lastLogin"
        private const val USERS_COLLECTION = "users"
        private const val MAX_IMAGE_SIZE = 1024 // Max width/height for profile images
        private const val IMAGE_QUALITY = 80 // JPEG compression quality (0-100)
    }
    
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
     * Register new user with email and password
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        age: Int,
        weightKg: Double
    ): Result<User> {
        return try {
            // Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Failed to create user account")
            
            // Create user object
            val userId = firebaseUser.uid
            val user = User(
                userId = userId,
                email = email,
                displayName = displayName,
                age = age,
                weightKg = weightKg
            )
            
            // Save to Firestore
            saveUserToFirestore(user.toFirebaseUser())
            
            // Save to Room
            userDao.insertUser(user)
            
            // Save session
            saveSession(userId)
            
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Registration failed")
        }
    }
    
    /**
     * Login user with email and password
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Authenticate with Firebase
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Login failed")
            
            val userId = firebaseUser.uid
            
            // Fetch user data from Firestore
            val user = fetchUserFromFirestore(userId)
            
            // Save to Room
            userDao.insertUser(user.toUserEntity())
            
            // Save session
            saveSession(userId)
            
            Result.Success(user.toUserEntity())
        } catch (e: Exception) {
            Result.Error(e, e.message ?: "Login failed")
        }
    }
    
    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit> {
        return try {
            // Sign out from Firebase
            auth.signOut()
            
            // Sign out from Google
            getGoogleSignInClient().signOut().await()
            
            // Clear local database
            userDao.deleteAllUsers()
            
            // Clear session
            clearSession()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Logout failed")
        }
    }
    
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
     * @param account GoogleSignInAccount from sign-in intent
     * @return Result with User data or error
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
            
            // Check if user exists in Firestore
            val userExists = try {
                fetchUserFromFirestore(userId)
                true
            } catch (e: Exception) {
                false
            }
            
            val user: User
            if (userExists) {
                // Existing user - fetch from Firestore
                val firebaseUserData = fetchUserFromFirestore(userId)
                user = firebaseUserData.toUserEntity()
            } else {
                // New user - create with default values
                // They'll need to complete profile setup
                user = User(
                    userId = userId,
                    email = email,
                    displayName = displayName,
                    age = 0, // Will be set in profile setup
                    weightKg = 0.0, // Will be set in profile setup
                    profileImageUrl = firebaseUser.photoUrl?.toString()
                )
                
                // Save to Firestore
                saveUserToFirestore(user.toFirebaseUser())
            }
            
            // Save to Room
            userDao.insertUser(user)
            
            // Save session
            saveSession(userId)
            
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e, "Google sign-in failed: ${e.message}")
        }
    }
    
    /**
     * Complete Google Sign-In profile
     * Updates age, weight, and display name for a user who signed in with Google
     */
    suspend fun completeGoogleProfile(displayName: String, age: Int, weight: Double): Result<User> {
        return try {
            // Validate inputs
            if (displayName.isBlank()) {
                return Result.Error(Exception("Name is required"), "Please enter your name")
            }
            
            if (age < 13 || age > 100) {
                return Result.Error(Exception("Invalid age"), "Age must be between 13 and 100")
            }
            
            if (weight < 30 || weight > 200) {
                return Result.Error(Exception("Invalid weight"), "Weight must be between 30 and 200 kg")
            }
            
            // Get current user
            val currentUser = getCurrentUserSuspend() 
                ?: return Result.Error(Exception("No user logged in"), "Please sign in first")
            
            // Update user with profile data
            val updatedUser = currentUser.copy(
                displayName = displayName,
                age = age,
                weightKg = weight,
                updatedAt = System.currentTimeMillis()
            )
            
            // Update in Room
            userDao.updateUser(updatedUser)
            
            // Update in Firestore
            saveUserToFirestore(updatedUser.toFirebaseUser())
            
            Result.Success(updatedUser)
        } catch (e: Exception) {
            Result.Error(e, "Failed to complete profile: ${e.message}")
        }
    }
    
    /**
     * Get current user from Room database
     */
    fun getCurrentUser(): Flow<User?> = userDao.getCurrentUserFlow()
    
    /**
     * Get current user (suspend)
     */
    suspend fun getCurrentUserSuspend(): User? = userDao.getCurrentUser()
    
    /**
     * Update user profile
     */
    suspend fun updateUser(user: User): Result<User> {
        return try {
            // Update in Room
            userDao.updateUser(user.copy(updatedAt = System.currentTimeMillis()))
            
            // Update in Firestore
            saveUserToFirestore(user.toFirebaseUser())
            
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update user")
        }
    }
    
    /**
     * Update user goals
     */
    suspend fun updateUserGoals(
        dailyStepGoal: Int,
        dailyCalorieGoal: Int,
        dailyWaterGoal: Int,
        weeklyWorkoutGoal: Int,
        proteinGoalG: Int,
        carbsGoalG: Int,
        fatsGoalG: Int
    ): Result<Unit> {
        return try {
            val currentUser = getCurrentUserSuspend() ?: throw Exception("No user found")
            
            val updatedUser = currentUser.copy(
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
            
            // Update in Firestore
            saveUserToFirestore(updatedUser.toFirebaseUser())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update goals")
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
            // CRITICAL: Always use Firebase Auth's current user ID, not Room's potentially stale data
            val firebaseUser = auth.currentUser ?: throw Exception("No user logged in")
            val currentUserId = firebaseUser.uid
            
            // Get user from Room (might have old userId, so we'll override it)
            val roomUser = getCurrentUserSuspend() ?: throw Exception("No user found")
            
            // Create updated user with CORRECT userId from Firebase Auth
            val updatedUser = roomUser.copy(
                userId = currentUserId,  // Override with current Firebase Auth user ID
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
            
            // Update in Firestore (now using correct userId)
            saveUserToFirestore(updatedUser.toFirebaseUser())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to complete profile setup")
        }
    }
    
    /**
     * Sync user data from Firestore to Room
     */
    suspend fun syncUserData(): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("No user logged in")
            val userId = currentUser.uid
            
            // Fetch from Firestore
            val firebaseUser = fetchUserFromFirestore(userId)
            
            // Save to Room
            userDao.insertUser(firebaseUser.toUserEntity())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Sync failed")
        }
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && auth.currentUser != null
    }
    
    /**
     * Get current Firebase user
     */
    fun getCurrentFirebaseUser(): FirebaseAuthUser? = auth.currentUser
    
    /**
     * Upload profile image - convert to Base64 and store in Firestore
     * @param imageUri URI of the image to upload
     * @return Result with the Base64 encoded image string
     */
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("No user logged in")
            
            // Read the image from URI
            val inputStream: InputStream = context.contentResolver.openInputStream(imageUri)
                ?: throw Exception("Failed to open image")
            
            // Decode the image
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            // Resize image to reduce size
            val resizedBitmap = resizeImage(originalBitmap, MAX_IMAGE_SIZE)
            
            // Convert to Base64
            val base64Image = bitmapToBase64(resizedBitmap)
            
            // Clean up
            if (resizedBitmap != originalBitmap) {
                resizedBitmap.recycle()
            }
            originalBitmap.recycle()
            
            // Validate size (Firestore has 1MB limit per field)
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
     * @param imageBase64 Base64 encoded image string
     */
    suspend fun updateProfileImage(imageBase64: String): Result<Unit> {
        return try {
            val currentUser = getCurrentUserSuspend() 
                ?: throw Exception("No user data found")
            
            // Update Room database
            val updatedUser = currentUser.copy(profileImageUrl = imageBase64)
            userDao.updateUser(updatedUser)
            
            // Update Firestore
            saveUserToFirestore(updatedUser.toFirebaseUser())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update profile image")
        }
    }
    
    /**
     * Delete profile image from user profile
     */
    suspend fun deleteProfileImage(): Result<Unit> {
        return try {
            val currentUser = getCurrentUserSuspend() 
                ?: throw Exception("No user data found")
            
            // Update Room database
            val updatedUser = currentUser.copy(profileImageUrl = null)
            userDao.updateUser(updatedUser)
            
            // Update Firestore (remove profileImageUrl field)
            saveUserToFirestore(updatedUser.toFirebaseUser())
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to delete profile image")
        }
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Resize image to fit within maxSize while maintaining aspect ratio
     */
    private fun resizeImage(image: Bitmap, maxSize: Int): Bitmap {
        val width = image.width
        val height = image.height
        
        // If image is already small enough, return original
        if (width <= maxSize && height <= maxSize) {
            return image
        }
        
        // Calculate new dimensions
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
    
    /**
     * Convert Bitmap to Base64 encoded string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    private suspend fun saveUserToFirestore(user: FirebaseUser) {
        // Convert to map to ensure proper serialization
        val userMap = hashMapOf<String, Any?>(
            "userId" to user.userId,
            "email" to user.email,
            "displayName" to user.displayName,
            "age" to user.age,
            "weightKg" to user.weightKg,
            "createdAt" to user.createdAt,
            "updatedAt" to user.updatedAt
        )
        
        // Add optional fields only if they are not null
        user.heightCm?.let { userMap["heightCm"] = it }
        user.profileImageUrl?.let { userMap["profileImageUrl"] = it }
        user.dailyStepGoal?.let { userMap["dailyStepGoal"] = it }
        user.dailyCalorieGoal?.let { userMap["dailyCalorieGoal"] = it }
        user.dailyWaterGoal?.let { userMap["dailyWaterGoal"] = it }
        user.weeklyWorkoutGoal?.let { userMap["weeklyWorkoutGoal"] = it }
        user.proteinGoalG?.let { userMap["proteinGoalG"] = it }
        user.carbsGoalG?.let { userMap["carbsGoalG"] = it }
        user.fatsGoalG?.let { userMap["fatsGoalG"] = it }
        
        firestore.collection(USERS_COLLECTION)
            .document(user.userId)
            .set(userMap)
            .await()
    }
    
    private suspend fun fetchUserFromFirestore(userId: String): FirebaseUser {
        val document = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .await()
        
        return document.toObject(FirebaseUser::class.java)
            ?: throw Exception("User data not found")
    }
    
    private fun saveSession(userId: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            apply()
        }
    }
    
    private fun clearSession() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_LAST_LOGIN)
            apply()
        }
    }
    
    /**
     * Get user ID from session
     */
    fun getUserIdFromSession(): String? = prefs.getString(KEY_USER_ID, null)
}
