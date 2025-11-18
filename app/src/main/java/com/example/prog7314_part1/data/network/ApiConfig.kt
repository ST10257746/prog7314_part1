package com.example.prog7314_part1.data.network

/**
 * API Configuration
 * Contains base URLs and configuration for REST API
 */
object ApiConfig {
    
    /**
     * Hosted API Base URL
     * Production API hosted on Render.com
     */
    const val BASE_URL = "https://fittrackr-api-c87x.onrender.com/"
    
    // For local development, uncomment and use:
    // const val BASE_URL = "http://192.168.8.79:3000/"  // Physical device
    // const val BASE_URL = "http://10.0.2.2:3000/"     // Emulator
    
    // API endpoints
    object Endpoints {
        const val USERS = "api/users"
        const val WORKOUTS = "api/workouts"
        const val GOALS = "api/goals"
        const val PROGRESS = "api/progress"
        const val NUTRITION = "api/nutrition"
    }
    
    // Timeouts
    const val CONNECT_TIMEOUT = 30L // seconds
    const val READ_TIMEOUT = 30L // seconds
    const val WRITE_TIMEOUT = 30L // seconds
}

