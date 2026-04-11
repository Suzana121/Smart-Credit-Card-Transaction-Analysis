package com.cardify.app.data.api

import com.cardify.app.data.UserSession
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Converts a network-related [Exception] to a concise, user-facing error message.
 *
 * Differentiates between no-internet, timeout, server-unreachable, and generic errors so
 * that UI layers can display a meaningful message without leaking internal stack traces.
 *
 * @return A short string suitable for display in a Toast or Snackbar.
 */
fun Exception.toUserMessage(): String = when (this) {
    is java.net.UnknownHostException  -> "No internet connection. Please check your network settings."
    is java.net.SocketTimeoutException -> "Connection timed out. Please try again."
    is java.net.ConnectException       -> "Unable to connect to the server. Please try again later."
    is javax.net.ssl.SSLException      -> "Secure connection failed. Please try again."
    else                               -> "An unexpected error occurred. Please try again."
}

/**
 * Singleton that provides the configured [AuthApiService] Retrofit instance.
 *
 * Responsibilities:
 * - Attaches the current JWT bearer token from [UserSession] to every outgoing request
 *   via an [Interceptor], so the token is always read fresh on each call.
 * - Configures lenient JSON parsing to tolerate minor server-side format variations.
 * - Logs full request/response bodies in debug builds via [HttpLoggingInterceptor].
 */
object RetrofitClient {

    /** Base URL of the backend API. All endpoint paths are resolved relative to this. */
    private const val BASE_URL = "http://127.0.0.1:5001/auth/"

    /** Lenient Gson instance that does not throw on malformed JSON values. */
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    /**
     * OkHttp interceptor that reads [UserSession.token] at request time and attaches it
     * as a `Authorization: Bearer <token>` header. If no token is present (e.g. before
     * login), the request is forwarded unmodified.
     */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val currentToken = UserSession.token
        val newRequest = if (!currentToken.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        } else {
            originalRequest
        }
        chain.proceed(newRequest)
    }

    /** Logs HTTP request and response bodies at [HttpLoggingInterceptor.Level.BODY] level. */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /** OkHttp client shared across all API calls, with a 30-second timeout on all operations. */
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
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    /** The application-wide [AuthApiService] instance used by all repositories and ViewModels. */
    val apiService: AuthApiService = retrofit.create(AuthApiService::class.java)
}
