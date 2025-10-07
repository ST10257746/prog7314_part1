package com.example.prog7314_part1.data.network

/**
 * API Configuration
 * Contains base URLs and configuration for REST API
 */
object ApiConfig {
    
    // Local development: Android emulator accessing localhost:3000
    // For physical device, use: "http://YOUR_COMPUTER_IP:3000/"
    // For production: "https://your-api.render.com/"
    const val BASE_URL = "http://10.0.2.2:3000/"
    
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

