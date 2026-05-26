package com.pingcorp.upcycleconnect

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pingcorp.upcycleconnect.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

object RetrofitClient {
    val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val authApi: AuthApiService by lazy {
        createApi(AuthApiService::class.java)
    }

    val api: ApiService by lazy {
        createApi(ApiService::class.java)
    }

    val geminiApi: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun <T> createApi(serviceClass: Class<T>): T {
        val apiUrl = BuildConfig.API_URL
        val baseUrl = when {
            apiUrl.startsWith("http") -> if (apiUrl.endsWith("/")) apiUrl else "$apiUrl/"
            else -> "http://$apiUrl/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(serviceClass)
    }
}