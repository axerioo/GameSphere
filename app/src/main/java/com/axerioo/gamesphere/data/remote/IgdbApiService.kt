package com.axerioo.gamesphere.data.remote

import com.axerioo.gamesphere.data.remote.dto.GameDto
import com.axerioo.gamesphere.data.remote.dto.PopularityPrimitiveDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

// --- IgdbApiService Interface ---
// This interface defines the API endpoints for interacting with the IGDB API using Retrofit.

interface IgdbApiService {

    // --- Get Games Endpoint ---
    // Fetches a list of games based on the provided query.

    @Headers("Content-Type: text/plain;charset=UTF-8")
    @POST("v4/games")
    suspend fun getGames(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authorization: String,
        @Body queryBody: RequestBody
    ): Response<List<GameDto>>

    // --- Get Popularity Primitives Endpoint ---
    // Fetches a list of popularity primitive records based on the provided query.

    @Headers("Content-Type: text/plain;charset=UTF-8")
    @POST("v4/popularity_primitives")
    suspend fun getPopularityPrimitives(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authorization: String,
        @Body queryBody: RequestBody
    ): Response<List<PopularityPrimitiveDto>>
}