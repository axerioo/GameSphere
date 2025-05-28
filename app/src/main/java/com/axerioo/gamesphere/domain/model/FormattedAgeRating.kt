package com.axerioo.gamesphere.domain.model

data class FormattedAgeRating(
    val categoryName: String,
    val ratingSymbol: String,
    val contentDescriptions: List<String> = emptyList()
)