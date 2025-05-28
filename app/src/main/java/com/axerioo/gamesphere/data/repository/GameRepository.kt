package com.axerioo.gamesphere.data.repository

import android.util.Log
import com.axerioo.gamesphere.data.local.GameDao
import com.axerioo.gamesphere.data.local.GameEntity
import com.axerioo.gamesphere.data.remote.IgdbApiService
import com.axerioo.gamesphere.data.remote.RetrofitInstance
import com.axerioo.gamesphere.data.remote.dto.GameDto
import com.axerioo.gamesphere.data.remote.utils.IgdbImageUtil
import com.axerioo.gamesphere.domain.model.FormattedAgeRating
import com.axerioo.gamesphere.domain.model.Game
import com.axerioo.gamesphere.domain.model.GameDetails
import com.axerioo.gamesphere.ui.screens.search.SearchFilters
import com.axerioo.gamesphere.ui.RateLimiter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- GameRepository Class ---
// This class serves as the single source of truth for game-related data.

class GameRepository(
    private val gameDao: GameDao,
    private val igdbApiService: IgdbApiService,
    private val imageStorageManager: ImageStorageManager
) {

    // --- API Credentials and Constants ---

    private val clientId = RetrofitInstance.CLIENT_ID
    private val authToken = "Bearer ${RetrofitInstance.TWITCH_ACCESS_TOKEN}"
    private val steam24hrPeakPlayersTypeId = 5
    private val igdbVisitsTypeId = 1

    // --- Moshi Instance for Age Ratings (De)serialization ---

    // This specific Moshi instance is used to convert the List<FormattedAgeRating>
    // into a JSON string for storage in the Room database (GameEntity.ageRatings)
    // and to parse it back when reading from the database.

    private val moshiForAgeRatings: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listOfFormattedAgeRatingAdapter = moshiForAgeRatings.adapter<List<FormattedAgeRating>>(
        Types.newParameterizedType(List::class.java, FormattedAgeRating::class.java)
    )

    // --- Local Database Operations (Room - GameDao) ---

    // Retrieves all games currently stored in the local library (database).
    // Games are ordered by name in ascending order.

    fun getLibraryGames(): Flow<List<GameEntity>> = gameDao.getAllGames()


    // Retrieves a single game entity from the local database by its unique ID.
    // IO dispatcher background execution.
    // Returns the GameEntity if found, or null otherwise.

    suspend fun getLibraryGameDetails(gameId: Long): GameEntity? = withContext(Dispatchers.IO) {
        gameDao.getGameById(id = gameId)
    }

    // Adds a game (represented by GameDetails domain model) to the local library.
    // 1. Download the game's cover image using ImageStorageManager.
    // 2. Convert the GameDetails domain model to a GameEntity.
    // 3. Inserting the GameEntity into the Room database.

    suspend fun addGameToLibrary(gameDetails: GameDetails) = withContext(Dispatchers.IO) {
        var localCoverPath: String? = null
        val sourceImageUrlForDownload = gameDetails.remoteCoverImageId?.let { imageId ->
            IgdbImageUtil.getImageUrl(imageId, IgdbImageUtil.ImageSize.HD)
        } ?: gameDetails.coverUrl // Fallback if remoteCoverImageId is not available.

        if (sourceImageUrlForDownload != null) {
            localCoverPath = imageStorageManager.saveImageFromUrl(
                imageUrl = sourceImageUrlForDownload,
                gameId = gameDetails.id
            )
        }

        val entity = this.run { gameDetails.toGameEntity(localCoverPathForEntity = localCoverPath) }
        gameDao.insertGame(game = entity) // Insert/replace the game in the database.
    }


    // Removes a game from the local library using its ID.
    // It also attempts to delete the locally stored cover image associated with the game.

    suspend fun removeGameFromLibrary(gameId: Long) = withContext(Dispatchers.IO) {
        val gameEntity = gameDao.getGameById(id = gameId) // Fetch entity to get localCoverPath.
        gameEntity?.localCoverPath?.let { filePath ->
            imageStorageManager.deleteImage(filePath = filePath) // Delete the image file.
        }
        gameDao.deleteGameById(id = gameId) // Delete the game record from the database.
    }

    // Checks if a game with the specified ID exists in the local library.
    // Returns true if the game exists (count > 0), false otherwise.

    suspend fun isGameInLibrary(gameId: Long): Boolean = withContext(Dispatchers.IO) {
        gameDao.isGameInLibrary(id = gameId) > 0
    }

    // --- Remote API Operations (IGDB - IgdbApiService) ---

    // Private helper function to fetch games based on a given IGDB popularity primitive type.
    // 1. Fetch a list of game IDs sorted by the specified popularity primitive (e.g., Steam CCU, IGDB Visits).
    // 2. Fetch the actual game data (GameDto) for these IDs.

    private suspend fun getGamesByPopularityPrimitive(
        popularityTypeId: Int,
        limit: Int = 20 // Default number of games to fetch for popularity lists.
    ): Result<List<GameDto>> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Fetch game IDs and their popularity values from `/popularity_primitives`.
            val popularityQueryString = "fields game_id, value;" +
                    " sort value desc;" +
                    " limit $limit;" +
                    " where popularity_type = $popularityTypeId;"
            val popularityRequestBody = popularityQueryString.toRequestBody("text/plain;charset=UTF-8".toMediaTypeOrNull())

            // Execute API call using RateLimiter to adhere to API rate limits.
            val popularityResponse = RateLimiter.execute {
                igdbApiService.getPopularityPrimitives(clientId, authToken, popularityRequestBody)
            }

            // Handle unsuccessful API response for popularity primitives.
            if (!popularityResponse.isSuccessful || popularityResponse.body() == null) {
                return@withContext Result.failure(Exception("API Error (PopularityPrimitives): ${popularityResponse.code()} ${popularityResponse.message()} Body: ${popularityResponse.errorBody()?.string()}"))
            }

            val popularGamePrimitives = popularityResponse.body()!!
            if (popularGamePrimitives.isEmpty()) {
                return@withContext Result.success(emptyList<GameDto>()) // No games found for the popularity metric.
            }

            val popularGameIds = popularGamePrimitives.map { it.gameId } // Extract game IDs.

            // Step 2: Fetch details for the obtained game IDs from `/games`.
            val gameIdsString = popularGameIds.joinToString(separator = ",", prefix = "(", postfix = ")")
            val gamesQueryString = "fields name, cover.image_id, aggregated_rating, first_release_date," +
                    "involved_companies.company.name, involved_companies.developer;" +
                    " where id = $gameIdsString & themes != (42);" +
                    " limit ${popularGameIds.size};"
            val gamesRequestBody = gamesQueryString.toRequestBody("text/plain;charset=UTF-8".toMediaTypeOrNull())

            val gamesResponse = RateLimiter.execute {
                igdbApiService.getGames(clientId, authToken, gamesRequestBody)
            }

            // Handle successful API response for game details.
            if (gamesResponse.isSuccessful && gamesResponse.body() != null) {
                // Re-sort the fetched GameDto list to match the original popularity order.
                val gamesMap = gamesResponse.body()!!.associateBy { it.id } // Create a map for quick lookup by ID.
                val sortedGames = popularGameIds.mapNotNull { gameId -> gamesMap[gameId] } // Reconstruct list in original order.
                Result.success(sortedGames)
            } else {
                // Handle unsuccessful API response for game details.
                Result.failure(Exception("API Error (Games for Popular): ${gamesResponse.code()} ${gamesResponse.message()} Body: ${gamesResponse.errorBody()?.string()}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Public function to fetch popular games sorted by "Steam 24hr Peak Players".

    suspend fun getPopularGamesByCCU(limit: Int = 15): Result<List<GameDto>> {
        return getGamesByPopularityPrimitive(steam24hrPeakPlayersTypeId, limit)
    }

    // Public function to fetch popular games sorted by "IGDB Visits".

    suspend fun getPopularGamesByIgdbVisits(limit: Int = 15): Result<List<GameDto>> {
        return getGamesByPopularityPrimitive(igdbVisitsTypeId, limit)
    }

    // Fetches upcoming games released on or after a specified `startDateTimestamp`.
    // Games are sorted by their release date in ascending order.

    suspend fun getUpcomingGames(
        startDateTimestamp: Long,
        limit: Int = 20
    ): Result<List<GameDto>> = withContext(Dispatchers.IO) {
        try {
            // Request fields necessary for displaying upcoming games.
            val queryString = "fields name, cover.image_id, first_release_date, aggregated_rating, involved_companies.company.name, involved_companies.developer;" +
                    " where first_release_date >= $startDateTimestamp & cover.image_id != null & themes != (42);" +
                    " sort first_release_date asc;" +
                    " limit $limit;"
            val requestBody = queryString.toRequestBody("text/plain;charset=UTF-8".toMediaTypeOrNull())

            val response = RateLimiter.execute { igdbApiService.getGames(clientId, authToken, requestBody) }
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API Error (Upcoming): ${response.code()} ${response.message()} Body: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Searches for games based on a text query and a set of `SearchFilters`.

    suspend fun searchGames(
        searchQuery: String,
        filters: SearchFilters,
        limit: Int = 15
    ): Result<List<GameDto>> = withContext(Dispatchers.IO) {
        try {
            val sanitizedQuery = searchQuery.replace("\"", "").replace(";", "")
            val whereClauses = mutableListOf("cover.image_id != null", "themes != (42)") // Default WHERE clauses.

            // Dynamically build additional WHERE clauses based on the provided filters.
            filters.minReleaseDate?.let { date ->
                val timestamp = date.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
                whereClauses.add("first_release_date >= $timestamp")
            }
            filters.maxReleaseDate?.let { date ->
                val timestamp = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1 // End of the day.
                whereClauses.add("first_release_date <= $timestamp")
            }
            filters.minRating?.let { rating ->
                whereClauses.add("aggregated_rating >= $rating")
            }
            filters.maxRating?.let { rating ->
                whereClauses.add("aggregated_rating <= $rating")
            }

            if (filters.selectedPegiRatings.isNotEmpty()) {
                // `selectedPegiRatings` contains IGDB's numerical rating IDs for PEGI (category 2).
                val pegiRatingsString = filters.selectedPegiRatings.joinToString(separator = ",", prefix = "(", postfix = ")")
                whereClauses.add("age_ratings.category = 2 & age_ratings.rating = $pegiRatingsString")
            }

            val whereString = whereClauses.joinToString(separator = " & ") // Combine all WHERE clauses.

            // Construct the full IGDB query string.
            val queryString = "search \"$sanitizedQuery\";" +
                    " fields name, cover.image_id, first_release_date, aggregated_rating, involved_companies.company.name, +" +
                    "involved_companies.developer, age_ratings.category, age_ratings.rating;" +
                    " where $whereString;" +
                    " limit $limit;"
            val requestBody = queryString.toRequestBody("text/plain;charset=UTF-8".toMediaTypeOrNull())

            val response = RateLimiter.execute { igdbApiService.getGames(clientId, authToken, requestBody) }
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API Error (Search): ${response.code()} ${response.message()} Query: $queryString Body: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetches comprehensive details for a single game identified by `gameId`.

    suspend fun getGameDetails(gameId: Long): Result<GameDto> = withContext(Dispatchers.IO) {
        try {
            // Request all necessary fields for the game details screen.
            val queryString = "fields name, url, cover.image_id, involved_companies.company.name, involved_companies.developer, " +
                    "aggregated_rating, first_release_date, summary, storyline, genres.name, screenshots.image_id," +
                    " age_ratings.category, age_ratings.rating, age_ratings.content_descriptions.description;" +
                    " where id = $gameId;"
            val requestBody = queryString.toRequestBody("text/plain;charset=UTF-8".toMediaTypeOrNull())
            val response = RateLimiter.execute { igdbApiService.getGames(clientId, authToken, requestBody) }
            if (response.isSuccessful && response.body()?.firstOrNull() != null) {
                Result.success(response.body()!!.first()) // Expecting a single game in the list.
            } else {
                Result.failure(Exception("API Error or game not found (Details for ID $gameId): ${response.code()} ${response.message()} Body: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // --- Mappers (DTO <-> Domain Model <-> Entity) ---

    // Extension function to convert a GameDto (from API) to a Game domain model.
    // Used for displaying games in lists (Explore, Soon, Search).

    fun GameDto.toDomainGame(): Game {
        // Extract developer names from involved companies.
        val developers = this.involvedCompanies
            ?.filter { it.developer == true }       // Filter for companies marked as developers.
            ?.mapNotNull { it.company?.name }       // Get their names.
            ?.joinToString(", ") ?: "N/A"  // Join multiple names

        return Game(
            id = this.id,
            name = this.name ?: "N/A",
            coverUrl = IgdbImageUtil.getImageUrl(this.cover?.imageId, IgdbImageUtil.ImageSize.COVER_BIG),
            communityRating = this.aggregatedRating,
            releaseDateTimestamp = this.firstReleaseDate,
            developerName = developers,
            userRating = null
        )
    }

    // Private helper function to map IGDB's numerical age rating ID to a displayable symbol string.
    // The `ratingId` parameter corresponds to the global enum `age_rating_rating_enum` in IGDB.

    private fun mapIgdbRatingToSymbol(ratingId: Int): String {
        return when (ratingId) {
            1 -> "3"; 2 -> "7"; 3 -> "12"; 4 -> "16"; 5 -> "18" // PEGI (Europe)
            6 -> "RP"; 7 -> "EC"; 8 -> "E"; 9 -> "E10+"; 10 -> "T"; 11 -> "M"; 12 -> "AO" // ESRB (US & Canada)
            13 -> "A"; 14 -> "B"; 15 -> "C"; 16 -> "D"; 17 -> "Z" // CERO (Japan)
            18 -> "0"; 19 -> "6"; 20 -> "12"; 21 -> "16"; 22 -> "18" // USK (Germany)
            23 -> "ALL"; 24 -> "12+"; 25 -> "15+"; 26 -> "18+"; 27 -> "TESTING" // GRAC (South Korea)
            28 -> "L"; 29 -> "10+"; 30 -> "12+"; 31 -> "14+"; 32 -> "16+"; 33 -> "18+" // CLASSIND (Brazil)
            34 -> "G"; 35 -> "PG"; 36 -> "M"; 37 -> "MA15+"; 38 -> "R18+"; 39 -> "RC" // ACB (Australia)
            else -> ratingId.toString()
        }
    }

    // Extension function to convert a GameDto (from API) to a GameDetails domain model.
    // Used for the game details screen.

    fun GameDto.toDomainGameDetails(
        localUserRating: Float? = null,
        isFavoriteInitially: Boolean = false
    ): GameDetails {
        // Format the first release date into a human-readable string.
        val humanDate = this.firstReleaseDate?.let { timestamp ->
            try {
                Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH))
            } catch (e: Exception) { "N/A" }
        } ?: "N/A"

        // Extract developer names.
        val developers = this.involvedCompanies
            ?.filter { it.developer == true }
            ?.mapNotNull { it.company?.name }
            ?.joinToString(", ") ?: "N/A"

        // Format age ratings into a list of FormattedAgeRating objects.
        val formattedAgeRatings = this.ageRatings?.mapNotNull { arDto ->
            val categoryId = arDto.category
            val ratingId = arDto.rating
            if (categoryId == null || ratingId == null) return@mapNotNull null // Skip if essential data is missing.

            // Map category ID to a displayable name.
            val categoryName = when (categoryId) {
                1 -> "ESRB (US & CA)"; 2 -> "PEGI (EU)"; 3 -> "CERO (JP)"; 4 -> "USK (DE)"
                5 -> "GRAC (KR)"; 6 -> "CLASSIND (BR)"; 7 -> "ACB (AU)"
                else -> "Unknown ($categoryId)"
            }

            // Map the numerical rating ID to its corresponding symbol.
            val ratingSymbol = mapIgdbRatingToSymbol(ratingId)

            // Extract content descriptions for this age rating.
            val descriptions = arDto.contentDescriptions
                ?.mapNotNull { it.description }
                ?: emptyList()

            FormattedAgeRating(categoryName, ratingSymbol, descriptions)
        } ?: emptyList() // Default to an empty list if no age ratings are available.

        return GameDetails(
            id = this.id,
            name = this.name ?: "N/A",
            igdbUrl = this.url, // URL to the game's page on IGDB.
            summary = this.summary ?: this.storyline ?: "No description available.",
            genres = this.genres?.mapNotNull { it.name } ?: emptyList(),
            releaseDateTimestamp = this.firstReleaseDate,
            releaseDateHuman = humanDate,
            screenshotsUrls = this.screenshots?.mapNotNull { IgdbImageUtil.getImageUrl(it.imageId, IgdbImageUtil.ImageSize.SCREENSHOT_HUGE) } ?: emptyList(),
            coverUrl = IgdbImageUtil.getImageUrl(this.cover?.imageId, IgdbImageUtil.ImageSize.HD),
            remoteCoverImageId = this.cover?.imageId,
            aggregatedRating = this.aggregatedRating,
            ageRatings = formattedAgeRatings,
            userRating = localUserRating,
            isFavorite = isFavoriteInitially,
            developerName = developers
        )
    }

    // Extension function to convert a GameEntity (from local DB) to a GameDetails domain model.
    // Used when displaying details for a game already in the user's library.

    fun GameEntity.toDomainGameDetails(): GameDetails {
        // Format the release date timestamp.
        val humanDate = this.releaseDateTimestamp?.let { timestamp ->
            try {
                Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.UTC).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH))
            } catch (e: Exception) {"N/A"}
        } ?: "N/A"

        // Deserialize the age ratings JSON string (stored in DB) back to a List<FormattedAgeRating>.
        val deserializedAgeRatings: List<FormattedAgeRating> = try {
            this.ageRatings?.let { jsonString -> listOfFormattedAgeRatingAdapter.fromJson(jsonString) } ?: emptyList()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error deserializing age ratings from DB for game ID ${this.id}", e)
            emptyList()
        }

        return GameDetails(
            id = this.id,
            name = this.name,
            igdbUrl = this.igdbUrl,
            summary = this.summary ?: "No description available.",
            genres = this.genres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
            releaseDateTimestamp = this.releaseDateTimestamp,
            releaseDateHuman = humanDate,
            screenshotsUrls = this.screenshotsUrls?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
            coverUrl = this.localCoverPath ?: this.coverUrl,
            remoteCoverImageId = null,
            aggregatedRating = this.aggregatedRating,
            ageRatings = deserializedAgeRatings,
            userRating = this.userRating,
            isFavorite = true,
            developerName = this.developerName ?: "N/A"
        )
    }

    // Private extension function to convert a GameDetails domain model to a GameEntity.
    // Used when saving a game to the local database (e.g., adding to library).

    private fun GameDetails.toGameEntity(localCoverPathForEntity: String?): GameEntity {
        // Serialize the List<FormattedAgeRating> to a JSON string for storage in the database.
        val ageRatingsJsonString = try {
            listOfFormattedAgeRatingAdapter.toJson(this.ageRatings)
        } catch (e: Exception) {
            Log.e("GameRepository", "Error serializing age ratings to JSON for game ID ${this.id}", e)
            null
        }

        return GameEntity(
            id = this.id,
            name = this.name,
            igdbUrl = this.igdbUrl,
            coverUrl = this.remoteCoverImageId?.let { IgdbImageUtil.getImageUrl(it, IgdbImageUtil.ImageSize.HD) } ?: this.coverUrl,
            localCoverPath = localCoverPathForEntity,
            summary = this.summary,
            genres = this.genres.joinToString(", "),
            releaseDateTimestamp = this.releaseDateTimestamp,
            screenshotsUrls = this.screenshotsUrls.joinToString(", "),
            aggregatedRating = this.aggregatedRating,
            ageRatings = ageRatingsJsonString,
            userRating = this.userRating,
            developerName = this.developerName
        )
    }
}