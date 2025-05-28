package com.axerioo.gamesphere.ui.screens.search

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// --- PEGI Rating Options ---
// A map defining display labels for PEGI ratings and their corresponding numerical IDs,

val pegiOptions = mapOf(
    "PEGI 3" to 1,
    "PEGI 7" to 2,
    "PEGI 12" to 3,
    "PEGI 16" to 4,
    "PEGI 18" to 5
)

// --- SearchScreen Composable ---
// This screen provides functionality for users to search for games and apply filters.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // --- Temporary States for Filters in BottomSheet ---
    // These states hold the filter values while the bottom sheet is open and being edited.

    var tempMinDate by remember { mutableStateOf(uiState.activeFilters.minReleaseDate) }
    var tempMaxDate by remember { mutableStateOf(uiState.activeFilters.maxReleaseDate) }
    var tempMinRating by remember { mutableStateOf(uiState.activeFilters.minRating?.toFloat()) }
    var tempMaxRating by remember { mutableStateOf(uiState.activeFilters.maxRating?.toFloat()) }
    var tempSelectedPegi by remember { mutableStateOf(uiState.activeFilters.selectedPegiRatings) }

    // --- Synchronize Temporary Filter States with ViewModel's Active Filters ---
    // This `LaunchedEffect` runs when `uiState.showFilterSheet` changes.
    // If the sheet is being shown, it copies the currently active filters from the ViewModel
    // into the temporary states, ensuring the filter sheet always opens with the latest applied values.

    LaunchedEffect(uiState.showFilterSheet) {
        if (uiState.showFilterSheet) {
            tempMinDate = uiState.activeFilters.minReleaseDate
            tempMaxDate = uiState.activeFilters.maxReleaseDate
            tempMinRating = uiState.activeFilters.minRating?.toFloat()
            tempMaxRating = uiState.activeFilters.maxRating?.toFloat()
            tempSelectedPegi = uiState.activeFilters.selectedPegiRatings
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Games") },
                actions = {
                    // Filter icon in the TopBar.
                    val filterIconColor =
                        if (uiState.activeFilters.isAnyFilterActive()) MaterialTheme.colorScheme.primary
                        else LocalContentColor.current // Default icon color.
                    IconButton(onClick = {
                        // When the filter icon is clicked, tell the ViewModel to show the filter sheet.
                        viewModel.showFilterSheet(true)
                    }) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filter Search",
                            tint = filterIconColor
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Enter game title...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
                trailingIcon = { // Display a clear button if there's text in the search field.
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true, // Restrict to a single line.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Box to display search results or state messages.
            Box(modifier = Modifier.weight(1f)) {
                when {
                    // Show loading indicator if data is currently being fetched.
                    uiState.isLoading -> LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                    // Show error message if an error occurred during fetching.
                    uiState.error != null -> ErrorMessage(
                        message = uiState.error!!,
                        onRetry = { viewModel.retrySearch() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    // Show empty state if no results were found for the current search query and filters.
                    uiState.noResultsFound -> EmptyState(
                        message = "No games found for \"${uiState.searchQuery}\" with current filters.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                    // Display the list of search results if available.
                    uiState.searchResults.isNotEmpty() -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.searchResults, key = { it.id }) { game ->
                                GameListItem(
                                    game = game,
                                    onItemClick = { gameId -> // Navigate to game details on item click.
                                        navController.navigate(Screen.Details.route + "/$gameId")
                                    }
                                )
                            }
                        }
                    }
                    // Display an initial message if the search query is too short or empty,
                    (uiState.searchQuery.isBlank() && !uiState.activeFilters.isAnyFilterActive()) ||
                            (uiState.searchQuery.length <= 2 && !uiState.activeFilters.isAnyFilterActive()) ->
                        EmptyState(
                            message = "The journey begins with a search.\nStart typing to find something great.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                }
            }
        }
    }

    // --- Modal Bottom Sheet for Filters ---
    // This sheet is displayed when `uiState.showFilterSheet` is true.

    if (uiState.showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showFilterSheet(false) },
            sheetState = sheetState
        ) {
            FilterSheetContent(
                // Pass the temporary filter states.
                initialFilters = SearchFilters(
                    minReleaseDate = tempMinDate,
                    maxReleaseDate = tempMaxDate,
                    minRating = tempMinRating?.toInt(),
                    maxRating = tempMaxRating?.toInt(),
                    selectedPegiRatings = tempSelectedPegi
                ),
                onApplyFilters = { appliedFilters ->
                    viewModel.applyFilters(appliedFilters)
                },
                onClearFilters = {
                    viewModel.clearFilters()
                },
                // Callbacks to update the temporary filter states in `SearchScreen` as user interacts.
                onMinDateChange = { tempMinDate = it },
                onMaxDateChange = { tempMaxDate = it },
                onMinRatingChange = { tempMinRating = it },
                onMaxRatingChange = { tempMaxRating = it },
                onPegiSelectionChange = { pegiId, isSelected -> // Handle PEGI chip selection changes.
                    tempSelectedPegi = if (isSelected) {
                        tempSelectedPegi + pegiId
                    } else {
                        tempSelectedPegi - pegiId // Remove from set.
                    }
                }
            )
        }
    }
}


// --- FilterSheetContent Composable ---
// This Composable defines the UI for the filter options within the ModalBottomSheet.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    initialFilters: SearchFilters,          // The current state of filters.
    onApplyFilters: (SearchFilters) -> Unit,// Callback for "Apply" button.
    onClearFilters: () -> Unit,             // Callback for "Clear All" button.

    // Callbacks to update the parent's temporary filter states as the user interacts with controls.
    onMinDateChange: (LocalDate?) -> Unit,
    onMaxDateChange: (LocalDate?) -> Unit,
    onMinRatingChange: (Float?) -> Unit,
    onMaxRatingChange: (Float?) -> Unit,
    onPegiSelectionChange: (Int, Boolean) -> Unit // Passes PEGI ID and its selected state.
) {
    // State for controlling the visibility of the DatePickerDialogs.
    var showMinDatePicker by remember { mutableStateOf(false) }
    var showMaxDatePicker by remember { mutableStateOf(false) }

    // State for the DatePickers, initialized with the current filter dates.
    val minDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialFilters.minReleaseDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )
    val maxDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialFilters.maxReleaseDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Make the content scrollable if it's too long.
    ) {
        Text( // Title for the filter sheet.
            "Filters",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- Release Date Range Filter Section ---

        Text("Release Date Range", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // Row for Min and Max date chips.
            FilterDateChip(
                text = "Min: ${initialFilters.minReleaseDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Any"}",
                modifier = Modifier.weight(1f),
                onClick = { showMinDatePicker = true }
            )
            FilterDateChip(
                text = "Max: ${initialFilters.maxReleaseDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Any"}",
                modifier = Modifier.weight(1f),
                onClick = { showMaxDatePicker = true }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- Community Rating Filter Section (0-100) ---

        Text("Community Rating (0-100)", style = MaterialTheme.typography.titleMedium)
        // Get current rating values from `initialFilters`.
        val currentMinRating = initialFilters.minRating?.toFloat() ?: 0f
        val currentMaxRating = initialFilters.maxRating?.toFloat() ?: 100f

        Text("Min: ${currentMinRating.toInt()}")
        Slider(
            value = currentMinRating,
            onValueChange = { newValue ->
                // Update min rating, ensuring it doesn't exceed the current max rating.
                onMinRatingChange(newValue.coerceAtMost(currentMaxRating))
            },
            valueRange = 0f..100f, // Slider for 0-100 range.
            steps = 99
        )
        Text("Max: ${currentMaxRating.toInt()}")
        Slider(
            value = currentMaxRating,
            onValueChange = { newValue ->
                // Update max rating, ensuring it's not less than the current min rating.
                onMaxRatingChange(newValue.coerceAtLeast(currentMinRating))
            },
            valueRange = 0f..100f,
            steps = 99
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- PEGI Rating Filter Section ---

        Text("PEGI Rating", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row( // Horizontally scrollable row for PEGI FilterChips.
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pegiOptions.forEach { (label, pegiId) ->
                FilterChip(
                    selected = initialFilters.selectedPegiRatings.contains(pegiId), // Check if this PEGI ID is selected.
                    onClick = {
                        // Toggle the selection state for this PEGI ID.
                        onPegiSelectionChange(pegiId, !initialFilters.selectedPegiRatings.contains(pegiId))
                    },
                    label = { Text(label) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- Action Buttons (Clear All, Apply) ---

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End, // Align buttons to the end (right side).
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClearFilters) { // Button to clear all applied filters.
                Text("Clear All")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { // Button to apply the currently configured filters.
                onApplyFilters(initialFilters)
            }) {
                Text("Apply")
            }
        }
    }

    // --- DatePickerDialogs for Min and Max Release Date ---

    if (showMinDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showMinDatePicker = false }, // Hide dialog on dismiss.
            confirmButton = {
                TextButton(onClick = {
                    // When "OK" is clicked, get the selected date (in UTC millis) from DatePicker.
                    minDatePickerState.selectedDateMillis?.let { millis ->
                        // Convert millis to LocalDate and update the min date filter.
                        onMinDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showMinDatePicker = false // Hide dialog.
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showMinDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = minDatePickerState) }
    }

    if (showMaxDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showMaxDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    maxDatePickerState.selectedDateMillis?.let { millis ->
                        onMaxDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showMaxDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showMaxDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = maxDatePickerState) }
    }
}

// --- FilterDateChip Composable ---
// A custom Composable that looks like a chip and is used to trigger a DatePickerDialog.

@Composable
fun FilterDateChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon( // Date range icon.
            Icons.Filled.DateRange,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text)
    }
}