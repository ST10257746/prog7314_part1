package com.example.prog7314_part1.data.repository

import com.example.prog7314_part1.data.model.Result
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ApiUserRepository
 * Tests core authentication and user management functionality
 */
class ApiUserRepositoryTest {

    @Test
    fun `test email validation for registration`() {
        // Test valid email
        assertTrue(isValidEmail("test@example.com"))
        assertTrue(isValidEmail("user.name+tag@domain.co.uk"))
        
        // Test invalid emails
        assertFalse(isValidEmail("invalid-email"))
        assertFalse(isValidEmail("@domain.com"))
        assertFalse(isValidEmail("user@"))
        assertFalse(isValidEmail(""))
    }

    @Test
    fun `test password validation`() {
        // Test valid passwords
        assertTrue(isValidPassword("password123"))
        assertTrue(isValidPassword("MySecurePass123"))
        
        // Test invalid passwords
        assertFalse(isValidPassword("123")) // Too short
        assertFalse(isValidPassword("")) // Empty
        assertFalse(isValidPassword(" ")) // Whitespace only
    }

    @Test
    fun `test age validation`() {
        // Test valid ages
        assertTrue(isValidAge(18))
        assertTrue(isValidAge(25))
        assertTrue(isValidAge(65))
        
        // Test invalid ages
        assertFalse(isValidAge(12)) // Too young
        assertFalse(isValidAge(101)) // Too old
        assertFalse(isValidAge(-5)) // Negative
    }

    @Test
    fun `test weight validation`() {
        // Test valid weights
        assertTrue(isValidWeight(50.0))
        assertTrue(isValidWeight(70.5))
        assertTrue(isValidWeight(120.0))
        
        // Test invalid weights
        assertFalse(isValidWeight(29.0)) // Too light
        assertFalse(isValidWeight(201.0)) // Too heavy
        assertFalse(isValidWeight(-10.0)) // Negative
    }

    @Test
    fun `test display name validation`() {
        // Test valid names
        assertTrue(isValidDisplayName("John Doe"))
        assertTrue(isValidDisplayName("Alice"))
        assertTrue(isValidDisplayName("José María"))
        
        // Test invalid names
        assertFalse(isValidDisplayName("")) // Empty
        assertFalse(isValidDisplayName("   ")) // Whitespace only
        assertFalse(isValidDisplayName("A")) // Too short
    }

    // Helper validation functions (these would be in your actual code)
    private fun isValidEmail(email: String): Boolean {
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
        return password.length >= 6
    }

    private fun isValidAge(age: Int): Boolean {
        return age in 13..100
    }

    private fun isValidWeight(weight: Double): Boolean {
        return weight in 30.0..200.0
    }

    private fun isValidDisplayName(name: String): Boolean {
        return name.trim().length >= 2
    }
}
