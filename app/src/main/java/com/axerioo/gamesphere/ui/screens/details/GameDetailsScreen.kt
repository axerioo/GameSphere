package com.axerioo.gamesphere.ui.screens.details

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.axerioo.gamesphere.domain.model.FormattedAgeRating
import com.axerioo.gamesphere.domain.model.GameDetails
import com.axerioo.gamesphere.ui.ErrorMessage
import com.axerioo.gamesphere.ui.LoadingIndicator
import kotlin.math.roundToInt

// --- GameDetailsScreen Composable ---
// Main Composable function for the game details screen.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(
    navController: NavController,    // NavController for handling navigation actions (e.g., back).
    gameId: Long,                    // The ID of the game to display details.
    viewModel: GameDetailsViewModel = viewModel(
        factory = GameDetailsViewModel.Factory(
            LocalContext.current.applicationContext as Application, // Application context for ViewModel factory.
            gameId // gameId for ViewModel factory.
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // IGDB Page Link Button
                    uiState.gameDetails?.igdbUrl?.let { url ->
                        IconButton(onClick = {
                            try {
                                uriHandler.openUri(url) // Attempt to open the URL.
                            } catch (e: Exception) {
                                // Show a Toast message if opening the URL fails.
                                Toast.makeText(
                                    context,
                                    "Failed to open IGDB page: ${e.localizedMessage}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Icon(Icons.Filled.Language, contentDescription = "Open IGDB Page")
                        }
                    }
                    // Favorite/Unfavorite Button
                    uiState.gameDetails?.let { gameDetails ->
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (gameDetails.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (gameDetails.isFavorite) "Remove from Library" else "Add to Library",
                                tint = if (gameDetails.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                    // Refresh Data Button
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Data")
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Conditional rendering based on UI state.
            when {
                uiState.isLoading && uiState.gameDetails == null -> LoadingIndicator()
                uiState.error != null && uiState.gameDetails == null -> ErrorMessage(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadGameDetails() } // Allow retrying the data load.
                )
                // Display game details if available.
                uiState.gameDetails != null -> {
                    GameDetailsContent(
                        details = uiState.gameDetails!!,
                        onRatingChanged = { newRating -> viewModel.updateUserRating(newRating) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Show a small loading indicator at the top if a refresh is in progress
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        )
                    }

                    // Show a non-blocking error message at the bottom
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(8.dp)
                        )
                    }
                }
                else -> {
                    Text(
                        "Game not found or error loading.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

// --- GameDetailsContent Composable ---
// This Composable is responsible for laying out all the detailed information of a game.

@Composable
fun GameDetailsContent(
    details: GameDetails,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section: Cover Image
        AsyncImage(
            model = details.coverUrl,
            contentDescription = "${details.name} Cover",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Section: Title
        Text(
            text = details.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Section: Developer Name
        details.developerName?.takeIf { it.isNotBlank() && it != "N/A" }?.let { devName ->
            Text(
                text = devName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: User Rating
        UserRatingSection(
            currentUserRating = details.userRating,
            onRatingChanged = onRatingChanged
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Section: Critic Rating (IGDB Rating)
        Text(
            text = "Critic Rating",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = details.aggregatedRating?.let { "%.1f / 10".format(it / 10.0) } ?: "N/A",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Section: Release Date
        Text(
            text = "Released",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = details.releaseDateHuman ?: "N/A",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Section: Summary
        Text(
            text = "Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = details.summary ?: "No summary available.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Section: Genres
        if (details.genres.isNotEmpty()) {
            Text(
                text = "Genres",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // LazyRow for a horizontally scrollable list of genre chips.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(details.genres) { genre ->
                    ChipView(text = genre)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Screenshots
        if (details.screenshotsUrls.isNotEmpty()) {
            Text(
                text = "Screenshots",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Swipeable image gallery.
            val pagerState = rememberPagerState(pageCount = { details.screenshotsUrls.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) { pageIndex ->
                AsyncImage(
                    model = details.screenshotsUrls[pageIndex],
                    contentDescription = "Screenshot ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Fit
                )
            }
            // Display pager indicator dots if there's more than one screenshot.
            if (pagerState.pageCount > 1) {
                Row(
                    Modifier
                        .height(20.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Age Ratings
        if (details.ageRatings.isNotEmpty()) {
            Text(
                text = "Age Ratings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Iterate through the list of formatted age ratings and display each one.
            details.ageRatings.forEach { ageRating ->
                AgeRatingCard(ageRating = ageRating)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- AgeRatingCard Composable ---
// Displays a single age rating in a card-like format.
// Includes the rating symbol, category name, and an expandable section for content descriptors.

@Composable
fun AgeRatingCard(ageRating: FormattedAgeRating) {
    val hasDescriptions = ageRating.contentDescriptions.isNotEmpty()
    // State to manage whether the content descriptors section is expanded.
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then( // Apply clickable modifier only if there are descriptions to expand.
                if (hasDescriptions) Modifier.clickable { expanded = !expanded }
                else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RectangleShape,
    ) {
        Column {
            // Row for rating symbol, category name, and expand icon.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Box for the rating symbol (e.g., "18", "M").
                Box(
                    modifier = Modifier
                        .widthIn(min = 56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(2.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ageRating.ratingSymbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text for the rating category name (e.g., "PEGI", "ESRB").
                Text(
                    text = ageRating.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // Display expand/collapse icon if there are content descriptors.
                if (hasDescriptions) {
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "arrowRotation"
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .rotate(rotationAngle)
                            .size(24.dp)
                    )
                }
            }

            // AnimatedVisibility for the expandable content descriptors section.
            if (hasDescriptions) {
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(start = 68.dp)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ageRating.contentDescriptions.forEach { descriptor ->
                                ChipView(text = descriptor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- UserRatingSection Composable ---
// Allows the user to view and set their personal rating for the game using a Slider.

@Composable
fun UserRatingSection(
    currentUserRating: Float?,
    onRatingChanged: (Float) -> Unit
) {
    // Internal state for the Slider, initialized with the current user rating or 0.
    var internalRating by remember(currentUserRating) { mutableFloatStateOf(currentUserRating ?: 0f) }

    Column {
        Text(
            text = "Your Rating",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Slider(
            value = internalRating,
            onValueChange = { newValue ->
                internalRating = newValue // Update internal state as slider moves.
                onRatingChanged(newValue.roundToInt().toFloat()) // Round to whole number for 0-10 scale.
            },
            valueRange = 0f..10f,
            steps = 9
        )

        // Display min, current, and max values for the slider.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0")
            Text(
                text = "%.0f".format(internalRating),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("10")
        }
    }
}

// --- ChipView Composable ---
// A simple Composable to display text within a chip-like Surface.

@Composable
fun ChipView(text: String) {
    Surface(
        modifier = Modifier.padding(end = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GameDetailsContentPreview() {
    val sampleDetails = GameDetails(
        id = 12345,
        name = "Epic Adventure Quest Saga: The Dragon's Whisper",
        coverUrl = null,
        developerName = "Awesome Game Studios Inc.",
        userRating = 8.5f,
        aggregatedRating = 92.5,
        releaseDateHuman = "Oct 26, 2023",
        summary = "Embark on a thrilling journey through mystical lands to uncover ancient secrets and battle fearsome beasts. This is a very long summary to test text wrapping and scrolling capabilities within the details screen content.",
        genres = listOf("Action RPG", "Open World", "Fantasy", "Adventure", "Story-Rich"),
        screenshotsUrls = listOf("url1", "url2", "url3"),
        ageRatings = listOf(
            FormattedAgeRating(
                categoryName = "PEGI (EU)",
                ratingSymbol = "18",
                contentDescriptions = listOf("Extreme Violence", "Strong Language", "Online Gameplay")
            ),
            FormattedAgeRating(
                categoryName = "ESRB (US & CA)",
                ratingSymbol = "M",
                contentDescriptions = listOf("Blood and Gore", "Intense Violence", "Suggestive Themes")
            )
        ),
        igdbUrl = "https://www.igdb.com/games/sample-game",
        isFavorite = true,
        releaseDateTimestamp = 1704067200L,
        remoteCoverImageId = "sample-cover-id"
    )

    GameDetailsContent(
        details = sampleDetails,
        onRatingChanged = {}
    )
}