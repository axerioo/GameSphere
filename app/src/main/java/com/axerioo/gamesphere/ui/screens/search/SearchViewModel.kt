package com.axerioo.gamesphere.ui.screens.search

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.axerioo.gamesphere.data.local.AppDatabase
import com.axerioo.gamesphere.data.remote.RetrofitInstance
import com.axerioo.gamesphere.data.repository.GameRepository
import com.axerioo.gamesphere.data.repository.ImageStorageManager
import com.axerioo.gamesphere.domain.model.Game
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// --- SearchUiState Data Class ---
// Represents the UI state for the Search screen.

data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val noResultsFound: Boolean = false,
    val showFilterSheet: Boolean = false,
    val activeFilters: SearchFilters = SearchFilters()
)

// --- SearchFilters Data Class ---
// Set of available filters that can be applied to a game search.

data class SearchFilters(
    val minReleaseDate: LocalDate? = null,
    val maxReleaseDate: LocalDate? = null,
    val minRating: Int? = null,
    val maxRating: Int? = null,
    val selectedPegiRatings: Set<Int> = emptySet()
) {
    // --- Is Any Filter Active ---
    // Helper function to check if any of the filter options have been set by the user.

    fun isAnyFilterActive(): Boolean {
        return minReleaseDate != null || maxReleaseDate != null ||
                minRating != null || maxRating != null ||
                selectedPegiRatings.isNotEmpty()
    }
}

// --- SearchViewModel Class ---
// Manages the search logic, including handling user input, applying filters,

class SearchViewModel(private val gameRepository: GameRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState()) // Initial empty state.
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null // Coroutine job for the current search operation.
    private val gamesLimit = 15        // Maximum number of search results to fetch per request.

    // --- On Search Query Changed ---
    // Called when the user types in the search input field.

    fun onSearchQueryChanged(query: String) {
        // Update the searchQuery in the UI state immediately.
        _uiState.update { it.copy(searchQuery = query, noResultsFound = false, error = null) }
        searchJob?.cancel()

        // Only proceed with search if the query is at least 3 characters long.
        if (query.length >= 3) {
            searchJob = viewModelScope.launch { // Launch a new coroutine for the search.
                delay(500) // Wait for 500ms before executing the search.
                performSearchInternal()
            }
        } else {
            // If the query is too short, clear search results and loading/error states.
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false, noResultsFound = false) }
        }
    }

    // --- Perform Search Internal ---
    // Private function containing the core logic for executing a search.

    private fun performSearchInternal() {
        val currentQuery = _uiState.value.searchQuery
        val currentFilters = _uiState.value.activeFilters

        // Search is only triggered if the text query is >= 3 characters.
        if (currentQuery.length < 3) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false, noResultsFound = false, error = null) }
            return
        }

        viewModelScope.launch {
            // Set loading state and clear previous error/noResults flags.
            _uiState.update { it.copy(isLoading = true, error = null, noResultsFound = false) }
            gameRepository.searchGames(
                searchQuery = currentQuery,
                filters = currentFilters, // Pass current filters to the repository.
                limit = gamesLimit
            )
                .onSuccess { dtoList ->
                    // Map DTOs to domain models.
                    val games = dtoList.map { gameDto ->
                        gameRepository.run { gameDto.toDomainGame() }
                    }
                    _uiState.update {
                        it.copy(
                            searchResults = games,
                            isLoading = false,
                            // Set `noResultsFound` to true if the list of games is empty after a valid search attempt.
                            noResultsFound = games.isEmpty()
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.message, isLoading = false, searchResults = emptyList()) }
                }
        }
    }

    // --- Retry Search ---
    // Public function to allow the UI (e.g., an error message's retry button) to re-trigger the search.

    fun retrySearch() {
        searchJob?.cancel()
        performSearchInternal()
    }

    // --- Show/Hide Filter Sheet ---
    // Updates the UI state to control the visibility of the filter modal bottom sheet.

    fun showFilterSheet(show: Boolean) {
        _uiState.update { it.copy(showFilterSheet = show) }
    }

    // --- Apply Filters ---
    // Called when the user applies filters from the filter sheet.

    fun applyFilters(newFilters: SearchFilters) {
        // Update active filters and hide the filter sheet.
        _uiState.update { it.copy(activeFilters = newFilters, showFilterSheet = false) }
        searchJob?.cancel()

        // Perform a new search if the current search query is valid (>= 3 characters).
        if (_uiState.value.searchQuery.length >= 3) {
            performSearchInternal()
        } else {
            // If query is too short, clear previous results.
            _uiState.update { it.copy(searchResults = emptyList(), noResultsFound = false) }
        }
    }

    // --- Clear Filters ---
    // Resets all active filters to their default (empty) state.

    fun clearFilters() {
        val hadActiveFilters = _uiState.value.activeFilters.isAnyFilterActive() // Check if filters were active before clearing.
        // Reset active filters and hide the filter sheet.
        _uiState.update { it.copy(activeFilters = SearchFilters(), showFilterSheet = false) }
        searchJob?.cancel()

        // If a valid search query exists, perform the search again (now without filters).
        if (_uiState.value.searchQuery.length >= 3) {
            performSearchInternal()
        } else {
            // If the query is too short, and filters were previously active, clear the search results.
            // This prevents showing old results that matched previous filters when query is short.
            if (hadActiveFilters) {
                _uiState.update { it.copy(searchResults = emptyList(), noResultsFound = false) }
            }
        }
    }

    // --- ViewModel Factory ---

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                val db = AppDatabase.getDatabase(application)
                val imageManager = ImageStorageManager(application)
                val repository = GameRepository(db.gameDao(), RetrofitInstance.api, imageManager)
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}