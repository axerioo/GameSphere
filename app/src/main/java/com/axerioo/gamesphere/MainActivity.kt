package com.axerioo.gamesphere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.axerioo.gamesphere.ui.Screen
import com.axerioo.gamesphere.ui.screens.details.GameDetailsScreen
import com.axerioo.gamesphere.ui.screens.explore.ExploreScreen
import com.axerioo.gamesphere.ui.screens.library.LibraryScreen
import com.axerioo.gamesphere.ui.screens.search.SearchScreen
import com.axerioo.gamesphere.ui.screens.soon.SoonScreen
import com.axerioo.gamesphere.ui.theme.GameSphereTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameSphereTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        Screen.Library,
        Screen.Explore,
        Screen.Soon,
        Screen.Search
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                // Get the current navigation back stack entry as state to observe changes.
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                // Get the current destination from the back stack entry.
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true // Save the state of popped destinations.
                                }
                                launchSingleTop = true // Avoid multiple copies of the same destination.
                                restoreState = true // Restore state when navigating to a previously visited destination.
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Define each Composable screen destination.
            composable(Screen.Library.route) { LibraryScreen(navController = navController) }
            composable(Screen.Explore.route) { ExploreScreen(navController = navController) }
            composable(Screen.Soon.route) { SoonScreen(navController = navController) }
            composable(Screen.Search.route) { SearchScreen(navController = navController) }
            composable(
                route = Screen.Details.route + "/{gameId}",
                arguments = listOf(navArgument("gameId") { type = NavType.LongType })
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getLong("gameId")
                requireNotNull(gameId) { "gameId parameter was not found. Please ensure it's passed." }
                GameDetailsScreen(navController = navController, gameId = gameId)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    GameSphereTheme {
        MainAppScreen()
    }
}