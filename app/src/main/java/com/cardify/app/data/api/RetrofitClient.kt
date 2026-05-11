package com.cardify.app.data.api

import android.content.Context
import android.content.Intent
import com.cardify.app.NetworkConfig
import com.cardify.app.data.UserSession
import com.cardify.app.ui.login.LoginActivity
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = NetworkConfig.BASE_URL
    private var appContext: Context? = null

    // פונקציית אתחול כדי שנוכל להשתמש ב-Context למעבר בין מסכים
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    // 1. אינטרספטור להוספת הטוקן לכל בקשה
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = UserSession.token

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        chain.proceed(newRequest)
    }

    // 2. אינטרספטור לזיהוי ניתוק (401) והחזרה ללוגין
    private val sessionInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        // בתוך sessionInterceptor ב-RetrofitClient.kt
        val isLoginRequest = request.url.encodedPath.contains("/login")
        if (response.code == 401 && !isLoginRequest) {
            UserSession.clear()

            appContext?.let { context ->
                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()

                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    // כאן אנחנו מוסיפים את ההודעה החמודה
                    putExtra("logout_reason", "session_expired")
                }
                context.startActivity(intent)
            }
        }
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(sessionInterceptor) // הוספת האינטרספטור החדש
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: AuthApiService = retrofit.create(AuthApiService::class.java)
}