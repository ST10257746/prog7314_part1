package com.example.prog7314_part1.data.network

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit Client Singleton
 * Provides configured Retrofit instance with Firebase Auth token injection
 */
object RetrofitClient {
    
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Interceptor to add Firebase ID token to all requests
     */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // Get current Firebase user token
        val currentUser = auth.currentUser
        val token = try {
            // Get token using coroutines in a blocking call
            if (currentUser != null) {
                runBlocking {
                    currentUser.getIdToken(false).await().token
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("RetrofitClient", "Error getting ID token: ${e.message}", e)
            null
        }
        
        // Add Authorization header if token exists
        val request = if (token != null) {
            android.util.Log.d("RetrofitClient", "✅ Adding auth token to: ${originalRequest.url}")
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            android.util.Log.w("RetrofitClient", "⚠️ No token for: ${originalRequest.url}")
            originalRequest
        }
        
        chain.proceed(request)
    }
    
    /**
     * Logging interceptor for debugging
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    /**
     * OkHttp client with interceptors
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    /**
     * Retrofit instance
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Get API service
     */
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
    
    /**
     * Convenience inline function
     */
    inline fun <reified T> create(): T = createService(T::class.java)
}

