package com.axerioo.gamesphere.ui.screens.soon

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
import java.time.LocalDate
import java.time.ZoneOffset

// --- SoonUiState Data Class ---
// Represents the UI state for the "Coming Soon" screen.

data class SoonUiState(
    val upcomingGames: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val showDatePickerDialog: Boolean = false
)

// --- SoonViewModel Class ---
// ViewModel for the "Coming Soon" screen.

class SoonViewModel(private val gameRepository: GameRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SoonUiState(isLoading = true)) // Initial state: loading.
    val uiState: StateFlow<SoonUiState> = _uiState.asStateFlow()

    private val gamesLimit = 15 // Define the maximum number of games to fetch.

    // --- Initialization ---
    // Fetch upcoming games for the default selected date (today) when the ViewModel is created.
    init {
        fetchUpcomingGames()
    }

    // --- On Date Selected ---
    // Called when the user selects a new date from the DatePickerDialog.

    fun onDateSelected(newDateMillis: Long?) {
        newDateMillis?.let { millis -> // Proceed only if a date was selected (not null).
            // Convert the UTC milliseconds from DatePicker to a LocalDate object.
            val newLocalDate = LocalDate.ofEpochDay(millis / (1000 * 60 * 60 * 24))

            // Update state and fetch games only if the date has actually changed and not currently loading.
            if (newLocalDate != _uiState.value.selectedDate && !_uiState.value.isLoading) {
                _uiState.update {
                    it.copy(
                        selectedDate = newLocalDate,
                        upcomingGames = emptyList(),
                        showDatePickerDialog = false
                    )
                }
                fetchUpcomingGames() // Fetch games for the new date.
            } else {
                // If date hasn't changed or still loading, just ensure the dialog is hidden.
                _uiState.update { it.copy(showDatePickerDialog = false) }
            }
        } ?: run {
            // If newDateMillis is null (user canceled the dialog), just hide the dialog.
            _uiState.update { it.copy(showDatePickerDialog = false) }
        }
    }

    // --- Show/Hide DatePicker ---
    // Controls the visibility of the DatePickerDialog from the UI.

    fun showDatePicker(show: Boolean) {
        _uiState.update { it.copy(showDatePickerDialog = show) }
    }

    // --- Fetch Upcoming Games ---
    // Fetches the list of upcoming games from the repository based on the `selectedDate` in the UI state.

    fun fetchUpcomingGames() {
        viewModelScope.launch { // Launch a coroutine within the ViewModel's scope.
            // Set loading state to true and clear any previous errors.
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Convert the selected LocalDate to a Unix timestamp.
            val startOfDayTimestamp = _uiState.value.selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toEpochSecond()

            // Call the repository to get upcoming games.
            gameRepository.getUpcomingGames(
                startDateTimestamp = startOfDayTimestamp,
                limit = gamesLimit
            )
                .onSuccess { dtoList ->
                    // Map the list of GameDto objects to a list of Game domain models.
                    val games = dtoList.map { gameDto ->
                        gameRepository.run { gameDto.toDomainGame() }
                    }
                    // Update the UI state with the fetched games and set loading to false.
                    _uiState.update { it.copy(upcomingGames = games, isLoading = false) }
                }
                .onFailure { throwable ->
                    // Update the UI state with the error message and set loading to false.
                    _uiState.update { it.copy(error = throwable.message, isLoading = false) }
                }
        }
    }

    // --- Refresh Current Date Data ---
    // Public function to allow the UI to trigger a data refresh for the currently selected date.

    fun refreshCurrentDateData() {
        fetchUpcomingGames()
    }

    // --- ViewModel Factory ---

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SoonViewModel::class.java)) {
                val db = AppDatabase.getDatabase(application)
                val imageManager = ImageStorageManager(application)
                val repository = GameRepository(db.gameDao(), RetrofitInstance.api, imageManager)
                @Suppress("UNCHECKED_CAST")
                return SoonViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}