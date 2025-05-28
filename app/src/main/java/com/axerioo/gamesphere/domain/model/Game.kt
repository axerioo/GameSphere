package com.axerioo.gamesphere.domain.model

data class Game(
    val id: Long,
    val name: String,
    val coverUrl: String?,
    val communityRating: Double?,
    val releaseDateTimestamp: Long? = null,
    val developerName: String?,
    val userRating: Float?
)