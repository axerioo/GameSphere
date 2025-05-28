package com.axerioo.gamesphere.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// --- GameEntity Data Class ---
// This data class represents a single row in the 'games' table in the local Room database.

@Entity(tableName = "games")
data class GameEntity(
    // --- Primary Key ---
    @PrimaryKey val id: Long,

    // --- Game Information ---
    // The name of the game. This field is non-nullable.
    val name: String,
    // The remote URL for the game's cover image.
    val coverUrl: String?,
    // The local file system path to the saved cover image.
    val localCoverPath: String?,
    // A summary or short description of the game.
    val summary: String?,
    // A comma-separated string of genre names associated with the game.
    val genres: String?,
    // The game's release date, stored as a Unix timestamp (seconds since epoch).
    val releaseDateTimestamp: Long?,
    // A comma-separated string of URLs for game screenshots.
    val screenshotsUrls: String?,
    // The aggregated rating from IGDB (or another API source).
    val aggregatedRating: Double?,
    // Information about age ratings for the game.
    val ageRatings: String?,
    // The user's personal rating for the game (e.g., 0-10 stars).
    val userRating: Float?,
    // The name of the game's developer(s).
    val developerName: String?,
    // The URL to the game's page on IGDB.com.
    val igdbUrl: String?
)