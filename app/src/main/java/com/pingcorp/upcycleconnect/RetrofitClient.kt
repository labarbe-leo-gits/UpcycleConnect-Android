package com.pingcorp.upcycleconnect

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.pingcorp.upcycleconnect.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

object RetrofitClient {
    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val authApi: AuthApiService by lazy {
        val apiUrl = BuildConfig.API_URL
        val baseUrl = when {
            apiUrl.startsWith("http") -> if (apiUrl.endsWith("/")) apiUrl else "$apiUrl/"
            else -> "http://$apiUrl/"
        }

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApiService::class.java)
    }
}