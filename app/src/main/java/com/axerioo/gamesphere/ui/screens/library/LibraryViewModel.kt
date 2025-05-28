package com.axerioo.gamesphere.ui.screens.library

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.axerioo.gamesphere.data.local.AppDatabase
import com.axerioo.gamesphere.data.local.GameEntity
import com.axerioo.gamesphere.data.remote.RetrofitInstance
import com.axerioo.gamesphere.data.repository.GameRepository
import com.axerioo.gamesphere.data.repository.ImageStorageManager
import com.axerioo.gamesphere.domain.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- LibraryUiState Data Class ---
// Holds the list of games in the library, loading status, and any error messages.

data class LibraryUiState(
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- LibraryViewModel Class ---
// ViewModel for the Library screen.

class LibraryViewModel(private val gameRepository: GameRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true)) // Initial state: loading.
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    // --- Initialization ---
    // Load the library games when the ViewModel is created.

    init {
        loadLibraryGames()
    }

    // --- Load Library Games ---
    // Fetches the list of games from the local database via the GameRepository.

    private fun loadLibraryGames() {
        viewModelScope.launch {
            // Set loading state to true and clear any previous errors.
            _uiState.update { it.copy(isLoading = true, error = null) }
            gameRepository.getLibraryGames() // Fetch games.
                .map { entities -> // Transform the Flow of GameEntity list to a Flow of Game domain model list.
                    entities.map { entity ->
                        entity.toDomainGame()
                    }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { games -> // Collect the emitted list of Game domain models.
                    _uiState.update { it.copy(games = games, isLoading = false, error = null) }
                }
        }
    }

    // --- Mapper: GameEntity to Game Domain Model ---
    // Private extension function to map a GameEntity (from Room database)
    // to a Game domain model (used by the UI).

    private fun GameEntity.toDomainGame(): Game {
        return Game(
            id = this.id,
            name = this.name,
            coverUrl = this.localCoverPath ?: this.coverUrl,
            communityRating = this.aggregatedRating,
            releaseDateTimestamp = this.releaseDateTimestamp,
            developerName = this.developerName,
            userRating = this.userRating
        )
    }

    // --- Refresh Library ---
    // Public function to allow the UI to trigger a refresh of the library data.

    fun refreshLibrary() {
        _uiState.update { it.copy(isLoading = true) }
        loadLibraryGames() // Re-fetch library games.
    }

    // --- Remove Game From Library ---
    // Removes a game from the library (local database) using its ID.

    fun removeGameFromLibrary(gameId: Long) {
        viewModelScope.launch {
            gameRepository.removeGameFromLibrary(gameId)
        }
    }

    // --- ViewModel Factory ---

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                val db = AppDatabase.getDatabase(application)
                val imageManager = ImageStorageManager(application)
                val repository = GameRepository(db.gameDao(), RetrofitInstance.api, imageManager)
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}