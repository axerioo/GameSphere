package com.axerioo.gamesphere.ui.screens.explore

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.axerioo.gamesphere.data.local.AppDatabase
import com.axerioo.gamesphere.data.remote.RetrofitInstance
import com.axerioo.gamesphere.data.repository.GameRepository
import com.axerioo.gamesphere.data.repository.ImageStorageManager
import com.axerioo.gamesphere.domain.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- ExploreTab Enum ---
// Defines the different categories or tabs available on the Explore screen.

enum class ExploreTab {
    CCU_24,        // Represents sorting by "Steam 24hr Peak Concurrent Users".
    IGDB_VISITS    // Represents sorting by "IGDB Page Visits".
}

// --- ExploreUiState Data Class ---
// Represents the UI state for the Explore screen.

data class ExploreUiState(
    val selectedTab: ExploreTab = ExploreTab.CCU_24, // The currently active tab
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- ExploreViewModel Class ---

class ExploreViewModel(private val gameRepository: GameRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true)) // Initial state: loading.
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    // --- Initialization ---
    // Fetch data for the default selected tab when the ViewModel is created.

    init {
        fetchGamesForCurrentTab()
    }

    // --- On Tab Selected ---
    // Updates the selected tab in the UI state and triggers fetching data for the new tab.

    fun onTabSelected(tab: ExploreTab) {
        // If the selected tab is already the current one, data is loaded, and not currently loading, do nothing.
        if (_uiState.value.selectedTab == tab && _uiState.value.games.isNotEmpty() && !_uiState.value.isLoading) {
            return
        }
        // Set the new selected tab and clear the current list of games.
        _uiState.update { it.copy(selectedTab = tab, games = emptyList()) }
        fetchGamesForCurrentTab()
    }

    // --- Fetch Games for Current Tab ---
    // Fetches the list of games based on the currently selected tab in the UI state.

    fun fetchGamesForCurrentTab() {
        viewModelScope.launch { // Launch a coroutine within the ViewModel's scope.
            _uiState.update { it.copy(isLoading = true, error = null) }
            val gamesLimit = 15

            // Determine which repository method to call based on the selected tab.
            val result = when (_uiState.value.selectedTab) {
                ExploreTab.CCU_24 -> gameRepository.getPopularGamesByCCU(gamesLimit)
                ExploreTab.IGDB_VISITS -> gameRepository.getPopularGamesByIgdbVisits(gamesLimit)
            }

            result
                .onSuccess { dtoList ->
                    // Map the list of GameDto objects (from API) to a list of Game domain models.
                    val mappedGames = dtoList.map { gameDto ->
                        gameRepository.run { gameDto.toDomainGame() } // Use extension function via repository scope.
                    }
                    _uiState.update { it.copy(games = mappedGames, isLoading = false) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.message, isLoading = false) }
                }
        }
    }

    // --- Refresh Current Tab Data ---
    // Public function to allow the UI to trigger a data refresh for the currently active tab.

    fun refreshCurrentTabData() {
        fetchGamesForCurrentTab()
    }

    // --- ViewModel Factory ---

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
                val db = AppDatabase.getDatabase(application)
                val imageManager = ImageStorageManager(application)
                val repository = GameRepository(db.gameDao(), RetrofitInstance.api, imageManager)
                @Suppress("UNCHECKED_CAST")
                return ExploreViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}