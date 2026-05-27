package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import android.content.Context
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.ProjectRepository
import androidx.compose.ui.platform.LocalContext

object AppDestinations {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val EDITOR = "editor"
    const val BULK = "bulk"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("bgwrap_prefs", Context.MODE_PRIVATE)
    }
    
    // Wire Database and Repository safely using LocalContext
    val db = AppDatabase.getDatabase(context)
    val repository = ProjectRepository(db.projectDao(), context)
    
    // Provide state controller ViewModel to screens
    val stateViewModel: RemBgViewModel = viewModel(
        factory = RemBgViewModelFactory(repository)
    )

    NavHost(
        navController = navController,
        startDestination = AppDestinations.SPLASH
    ) {
        // 1. Splash Transition screen
        composable(AppDestinations.SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    val hasCompletedOnboarding = sharedPrefs.getBoolean("has_completed_onboarding", false)
                    val destination = if (hasCompletedOnboarding) AppDestinations.HOME else AppDestinations.ONBOARDING
                    if (navController.currentDestination?.route == AppDestinations.SPLASH) {
                        navController.navigate(destination) {
                            popUpTo(AppDestinations.SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 2. Onboarding slides Carousel
        composable(AppDestinations.ONBOARDING) {
            OnboardingScreen(
                onOnboardingComplete = {
                    sharedPrefs.edit().putBoolean("has_completed_onboarding", true).apply()
                    if (navController.currentDestination?.route == AppDestinations.ONBOARDING) {
                        navController.navigate(AppDestinations.HOME) {
                            popUpTo(AppDestinations.ONBOARDING) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 3. Home dashboard, list of local processed items
        composable(AppDestinations.HOME) {
            HomeScreen(
                viewModel = stateViewModel,
                onNavigateToEditor = {
                    navController.navigate(AppDestinations.EDITOR)
                },
                onNavigateToBulk = {
                    navController.navigate(AppDestinations.BULK)
                }
            )
        }

        // 4. Fine grain professional Editor
        composable(AppDestinations.EDITOR) {
            EditorScreen(
                viewModel = stateViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 5. Bulk batch removal progress screen
        composable(AppDestinations.BULK) {
            BulkScreen(
                viewModel = stateViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
