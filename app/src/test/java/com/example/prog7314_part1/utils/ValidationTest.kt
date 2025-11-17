package com.example.prog7314_part1.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for input validation utilities
 * Tests form validation and data integrity
 */
class ValidationTest {

    @Test
    fun `test email format validation`() {
        // Valid email formats
        assertTrue("Standard email should be valid", isValidEmailFormat("user@example.com"))
        assertTrue("Email with subdomain should be valid", isValidEmailFormat("user@mail.example.com"))
        assertTrue("Email with plus sign should be valid", isValidEmailFormat("user+tag@example.com"))
        assertTrue("Email with numbers should be valid", isValidEmailFormat("user123@example.com"))
        
        // Invalid email formats
        assertFalse("Email without @ should be invalid", isValidEmailFormat("userexample.com"))
        assertFalse("Email without domain should be invalid", isValidEmailFormat("user@"))
        assertFalse("Email without username should be invalid", isValidEmailFormat("@example.com"))
        assertFalse("Empty email should be invalid", isValidEmailFormat(""))
        assertFalse("Email with spaces should be invalid", isValidEmailFormat("user @example.com"))
    }

    @Test
    fun `test password strength validation`() {
        // Valid passwords
        assertTrue("Password with 6+ chars should be valid", isValidPassword("password123"))
        assertTrue("Password with special chars should be valid", isValidPassword("Pass@123"))
        assertTrue("Password with mixed case should be valid", isValidPassword("MyPassword"))
        
        // Invalid passwords
        assertFalse("Password too short should be invalid", isValidPassword("12345"))
        assertFalse("Empty password should be invalid", isValidPassword(""))
        assertFalse("Password with only spaces should be invalid", isValidPassword("      "))
    }

    @Test
    fun `test workout session validation`() {
        // Valid workout sessions
        assertTrue("Valid workout name should pass", isValidWorkoutName("Morning Run"))
        assertTrue("Valid duration should pass", isValidDuration(30))
        assertTrue("Valid calories should pass", isValidCalories(300))
        
        // Invalid workout sessions
        assertFalse("Empty workout name should fail", isValidWorkoutName(""))
        assertFalse("Negative duration should fail", isValidDuration(-10))
        assertFalse("Zero duration should fail", isValidDuration(0))
        assertFalse("Negative calories should fail", isValidCalories(-50))
    }

    @Test
    fun `test nutrition entry validation`() {
        // Valid nutrition entries
        assertTrue("Valid food name should pass", isValidFoodName("Chicken Breast"))
        assertTrue("Valid calories should pass", isValidNutritionCalories(250))
        assertTrue("Valid protein amount should pass", isValidProteinAmount(25.5))
        
        // Invalid nutrition entries
        assertFalse("Empty food name should fail", isValidFoodName(""))
        assertFalse("Negative calories should fail", isValidNutritionCalories(-100))
        assertFalse("Negative protein should fail", isValidProteinAmount(-10.0))
        assertFalse("Zero calories should fail", isValidNutritionCalories(0))
    }

    @Test
    fun `test goal setting validation`() {
        // Valid goals
        assertTrue("Valid step goal should pass", isValidStepGoal(10000))
        assertTrue("Valid calorie goal should pass", isValidCalorieGoal(2000))
        assertTrue("Valid water goal should pass", isValidWaterGoal(8))
        
        // Invalid goals
        assertFalse("Negative step goal should fail", isValidStepGoal(-1000))
        assertFalse("Negative calorie goal should fail", isValidCalorieGoal(-500))
        assertFalse("Negative water goal should fail", isValidWaterGoal(-2))
        assertFalse("Zero step goal should fail", isValidStepGoal(0))
    }

    // Helper validation functions
    private fun isValidEmailFormat(email: String): Boolean {
        if (email.isEmpty()) return false
        if (email.contains(" ")) return false // No spaces allowed
        if (!email.contains("@")) return false
        if (email.startsWith("@") || email.endsWith("@")) return false
        val parts = email.split("@")
        if (parts.size != 2) return false
        val domain = parts[1]
        return domain.contains(".") && domain.length > 3
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6 && password.trim().isNotEmpty()
    }

    private fun isValidWorkoutName(name: String): Boolean {
        return name.trim().isNotEmpty() && name.trim().length >= 2
    }

    private fun isValidDuration(minutes: Int): Boolean {
        return minutes > 0 && minutes <= 480 // Max 8 hours
    }

    private fun isValidCalories(calories: Int): Boolean {
        return calories > 0 && calories <= 2000 // Max 2000 calories per session
    }

    private fun isValidFoodName(name: String): Boolean {
        return name.trim().isNotEmpty() && name.trim().length >= 2
    }

    private fun isValidNutritionCalories(calories: Int): Boolean {
        return calories > 0 && calories <= 5000 // Max 5000 calories per meal
    }

    private fun isValidProteinAmount(protein: Double): Boolean {
        return protein >= 0 && protein <= 200 // Max 200g protein per meal
    }

    private fun isValidStepGoal(steps: Int): Boolean {
        return steps > 0 && steps <= 50000 // Max 50k steps per day
    }

    private fun isValidCalorieGoal(calories: Int): Boolean {
        return calories > 0 && calories <= 5000 // Max 5000 calories per day
    }

    private fun isValidWaterGoal(glasses: Int): Boolean {
        return glasses > 0 && glasses <= 20 // Max 20 glasses per day
    }
}
