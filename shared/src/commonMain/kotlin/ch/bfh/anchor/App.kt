package ch.bfh.anchor

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavGraphBuilder
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.bfh.anchor.data.AnchorDatabase
import ch.bfh.anchor.model.effectiveCrisisLine
import ch.bfh.anchor.notifications.MoodReminderController
import ch.bfh.anchor.notifications.NoMoodReminderController
import ch.bfh.anchor.platform.rememberDeviceRegion
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.tab_home
import ch.bfh.anchor.resources.tab_journal
import ch.bfh.anchor.resources.tab_settings
import ch.bfh.anchor.resources.tab_toolkit
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.ui.adaptive.AdaptiveTabBar
import ch.bfh.anchor.ui.adaptive.TabItem
import ch.bfh.anchor.ui.breathing.BreathingPracticeScreen
import ch.bfh.anchor.ui.crisis.CrisisScreen
import ch.bfh.anchor.ui.home.HomeScreen
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.journal.JournalScreen
import ch.bfh.anchor.ui.mood.MoodTrackerScreen
import ch.bfh.anchor.ui.settings.SettingsScreen
import ch.bfh.anchor.ui.theme.AnchorTheme
import ch.bfh.anchor.ui.theme.LocalIsIos
import ch.bfh.anchor.ui.toolkit.ToolkitScreen
import ch.bfh.anchor.viewmodel.AnchorViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val ROUTE_CRISIS = "crisis"
private const val ROUTE_BREATHING = "breathing"
private const val ROUTE_MOOD = "mood"

@Composable
fun App(
    database: AnchorDatabase,
    settingsManager: SettingsManager,
    reminderController: MoodReminderController = NoMoodReminderController,
    openMoodRequest: Int = 0
) {
    AnchorTheme {
        val vm: AnchorViewModel = viewModel { AnchorViewModel(database) }
        AnchorApp(
            vm = vm,
            settingsManager = settingsManager,
            reminderController = reminderController,
            openMoodRequest = openMoodRequest
        )
    }
}

@Composable
fun AnchorApp(
    vm: AnchorViewModel,
    settingsManager: SettingsManager,
    reminderController: MoodReminderController = NoMoodReminderController,
    openMoodRequest: Int = 0
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isIos = LocalIsIos.current

    // A tapped mood reminder bumps openMoodRequest, which opens the tracker.
    LaunchedEffect(openMoodRequest) {
        if (openMoodRequest > 0 && navController.currentDestination?.route != ROUTE_MOOD) {
            navController.navigate(ROUTE_MOOD) { launchSingleTop = true }
        }
    }

    // The crisis flow and the breathing practice are destinations of their own and own the whole
    // screen, so the bottom bar is gone while either is open: nothing to tap through, and the
    // system back button leaves them.
    val showBottomBar = currentRoute !in setOf(ROUTE_CRISIS, ROUTE_BREATHING)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AnchorBottomBar(currentRoute = currentRoute, navController = navController)
            }
        },
        // Each screen draws its own top bar or status-bar padding, so its background can run up
        // behind the status bar. The bottom and sides are handled here.
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AnchorTab.Home.route,
            modifier = Modifier
                .padding(padding)
                // Without this, screens with their own Scaffold added the insets a second time.
                .consumeWindowInsets(padding),
            // iOS switches tabs instantly; Android keeps navigation-compose's default cross-fade.
            enterTransition = { if (isIos) EnterTransition.None else fadeIn(tween(700)) },
            exitTransition = { if (isIos) ExitTransition.None else fadeOut(tween(700)) }
        ) {
            composable(AnchorTab.Home.route) {
                HomeScreen(
                    vm = vm,
                    settingsManager = settingsManager,
                    onSosClick = { navController.navigate(ROUTE_CRISIS) },
                    onBreathingClick = { navController.navigate(ROUTE_BREATHING) },
                    onMoodClick = { navController.navigate(ROUTE_MOOD) }
                )
            }
            composable(ROUTE_MOOD) {
                MoodTrackerScreen(
                    vm = vm,
                    settingsManager = settingsManager,
                    reminderController = reminderController,
                    onBack = { navController.popBackStack() }
                )
            }
            modalComposable(ROUTE_CRISIS, isIos) {
                val region = rememberDeviceRegion()
                CrisisScreen(
                    onDismiss = { navController.popBackStack() },
                    vm = vm,
                    breathingPattern = settingsManager.crisisBreathingPattern,
                    crisisLine = effectiveCrisisLine(settingsManager.customCrisisLine, region),
                    trustedContact = settingsManager.trustedContact
                )
            }
            modalComposable(ROUTE_BREATHING, isIos) {
                BreathingPracticeScreen(onDismiss = { navController.popBackStack() })
            }
            composable(AnchorTab.Toolkit.route) { ToolkitScreen(viewModel = vm) }
            composable(AnchorTab.Journal.route) { JournalScreen(viewModel = vm) }
            composable(AnchorTab.Settings.route) { SettingsScreen(settingsManager = settingsManager) }
        }
    }
}

/**
 * A full-screen destination. On iOS it slides up over the tab and back down when closed, like a
 * full-screen modal; on Android it uses the NavHost defaults.
 */
private fun NavGraphBuilder.modalComposable(
    route: String,
    isIos: Boolean,
    content: @Composable () -> Unit
) {
    if (!isIos) {
        composable(route) { content() }
        return
    }
    val slide = tween<IntOffset>(durationMillis = 400, easing = FastOutSlowInEasing)
    composable(
        route,
        enterTransition = { slideInVertically(slide) { it } },
        popExitTransition = { slideOutVertically(slide) { it } }
    ) { content() }
}

/**
 * The four tabs, in bar order. Routes live here so the graph and the bar cannot drift apart.
 */
private enum class AnchorTab(
    val route: String,
    val icon: ImageVector,
    val labelRes: StringResource
) {
    Home("home", AnchorIcons.Home, Res.string.tab_home),
    Toolkit("toolkit", AnchorIcons.Favorite, Res.string.tab_toolkit),
    Journal("journal", AnchorIcons.MenuBook, Res.string.tab_journal),
    Settings("settings", AnchorIcons.Settings, Res.string.tab_settings)
}

@Composable
private fun AnchorBottomBar(currentRoute: String?, navController: NavHostController) {
    val tabs = AnchorTab.entries.map { TabItem(label = stringResource(it.labelRes), icon = it.icon) }
    AdaptiveTabBar(
        tabs = tabs,
        // The mood tracker is opened from Home, so Home stays highlighted while it is open.
        selectedIndex = if (currentRoute == ROUTE_MOOD) AnchorTab.Home.ordinal
        else AnchorTab.entries.indexOfFirst { it.route == currentRoute },
        onSelect = { index ->
            val tab = AnchorTab.entries[index]
            // Home acts as Back from the tracker: drop it rather than restore it later.
            if (currentRoute == ROUTE_MOOD && tab == AnchorTab.Home &&
                navController.popBackStack(AnchorTab.Home.route, inclusive = false)
            ) return@AdaptiveTabBar
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    )
}
