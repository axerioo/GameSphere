package com.axerioo.gamesphere.ui.screens.explore

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.axerioo.gamesphere.ui.EmptyState
import com.axerioo.gamesphere.ui.ErrorMessage
import com.axerioo.gamesphere.ui.GameListItem
import com.axerioo.gamesphere.ui.LoadingIndicator
import com.axerioo.gamesphere.ui.Screen

// --- ExploreScreen Composable ---
// This screen allows users to explore popular games, categorized by different metrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavController, // NavController for navigating to other screens (e.g., game details).
    viewModel: ExploreViewModel = viewModel( // Obtain ExploreViewModel instance.
        factory = ExploreViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    // Observe the UI state from the ViewModel. Recomposition occurs when uiState changes.
    val uiState by viewModel.uiState.collectAsState()
    // Define the titles for the tabs.
    val tabTitles = listOf("Steam 24h CCU", "IGDB Visits")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore Top Games") },
                actions = {
                    // Refresh button to reload data for the currently active tab.
                    IconButton(onClick = { viewModel.refreshCurrentTabData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) { // `ordinal` gives the Int index of the enum.
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab.ordinal == index, // Highlight the currently selected tab.
                        onClick = {
                            // When a tab is clicked, get the corresponding ExploreTab enum entry.
                            val newTab = ExploreTab.entries[index]
                            viewModel.onTabSelected(newTab)
                        },
                        text = { Text(title) }
                    )
                }
            }

            // Box to hold the content displayed below the tabs (game list, loading, error, or empty state).
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // Show loading indicator if data is loading AND the games list is currently empty.
                    uiState.isLoading && uiState.games.isEmpty() -> LoadingIndicator()
                    // Show error message if an error occurred.
                    uiState.error != null -> ErrorMessage(
                        message = uiState.error!!,
                        onRetry = { viewModel.fetchGamesForCurrentTab() } // Allow retrying.
                    )
                    // Show empty state message if the games list is empty and not currently loading.
                    uiState.games.isEmpty() -> EmptyState(message = "No games found for this category. Try refreshing.")
                    // Display the list of games if data is available.
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.games, key = { it.id }) { game ->
                                GameListItem(
                                    game = game,
                                    onItemClick = { gameId ->
                                        // Navigate to the game details screen when an item is clicked.
                                        navController.navigate(Screen.Details.route + "/$gameId")
                                    }
                                )
                            }
                            // Show a loading indicator at the bottom of the list if more data is being loaded
                            if (uiState.isLoading && uiState.games.isNotEmpty()) {
                                item { LoadingIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}