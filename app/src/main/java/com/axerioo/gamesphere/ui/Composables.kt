package com.axerioo.gamesphere.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.axerioo.gamesphere.domain.model.Game
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


// --- GameListItem Composable ---
// A general-purpose Composable for displaying a game item in a list for screens like Explore, Soon, Search results.

@Composable
fun GameListItem(
    game: Game,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onItemClick(game.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Game Cover Image
            AsyncImage(
                model = game.coverUrl,
                contentDescription = "${game.name} cover",
                modifier = Modifier
                    .size(width = 80.dp, height = 120.dp)
                    .clip(MaterialTheme.shapes.medium), // Rounded corners.
                contentScale = ContentScale.Crop // Crop the image to fill the bounds.
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Game Info Column (Name, Developer, Release Date)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2, // Limit name to two lines.
                    overflow = TextOverflow.Ellipsis // Add ellipsis if name is too long.
                )

                game.developerName?.let { devName ->
                    if (devName != "N/A") {
                        Text(
                            text = devName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Display release date if available.
                game.releaseDateTimestamp?.let { timestamp ->
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    formatter.timeZone = TimeZone.getTimeZone("UTC") // IGDB dates are typically in UTC.
                    Text(
                        text = "Release: ${formatter.format(Date(timestamp * 1000L))}", // Unix timestamp.
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Community Rating Column (Aligned to the right)
            Column(
                horizontalAlignment = Alignment.End, // Align text to the end (right).
                modifier = Modifier.wrapContentWidth() // Column takes only necessary width.
            ) {
                // Display community rating if available.
                game.communityRating?.let { rating ->
                    Text(
                        // Assuming `communityRating` is 0-100 from IGDB, scale it to 0-10 for display.
                        text = "%.1f ★".format(rating / 10.0),
                        style = MaterialTheme.typography.titleMedium, // Prominent style for rating.
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary // Use primary color for emphasis.
                    )
                }
            }
        }
    }
}

// --- LibraryGameListItem Composable ---
// A specialized Composable for displaying a game item within the user's Library screen.

@Composable
fun LibraryGameListItem(
    game: Game,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Game) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .combinedClickable( // Handles both regular and long clicks.
                onClick = { onItemClick(game.id) },
                onLongClick = { onItemLongClick(game) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Game Cover Image
            AsyncImage(
                model = game.coverUrl,
                contentDescription = "${game.name} cover",
                modifier = Modifier
                    .size(width = 80.dp, height = 120.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Game Info Column (Name, Developer)
            Column(
                modifier = Modifier.weight(1f) // Takes available horizontal space.
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Display developer name if available.
                game.developerName?.let { devName ->
                    if (devName != "N/A") {
                        Text(
                            text = devName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Display release date if available.
                game.releaseDateTimestamp?.let { timestamp ->
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    Text(
                        text = "Release: ${formatter.format(Date(timestamp * 1000L))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Ratings Column (User Rating and Community Rating, aligned to the right)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                // Display user's app-specific rating if available.
                game.userRating?.let { userRating ->
                    Text(
                        text = "%.1f ★".format(userRating), // User's personal rating.
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                // Display critics (API) rating if available.
                game.communityRating?.let { apiRating ->
                    Text(
                        // Assuming `apiRating` (communityRating) is 0-100, scale to 0-10.
                        text = "%.1f".format(apiRating / 10.0),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// --- LoadingIndicator Composable ---
// Used to indicate loading states.

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// --- ErrorMessage Composable ---
// Displays an error message and an optional retry button.

@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error: $message",
            color = MaterialTheme.colorScheme.error
        )
        // Display retry button if onRetry lambda is provided.
        onRetry?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = it) {
                Text("Retry")
            }
        }
    }
}

// --- EmptyState Composable ---
// Displays a message when a list is empty or no data is available.

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}