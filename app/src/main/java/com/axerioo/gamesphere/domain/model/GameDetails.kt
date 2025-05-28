package com.axerioo.gamesphere.domain.model

data class GameDetails(
    val id: Long,
    val name: String,
    val summary: String?,
    val genres: List<String>,
    val releaseDateTimestamp: Long?,
    val releaseDateHuman: String?,
    val screenshotsUrls: List<String>,
    val coverUrl: String?,
    val remoteCoverImageId: String?,
    val aggregatedRating: Double?,
    val ageRatings: List<FormattedAgeRating>,
    var userRating: Float?,
    var isFavorite: Boolean,
    val developerName: String?,
    val igdbUrl: String?
)