package com.cardify.app.data.api

import com.cardify.app.NetworkConfig
import com.cardify.app.data.UserSession
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = NetworkConfig.BASE_URL

    // יצירת אובייקט GSON סלחני כדי לטפל בשגיאות מבנה ב-JSON
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    // התיקון הקריטי: המשיכה של הטוקן מתבצעת בתוך ה-lambda של ה-Interceptor
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        // כאן אנחנו מוודאים שכל בקשה לוקחת את הטוקן הכי עדכני מ-UserSession
        val currentToken = UserSession.token

        val newRequest = if (!currentToken.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        } else {
            // אם אין טוקן (למשל לפני לוגין), שולחים את הבקשה המקורית
            originalRequest
        }
        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor) // ה-Interceptor המתוקן כאן
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