package com.axerioo.gamesphere.ui.screens.soon

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

// --- SoonScreen Composable ---
// This screen displays games with upcoming release dates.
// Users can select a start date, and the list will show games releasing on or after that date.

@OptIn(ExperimentalMaterial3Api::class) // Opt-in for Material 3 experimental APIs.
@Composable
fun SoonScreen(
    navController: NavController,
    viewModel: SoonViewModel = viewModel(
        factory = SoonViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    // Observe UI state from the ViewModel.
    val uiState by viewModel.uiState.collectAsState()

    // --- DatePicker State ---
    // `rememberDatePickerState` is used to manage the state of the DatePicker component.
    // `initialSelectedDateMillis`: Sets the initially selected date in the picker.
    //   It's derived from `uiState.selectedDate` and converted to UTC milliseconds,
    //   as DatePickerState operates with UTC timestamps.
    // `selectableDates`: An object implementing `SelectableDates` to control which dates can be selected.
    //   Here, it's configured to allow selection only from today onwards.

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.selectedDate
            .atStartOfDay(ZoneOffset.UTC) // Convert LocalDate to the start of the day in UTC.
            .toInstant()
            .toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Get the start of today in UTC milliseconds.
                val todayStartOfDayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                return utcTimeMillis >= todayStartOfDayMillis
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coming Soon") }, // Screen title.
                actions = {
                    // Refresh button to reload data for the currently selected date.
                    IconButton(onClick = { viewModel.refreshCurrentDateData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues) // Apply padding from Scaffold.
                .fillMaxSize()
        ) {
            // --- Date Selection Section ---
            // This Row displays the currently selected start date and is clickable to open the DatePickerDialog.

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showDatePicker(true) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    // Display the selected date, formatted
                    text = "Date: ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(Icons.Filled.CalendarToday, contentDescription = "Select Date")
            }

            HorizontalDivider()

            // --- Material 3 DatePickerDialog ---
            // Displayed when `uiState.showDatePickerDialog` is true (controlled by ViewModel).
            if (uiState.showDatePickerDialog) {
                DatePickerDialog(
                    onDismissRequest = { viewModel.showDatePicker(false) }, // Hide dialog on dismiss.
                    confirmButton = {
                        TextButton(onClick = {
                            // When "OK" is clicked, pass the selected date (in UTC millis) to the ViewModel.
                            viewModel.onDateSelected(datePickerState.selectedDateMillis)
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showDatePicker(false) }) { // Hide dialog on cancel.
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(
                        state = datePickerState, // Pass the managed state to the DatePicker.
                        title = { Text(text = "Select date", modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
                        headline = {
                            // Display the currently selected date in the picker's headline.
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                                Text(
                                    text = selectedLocalDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom=12.dp)
                                )
                            }
                        }
                    )
                }
            }

            // --- Game List Section ---
            // Box to display the list of upcoming games or state messages (loading, error, empty).

            Box(modifier = Modifier.weight(1f)) { // Takes remaining vertical space.
                when {
                    // Show loading indicator if data is loading AND the games list is currently empty.
                    uiState.isLoading && uiState.upcomingGames.isEmpty() -> LoadingIndicator()
                    // Show error message if an error occurred.
                    uiState.error != null -> ErrorMessage(
                        message = uiState.error!!,
                        onRetry = { viewModel.fetchUpcomingGames() } // Allow retrying.
                    )
                    // Show empty state message if the games list is empty and not currently loading.
                    uiState.upcomingGames.isEmpty() -> EmptyState(message = "No upcoming games found from this date. Try an earlier date or refresh.")
                    // Display the list of games if data is available.
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.upcomingGames, key = { it.id }) { game ->
                                GameListItem(
                                    game = game,
                                    onItemClick = { gameId ->
                                        // Navigate to game details screen on item click.
                                        navController.navigate(Screen.Details.route + "/$gameId")
                                    }
                                )
                            }
                            // Show a loading indicator at the bottom if a refresh is in progress
                            if (uiState.isLoading && uiState.upcomingGames.isNotEmpty()) {
                                item { LoadingIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}