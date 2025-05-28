package com.axerioo.gamesphere.ui.screens.details

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.axerioo.gamesphere.data.local.AppDatabase
import com.axerioo.gamesphere.data.remote.RetrofitInstance
import com.axerioo.gamesphere.data.repository.GameRepository
import com.axerioo.gamesphere.data.repository.ImageStorageManager
import com.axerioo.gamesphere.domain.model.GameDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- GameDetailsUiState Data Class ---
// Holds the game details, loading status, error messages, and other UI-related states.

data class GameDetailsUiState(
    val gameDetails: GameDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- GameDetailsViewModel Class ---
// Responsible for fetching game details, handling user interactions

class GameDetailsViewModel(
    private val gameRepository: GameRepository, // Repository to fetch and manage game data.
    private val gameId: Long                    // The ID of the game whose details are to be displayed.
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameDetailsUiState(isLoading = true))
    val uiState: StateFlow<GameDetailsUiState> = _uiState.asStateFlow()

    // --- Initialization ---
    // Load game details when the ViewModel is created.

    init {
        loadGameDetails()
    }

    // --- Load Game Details ---
    // Fetches game details either from the local database or from the remote API.

    fun loadGameDetails(forceRefresh: Boolean = false) {
        viewModelScope.launch { // Launch a coroutine in the ViewModel's scope.
            // Update UI state to indicate loading and clear any previous error.
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Try to get game details from the local database (user's library).
            val localGameEntity = gameRepository.getLibraryGameDetails(gameId)
            var isFavorite: Boolean  // Game is favorite if found locally.
            var userRatingFromDb = localGameEntity?.userRating // Get user's rating if game is local.

            // If game is found locally and not forcing a refresh, display local data.
            if (localGameEntity != null && !forceRefresh) {
                // Map GameEntity to GameDetails domain model using the repository's mapper.
                val details = gameRepository.run { localGameEntity.toDomainGameDetails() }
                _uiState.update { it.copy(gameDetails = details, isLoading = false, error = null) }
            } else {
                // If not found locally or forceRefresh is true, fetch from the remote API.
                gameRepository.getGameDetails(gameId)
                    .onSuccess { gameDto -> // Handle successful API response.
                        // Re-check favorite status and user rating from DB, as it might have changed
                        // or this is the first load from API for a potentially favorited game.
                        isFavorite = gameRepository.isGameInLibrary(gameId)
                        if (isFavorite && userRatingFromDb == null) { // If marked favorite but rating wasn't loaded initially.
                            userRatingFromDb = gameRepository.getLibraryGameDetails(gameId)?.userRating
                        }

                        // Map GameDto (from API) to GameDetails domain model.
                        val details = gameRepository.run {
                            gameDto.toDomainGameDetails(
                                localUserRating = userRatingFromDb,
                                isFavoriteInitially = isFavorite
                            )
                        }
                        _uiState.update { it.copy(gameDetails = details, isLoading = false, error = null) }

                        // If it was a force refresh and the game is in the library,
                        // update the local cache with the (potentially newer) details from the API.
                        if (forceRefresh && isFavorite) {
                            gameRepository.addGameToLibrary(details)
                        }
                    }
                    .onFailure { throwable -> // Handle API request failure.
                        // If API fails but we have local data,
                        // show the local data along with an error message indicating refresh failure.
                        if (localGameEntity != null) {
                            val details = gameRepository.run { localGameEntity.toDomainGameDetails() }
                            _uiState.update {
                                it.copy(
                                    gameDetails = details,
                                    isLoading = false,
                                    error = "Failed to refresh data: ${throwable.message}. Showing cached version."
                                )
                            }
                        } else {
                            // If no local data and API fails, show a full error.
                            _uiState.update { it.copy(error = throwable.message, isLoading = false, gameDetails = null) }
                        }
                    }
            }
        }
    }

    // --- Toggle Favorite Status ---
    // Adds or removes the game from the user's library (favorites).

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentDetailsValue = _uiState.value.gameDetails ?: return@launch // Exit if no details loaded.
            val wasFavorite = currentDetailsValue.isFavorite
            // Create an updated GameDetails object with the toggled favorite status.
            val updatedDetails = currentDetailsValue.copy(isFavorite = !wasFavorite)

            _uiState.update { it.copy(gameDetails = updatedDetails) }

            // Perform the actual repository operation.
            if (wasFavorite) {
                gameRepository.removeGameFromLibrary(updatedDetails.id)
            } else {
                gameRepository.addGameToLibrary(updatedDetails)
            }
        }
    }

    // --- Update User Rating ---
    // Updates the user's personal rating for the game.

    fun updateUserRating(rating: Float) {
        viewModelScope.launch {
            val currentDetailsValue = _uiState.value.gameDetails ?: return@launch

            // Create a GameDetails object with the new user rating.
            val updatedDetailsWithRating = currentDetailsValue.copy(userRating = rating)

            // If the game wasn't a favorite, marking it as favorite now since the user is rating it.
            val detailsToSave = if (!updatedDetailsWithRating.isFavorite) {
                updatedDetailsWithRating.copy(isFavorite = true)
            } else {
                updatedDetailsWithRating
            }

            // Save the updated game details (which includes the new rating and favorite status)
            // to the repository. The `addGameToLibrary` method handles both insert and update logic.
            gameRepository.addGameToLibrary(detailsToSave)

            // Update the UI state to reflect the saved details.
            _uiState.update { it.copy(gameDetails = detailsToSave) }
        }
    }

    // --- Refresh Data ---
    // Public function to explicitly trigger a refresh of game details from the API.

    fun refreshData() {
        loadGameDetails(forceRefresh = true)
    }

    // --- ViewModel Factory ---

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val application: Application, // Application context needed to get database instance.
        private val gameId: Long              // ID of the game for this ViewModel instance.
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameDetailsViewModel::class.java)) {
                val db = AppDatabase.getDatabase(application)
                val imageManager = ImageStorageManager(application)
                val repository = GameRepository(db.gameDao(), RetrofitInstance.api, imageManager)
                return GameDetailsViewModel(repository, gameId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}