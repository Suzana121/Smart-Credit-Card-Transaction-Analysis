package com.cardify.app.data.api

import com.cardify.app.data.UserSession // הייבוא של הקובץ ששלחת לי
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // נשארים עם localhost כי את עובדת עם הטאבלט ו-adb
    private const val BASE_URL = "http://localhost:5001/"

    // 1. יצרנו את "השומר" שמוסיף את הטוקן לכל בקשה
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = UserSession.token // לוקחים את הטוקן מהסשן

        val newRequest = if (token != null) {
            // אם יש טוקן, מוסיפים אותו ל"כרטיס הביקור" (Header)
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            // אם אין טוקן, שולחים רגיל
            originalRequest
        }

        chain.proceed(newRequest)
    }

    // הלוגר שעוזר לנו לראות מה קורה (נשאר אותו דבר)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 2. מוסיפים את השומר (authInterceptor) לרשימה
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor) // הנה ההוספה החשובה!
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: AuthApiService = retrofit.create(AuthApiService::class.java)
}