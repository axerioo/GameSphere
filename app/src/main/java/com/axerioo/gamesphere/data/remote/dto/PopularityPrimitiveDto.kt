package com.axerioo.gamesphere.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Popularity Primitive DTO ---
// Represents a single popularity primitive record from the IGDB API (`/popularity_primitives` endpoint).

@JsonClass(generateAdapter = true)
data class PopularityPrimitiveDto(
    val id: Long,
    @field:Json(name = "game_id")
    val gameId: Long,
    val value: Double
)