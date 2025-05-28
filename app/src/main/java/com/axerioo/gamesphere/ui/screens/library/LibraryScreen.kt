package com.axerioo.gamesphere.ui.screens.library

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.axerioo.gamesphere.domain.model.Game
import com.axerioo.gamesphere.ui.EmptyState
import com.axerioo.gamesphere.ui.ErrorMessage
import com.axerioo.gamesphere.ui.LibraryGameListItem
import com.axerioo.gamesphere.ui.LoadingIndicator
import com.axerioo.gamesphere.ui.Screen

// --- LibraryScreen Composable ---
// This screen displays the user's collection of saved/favorited games.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    // Observe the UI state from the ViewModel.
    val uiState by viewModel.uiState.collectAsState()
    // State to control the visibility of the delete confirmation dialog.
    var showDeleteDialog by remember { mutableStateOf(false) }
    // State to hold the game object that is about to be deleted.
    var gameToDelete by remember { mutableStateOf<Game?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                actions = {
                    // Refresh button to reload the library data.
                    IconButton(onClick = { viewModel.refreshLibrary() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Library")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                // Show loading indicator if data is loading (typically on initial load or refresh).
                uiState.isLoading -> LoadingIndicator()
                // Show error message if an error occurred.
                uiState.error != null -> ErrorMessage(
                    message = uiState.error!!,
                    onRetry = { viewModel.refreshLibrary() } // Allow retrying.
                )
                // Show empty state message if the library is empty and not currently loading.
                uiState.games.isEmpty() -> EmptyState(message = "Your library is empty.")
                // Display the list of games if data is available.
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) { // Efficiently display a scrollable list.
                        items(uiState.games, key = { it.id }) { game -> // `key` helps Compose optimize.
                            LibraryGameListItem( // Use the specialized Composable for library items.
                                game = game,
                                onItemClick = { gameId ->
                                    // Navigate to game details screen on item click.
                                    navController.navigate(Screen.Details.route + "/$gameId")
                                },
                                onItemLongClick = { selectedGame ->
                                    // On long click, set the game to be deleted and show the confirmation dialog.
                                    gameToDelete = selectedGame
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Delete Confirmation Dialog ---
    // Display the AlertDialog if `showDeleteDialog` is true and a `gameToDelete` is set.

    if (showDeleteDialog && gameToDelete != null) {
        AlertDialog(
            onDismissRequest = { // Called when the dialog is dismissed (e.g., by tapping outside or back button).
                showDeleteDialog = false
                gameToDelete = null // Clear the game to be deleted.
            },
            title = { Text("Delete game?") }, // Title of the dialog.
            text = { Text("${gameToDelete?.name} will be removed from your library.") }, // Confirmation message.
            confirmButton = {
                Button(
                    onClick = {
                        // If confirmed, remove the game from the library via the ViewModel.
                        gameToDelete?.let { viewModel.removeGameFromLibrary(it.id) }
                        showDeleteDialog = false
                        gameToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        gameToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}