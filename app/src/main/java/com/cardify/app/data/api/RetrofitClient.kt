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

    // ה-Interceptor שמוסיף את הטוקן אוטומטית לכל Header
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

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson)) // הוספת ה-GSON המותאם כאן
        .build()

    val apiService: AuthApiService = retrofit.create(AuthApiService::class.java)
}