package com.educationwithhassan.financetools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.educationwithhassan.financetools.ads.BannerAdView
import com.educationwithhassan.financetools.ads.InterstitialAdManager
import com.educationwithhassan.financetools.ads.RewardedAdManager
import com.educationwithhassan.financetools.domain.model.FinanceCategory
import com.educationwithhassan.financetools.domain.model.ToolCatalog
import com.educationwithhassan.financetools.ui.screens.*
import com.educationwithhassan.financetools.ui.theme.FinanceToolsTheme
import com.educationwithhassan.financetools.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var interstitialManager: InterstitialAdManager
    private lateinit var rewardedManager: RewardedAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        interstitialManager = InterstitialAdManager(this, cooldownSeconds = 120L).apply { preload() }
        rewardedManager = RewardedAdManager(this).apply { preload() }

        // Never blocks startup: shows App Open Ad only if already loaded & eligible
        (application as FinanceToolsApplication).appOpenAdManager.showAdIfAvailable(this)

        setContent {
            val vm: FinanceViewModel = viewModel()
            val prefs by vm.preferences.collectAsState()
            val history by vm.history.collectAsState()
            val favoriteIds by vm.favoriteIds.collectAsState()
            val recentIds by vm.recentIds.collectAsState()
            val fxStatus by vm.fxStatus.collectAsState()

            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            FinanceToolsTheme(darkTheme = prefs.darkMode) {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route ?: "home"

                val showBottomNav = !currentRoute.startsWith("tool/") &&
                    !currentRoute.startsWith("category/") &&
                    currentRoute != "ad_diagnostics"

                val showAdaptiveBanner = currentRoute == "home" ||
                    currentRoute == "search" ||
                    currentRoute.startsWith("category/") ||
                    currentRoute.startsWith("tool/")

                fun navigateToToolWithInterstitialCheck(toolId: String) {
                    interstitialManager.showIfEligibleOnToolNavigation(
                        activity = this@MainActivity,
                        targetToolId = toolId,
                        isUserEnteringInputs = false
                    ) {
                        vm.markToolOpened(toolId)
                        navController.navigate("tool/$toolId")
                    }
                }

                fun launchRewardedForPdfUnlock() {
                    rewardedManager.show(
                        activity = this@MainActivity,
                        onRewardEarned = { _, _ ->
                            vm.grantAdvancedPdfUnlock()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("PDF Financial Reports unlocked for 24 hours.")
                            }
                        },
                        onDismissedWithoutReward = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Ad closed before completion. No reward granted.")
                            }
                        },
                        onUnavailableOrFailed = { message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }

                fun launchRewardedForBonusSlots() {
                    rewardedManager.show(
                        activity = this@MainActivity,
                        onRewardEarned = { _, _ ->
                            vm.grantBonusSavedSlots()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("+15 bonus saved calculation slots unlocked.")
                            }
                        },
                        onDismissedWithoutReward = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Ad closed before completion. No bonus slots granted.")
                            }
                        },
                        onUnavailableOrFailed = { message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (showAdaptiveBanner) {
                                BannerAdView(applyNavigationBarPadding = !showBottomNav)
                            }
                            if (showBottomNav) {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = currentRoute == "home",
                                        onClick = { navController.navigate("home") { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                        label = { Text("Home") }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "search",
                                        onClick = { navController.navigate("search") { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.Search, contentDescription = "Tools") },
                                        label = { Text("120 Tools") }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "history",
                                        onClick = { navController.navigate("history") { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                        label = { Text("History") }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "settings",
                                        onClick = { navController.navigate("settings") { launchSingleTop = true } },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") }
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable("home") {
                            HomeDashboardScreen(
                                recentIds = recentIds,
                                favoriteIds = favoriteIds,
                                onOpenTool = { id -> navigateToToolWithInterstitialCheck(id) },
                                onOpenCategory = { cat -> navController.navigate("category/${cat.id}") },
                                onOpenSearch = { navController.navigate("search") },
                                onOpenSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("search") {
                            ToolSearchScreen(
                                onOpenTool = { id -> navigateToToolWithInterstitialCheck(id) }
                            )
                        }
                        composable("category/{catId}") { entry ->
                            val catId = entry.arguments?.getString("catId")
                            val category = FinanceCategory.entries.firstOrNull { it.id == catId } ?: FinanceCategory.PERSONAL
                            CategoryToolsScreen(
                                category = category,
                                onBack = { navController.popBackStack() },
                                onOpenTool = { id -> navigateToToolWithInterstitialCheck(id) }
                            )
                        }
                        composable("tool/{toolId}") { entry ->
                            val toolId = entry.arguments?.getString("toolId") ?: "loan-calculator"
                            val tool = ToolCatalog.byId[toolId] ?: ToolCatalog.allTools.first()
                            CalculatorDetailScreen(
                                tool = tool,
                                defaultCurrency = prefs.defaultCurrency,
                                isFavorite = favoriteIds.contains(tool.id),
                                isAdvancedPdfUnlocked = prefs.isAdvancedPdfUnlocked,
                                onToggleFavorite = { vm.toggleFavorite(tool.id) },
                                // Back navigation NEVER triggers an interstitial ad
                                onBack = { navController.popBackStack() },
                                onCalculate = { inputs, rows, curr -> vm.calculate(tool, inputs, rows, curr) },
                                onSave = { inputs, curr, out -> vm.saveCalculation(tool, inputs, curr, out) },
                                onWatchAdForPdfUnlock = { launchRewardedForPdfUnlock() },
                                onWatchAdForBonusSlots = { launchRewardedForBonusSlots() }
                            )
                        }
                        composable("history") {
                            HistoryFavoritesScreen(
                                history = history,
                                savedSlotsLimit = prefs.totalSavedSlotsLimit,
                                onOpenTool = { id -> navigateToToolWithInterstitialCheck(id) },
                                onDeleteItem = { vm.deleteCalculation(it) },
                                onClearHistory = { vm.clearHistory() },
                                onWatchAdForBonusSlots = { launchRewardedForBonusSlots() }
                            )
                        }
                        composable("settings") {
                            SettingsAboutPrivacyScreen(
                                darkMode = prefs.darkMode,
                                defaultCurrency = prefs.defaultCurrency,
                                fxStatus = fxStatus,
                                isAdvancedPdfUnlocked = prefs.isAdvancedPdfUnlocked,
                                onToggleDarkMode = { vm.setDarkMode(it) },
                                onChangeCurrency = { vm.setDefaultCurrency(it) },
                                onSyncFx = { vm.syncLiveExchangeRates() },
                                onWatchAdForPdfUnlock = { launchRewardedForPdfUnlock() },
                                onOpenAdDiagnostics = { navController.navigate("ad_diagnostics") }
                            )
                        }
                        composable("ad_diagnostics") {
                            AdDiagnosticsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::interstitialManager.isInitialized) {
            interstitialManager.destroy()
        }
        if (::rewardedManager.isInitialized) {
            rewardedManager.destroy()
        }
    }
}
