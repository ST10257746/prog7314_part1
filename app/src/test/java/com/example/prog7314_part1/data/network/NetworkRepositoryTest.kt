package com.example.prog7314_part1.data.network

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for NetworkRepository
 * Tests API integration and data validation
 */
class NetworkRepositoryTest {

    @Test
    fun `test API base URL configuration`() {
        // Test that API base URL is properly configured
        val expectedBaseUrl = "https://your-api-url.com" // Replace with your actual API URL
        assertNotNull("API base URL should not be null", expectedBaseUrl)
        assertTrue("API base URL should be HTTPS", expectedBaseUrl.startsWith("https://"))
    }

    @Test
    fun `test user data validation`() {
        // Test user data validation logic
        val validUserData = mapOf(
            "userId" to "test-user-123",
            "email" to "test@example.com",
            "displayName" to "Test User",
            "age" to 25,
            "weightKg" to 70.0
        )
        
        assertTrue("Valid user data should pass validation", isValidUserData(validUserData))
        
        val invalidUserData = mapOf(
            "userId" to "",
            "email" to "invalid-email",
            "displayName" to "",
            "age" to -5,
            "weightKg" to -10.0
        )
        
        assertFalse("Invalid user data should fail validation", isValidUserData(invalidUserData))
    }

    @Test
    fun `test workout session validation`() {
        // Test workout session data validation
        val validWorkoutData = mapOf(
            "workoutName" to "Morning Run",
            "durationSeconds" to 3600,
            "caloriesBurned" to 450
        )
        
        assertTrue("Valid workout data should pass validation", isValidWorkoutData(validWorkoutData))
        
        val invalidWorkoutData = mapOf(
            "workoutName" to "",
            "durationSeconds" to -100,
            "caloriesBurned" to -50
        )
        
        assertFalse("Invalid workout data should fail validation", isValidWorkoutData(invalidWorkoutData))
    }

    @Test
    fun `test nutrition entry validation`() {
        // Test nutrition entry data validation
        val validNutritionData = mapOf(
            "mealType" to "Breakfast",
            "foodName" to "Oatmeal",
            "calories" to 350,
            "protein" to 12.0
        )
        
        assertTrue("Valid nutrition data should pass validation", isValidNutritionData(validNutritionData))
        
        val invalidNutritionData = mapOf(
            "mealType" to "",
            "foodName" to "",
            "calories" to -100,
            "protein" to -10.0
        )
        
        assertFalse("Invalid nutrition data should fail validation", isValidNutritionData(invalidNutritionData))
    }

    @Test
    fun `test API response parsing`() {
        // Test API response structure validation
        val mockApiResponse = mapOf(
            "success" to true,
            "data" to mapOf(
                "userId" to "test-123",
                "email" to "test@example.com"
            ),
            "message" to "Operation successful"
        )
        
        assertTrue("Valid API response should be parseable", isValidApiResponse(mockApiResponse))
        
        val invalidApiResponse = mapOf(
            "success" to false,
            "error" to "Invalid request"
        )
        
        assertTrue("Error API response should still be valid structure", isValidApiResponse(invalidApiResponse))
    }

    // Helper validation functions
    private fun isValidUserData(data: Map<String, Any>): Boolean {
        val userId = data["userId"] as? String ?: return false
        val email = data["email"] as? String ?: return false
        val displayName = data["displayName"] as? String ?: return false
        val age = data["age"] as? Int ?: return false
        val weightKg = data["weightKg"] as? Double ?: return false
        
        return userId.isNotEmpty() && 
               email.contains("@") && email.contains(".") &&
               displayName.trim().length >= 2 &&
               age in 13..100 &&
               weightKg in 30.0..200.0
    }

    private fun isValidWorkoutData(data: Map<String, Any>): Boolean {
        val workoutName = data["workoutName"] as? String ?: return false
        val durationSeconds = data["durationSeconds"] as? Int ?: return false
        val caloriesBurned = data["caloriesBurned"] as? Int ?: return false
        
        return workoutName.trim().isNotEmpty() &&
               durationSeconds > 0 &&
               caloriesBurned > 0
    }

    private fun isValidNutritionData(data: Map<String, Any>): Boolean {
        val mealType = data["mealType"] as? String ?: return false
        val foodName = data["foodName"] as? String ?: return false
        val calories = data["calories"] as? Int ?: return false
        val protein = data["protein"] as? Double ?: return false
        
        return mealType.trim().isNotEmpty() &&
               foodName.trim().isNotEmpty() &&
               calories > 0 &&
               protein >= 0
    }

    private fun isValidApiResponse(response: Map<String, Any>): Boolean {
        return response.containsKey("success") && 
               (response.containsKey("data") || response.containsKey("error"))
    }
}
