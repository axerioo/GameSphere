package com.axerioo.gamesphere.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

// --- Screen Sealed Class ---
// Defines the different screens in the application for Jetpack Compose Navigation.

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {

    data object Library : Screen("library", "Library", Icons.Filled.Bookmark)
    data object Explore : Screen("explore", "Explore", Icons.Filled.Explore)
    data object Soon : Screen("soon", "Soon", Icons.Filled.History)
    data object Search : Screen("search", "Search", Icons.Filled.Search)
    data object Details : Screen("details", "Details", Icons.Filled.Details)
}