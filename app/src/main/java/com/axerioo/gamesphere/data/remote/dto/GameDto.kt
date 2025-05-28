package com.axerioo.gamesphere.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Data Transfer Objects (DTOs) for IGDB API ---
// These data classes are used by Moshi to parse the JSON responses from the IGDB API.

// --- Main Game DTO ---
// Represents the primary game object received from the IGDB API.

@JsonClass(generateAdapter = true)
data class GameDto(
    val id: Long,               // Unique IGDB identifier for the game.
    val name: String?,          // Name of the game. Nullable as it might be missing.
    val url: String?,           // URL to the game's page on IGDB.com.

    // --- Fields primarily for list views (Explore, Soon, Search) ---

    val cover: CoverDto?,           // Nested object containing cover image information (specifically `image_id`).
    val popularity: Double?,        // Popularity score from IGDB.
    @field:Json(name = "aggregated_rating")
    val aggregatedRating: Double?,  // Aggregated rating from IGDB (0-100).
    @field:Json(name = "first_release_date")
    val firstReleaseDate: Long?,    // Timestamp (Unix, seconds) of the game's earliest release.

    // --- Fields primarily for detailed game views (Details screen) ---

    @field:Json(name = "involved_companies")
    val involvedCompanies: List<InvolvedCompanyDto>?,
    val summary: String?,
    val storyline: String?,
    val genres: List<GenreDto>?,
    val screenshots: List<ScreenshotDto>?,
    @field:Json(name = "age_ratings")
    val ageRatings: List<AgeRatingDto>?,
    @field:Json(name = "release_dates")
    val releaseDates: List<ReleaseDateDto>?
)

// --- Cover DTO ---
// Represents cover image information.

@JsonClass(generateAdapter = true)
data class CoverDto(
    val id: Long,
    @field:Json(name = "image_id")
    val imageId: String?

)

// --- Involved Company DTO ---
// Represents a company's involvement with a game (e.g., as a developer or publisher).

@JsonClass(generateAdapter = true)
data class InvolvedCompanyDto(
    val id: Long,
    val company: CompanyDto?,
    val developer: Boolean?,
    val publisher: Boolean?,
    val supporting: Boolean?
)

// --- Company DTO ---
// Represents a game company (developer, publisher, etc.).

@JsonClass(generateAdapter = true)
data class CompanyDto(
    val id: Long,
    val name: String?
)

// --- Genre DTO ---
// Represents a game genre.

@JsonClass(generateAdapter = true)
data class GenreDto(
    val id: Long,
    val name: String?
)

// --- Screenshot DTO ---
// Represents screenshot image information.

@JsonClass(generateAdapter = true)
data class ScreenshotDto(
    val id: Long,
    @field:Json(name = "image_id")
    val imageId: String?
)

// --- Age Rating DTO ---
// Represents an age rating from a specific rating board (ESRB, PEGI, etc.).

@JsonClass(generateAdapter = true)
data class AgeRatingDto(
    val id: Long,
    val category: Int?,
    val rating: Int?,
    @field:Json(name = "content_descriptions")
    val contentDescriptions: List<ContentDescriptionDto>?
)

// --- Content Description DTO ---
// Represents a specific content descriptor associated with an age rating.

@JsonClass(generateAdapter = true)
data class ContentDescriptionDto(
    val id: Int,
    val category: Int?,
    val description: String?
)

// --- Release Date DTO ---
// Represents a specific release date, often tied to a platform or region.

@JsonClass(generateAdapter = true)
data class ReleaseDateDto(
    val id: Long,
    val date: Long?,
    val human: String?
)