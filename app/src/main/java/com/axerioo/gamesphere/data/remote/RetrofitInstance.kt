package com.axerioo.gamesphere.data.remote

import com.axerioo.gamesphere.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

// --- RetrofitInstance Object ---
// This object provides a singleton instance of the Retrofit service, configured for IGDB API.

object RetrofitInstance {

    // --- Base URL ---
    private const val BASE_URL = "https://api.igdb.com/"

    // --- API Credentials ---
    const val CLIENT_ID = BuildConfig.CLIENT_ID                         // TODO: Set your IGDB client ID in local.properties
    const val TWITCH_ACCESS_TOKEN = BuildConfig.TWITCH_ACCESS_TOKEN     // TODO: Set your Twitch access token in local.properties

    // --- HTTP Logging Interceptor ---
    // Configures an OkHttp interceptor to log HTTP request and response data.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // --- OkHttpClient ---
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)         // Logs network requests and responses.
        .connectTimeout(30, TimeUnit.SECONDS)   // Timeout for establishing a connection.
        .readTimeout(30, TimeUnit.SECONDS)      // Timeout for reading data from the server.
        .writeTimeout(30, TimeUnit.SECONDS)     // Timeout for writing data to the server.
        .build()

    // --- Moshi Setup ---
    // Configures the Moshi instance for JSON parsing.
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // --- Retrofit API Service Instance ---
    // Lazily initializes the `IgdbApiService` instance using Retrofit.
    val api: IgdbApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // Sets the base URL for the API.
            .client(okHttpClient) // Sets the custom configured OkHttpClient.
            .addConverterFactory(MoshiConverterFactory.create(moshi)) // Adds Moshi as the JSON converter.
            .build() // Creates the Retrofit instance.
            .create(IgdbApiService::class.java) // Creates an implementation of the IgdbApiService interface.
    }
}