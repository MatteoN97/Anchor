package ch.bfh.anchor

import kotlinx.coroutines.flow.first
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ch.bfh.anchor.ui.journal.NoteEditorScreen
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.bfh.anchor.data.AnchorDatabase
import ch.bfh.anchor.model.AppLanguage
import ch.bfh.anchor.model.effectiveCrisisLine
import ch.bfh.anchor.platform.LocalAppLocale
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
import ch.bfh.anchor.ui.settings.SettingsPage
import ch.bfh.anchor.ui.settings.SettingsPageScreen
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
/** A topic page of Settings, e.g. "settings/language". */
private const val ROUTE_SETTINGS_PAGE = "settings/{page}"
private const val ROUTE_NOTE = "note?id={id}"
private fun noteRoute(id: String?) = if (id == null) "note" else "note?id=$id"

/**
 * [reminderController] schedules mood reminders natively on Android and iOS (each platform passes
 * its own controller). [openMoodRequest] is bumped each time a reminder notification is tapped.
 */
@Composable
fun App(
    database: AnchorDatabase,
    settingsManager: SettingsManager,
    reminderController: MoodReminderController = NoMoodReminderController,
    openMoodRequest: Int = 0
) {
    val language by settingsManager.appLanguage.collectAsState()
    val themeMode by settingsManager.themeMode.collectAsState()
    val dynamicColor by settingsManager.dynamicColor.collectAsState()
    // Set when the language is changed from Settings, so the rebuilt app reopens there.
    var reopenSettings by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalAppLocale provides language.code) {
        // Strings are looked up once per screen; rebuilding everything under a new key makes
        // every one of them load again in the new language. The theme is not part of the key:
        // changing it recolours in place, without rebuilding.
        key(language) {
            AnchorTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                val vm: AnchorViewModel = viewModel { AnchorViewModel(database) }
                AnchorApp(
                    vm = vm,
                    settingsManager = settingsManager,
                    reminderController = reminderController,
                    openMoodRequest = openMoodRequest,
                    startInSettings = reopenSettings,
                    onLanguageChange = { newLanguage ->
                        reopenSettings = true
                        settingsManager.setAppLanguage(newLanguage)
                    }
                )
            }
        }
    }
}

@Composable
fun AnchorApp(
    vm: AnchorViewModel,
    settingsManager: SettingsManager,
    reminderController: MoodReminderController = NoMoodReminderController,
    openMoodRequest: Int = 0,
    startInSettings: Boolean = false,
    onLanguageChange: (AppLanguage) -> Unit = settingsManager::setAppLanguage
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isIos = LocalIsIos.current

    // These effects can run before the NavHost has set its graph (at launch, or right after a
    // language change rebuilt the app), and navigating then throws. Waiting for the first back
    // stack entry waits for the graph.
    LaunchedEffect(Unit) {
        // Back on the Language page the change was made on.
        if (startInSettings) {
            navController.currentBackStackEntryFlow.first()
            navController.navigateToTab(AnchorTab.Settings)
            navController.navigate("settings/${SettingsPage.Language.route}")
        }
    }

    // A tapped mood reminder opens the tracker.
    LaunchedEffect(openMoodRequest) {
        if (openMoodRequest > 0) {
            navController.currentBackStackEntryFlow.first()
            if (navController.currentDestination?.route != ROUTE_MOOD) {
                navController.navigate(ROUTE_MOOD) { launchSingleTop = true }
            }
        }
    }

    // The crisis flow and the breathing practice are destinations of their own and own the whole
    // screen, so the bottom bar is gone while either is open: nothing to tap through, and the
    // system back button leaves them.
    val showBottomBar = currentRoute !in setOf(ROUTE_CRISIS, ROUTE_BREATHING, ROUTE_NOTE)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AnchorBottomBar(currentRoute = currentRoute, navController = navController)
            }
        },
        // Each screen draws its own top bar or status-bar padding, so its background can run up
        // behind the status bar. The bottom and sides are handled here.
        // System bars and cutout only, not the keyboard: safeDrawing includes the IME, so every
        // frame of the keyboard animation re-measured the whole page behind a text field. Screens
        // that type (the note editor) pad for the keyboard themselves; sheets do it on their own.
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
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
            // A page under Home, not a tab: the bar stays visible with Home selected.
            composable(ROUTE_MOOD) {
                MoodTrackerScreen(
                    vm = vm,
                    onBack = dropUnlessResumed { navController.popBackStack() }
                )
            }
            modalComposable(ROUTE_CRISIS, isIos) {
                val region = rememberDeviceRegion()
                CrisisScreen(
                    // A second tap during the exit animation would pop Home too and leave a blank screen.
                    onDismiss = dropUnlessResumed { navController.popBackStack() },
                    vm = vm,
                    breathingPattern = settingsManager.crisisBreathingPattern,
                    crisisLine = effectiveCrisisLine(settingsManager.customCrisisLine, region),
                    trustedContact = settingsManager.trustedContact
                )
            }
            modalComposable(ROUTE_BREATHING, isIos) {
                BreathingPracticeScreen(
                    pattern = settingsManager.crisisBreathingPattern,
                    onDismiss = dropUnlessResumed { navController.popBackStack() }
                )
            }
            composable(AnchorTab.Toolkit.route) { ToolkitScreen(viewModel = vm) }
            composable(AnchorTab.Journal.route) {
                JournalScreen(viewModel = vm, onOpenNote = { id -> navController.navigate(noteRoute(id)) })
            }
            // Full-screen writing: no tab bar, slides up like a modal on iOS.
            modalComposable(
                ROUTE_NOTE,
                isIos,
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true })
            ) { entry ->
                NoteEditorScreen(
                    vm = vm,
                    noteId = entry.savedStateHandle.get<String>("id"),
                    onClose = dropUnlessResumed { navController.popBackStack() }
                )
            }
            composable(AnchorTab.Settings.route) {
                SettingsScreen(
                    settingsManager = settingsManager,
                    reminderController = reminderController,
                    onOpenPage = { page -> navController.navigate("settings/${page.route}") }
                )
            }
            pushedComposable(ROUTE_SETTINGS_PAGE, isIos) { entry ->
                val page = SettingsPage.fromRoute(entry.savedStateHandle.get<String>("page"))
                    ?: return@pushedComposable
                SettingsPageScreen(
                    page = page,
                    settingsManager = settingsManager,
                    reminderController = reminderController,
                    onLanguageChange = onLanguageChange,
                    onAddSampleMoods = vm::addSampleMoods,
                    onRemoveSampleMoods = vm::removeSampleMoods,
                    onBack = dropUnlessResumed { navController.popBackStack() }
                )
            }
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
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    if (!isIos) {
        composable(route, arguments = arguments) { content(it) }
        return
    }
    val slide = tween<IntOffset>(durationMillis = 400, easing = FastOutSlowInEasing)
    composable(
        route,
        arguments = arguments,
        enterTransition = { slideInVertically(slide) { it } },
        popExitTransition = { slideOutVertically(slide) { it } }
    ) { content(it) }
}

/**
 * A page pushed within a tab. On iOS it slides in from the right over the page it came from,
 * which moves a little to the left, and leaves the same way back, as in a navigation stack;
 * on Android it uses the NavHost defaults.
 */
private fun NavGraphBuilder.pushedComposable(
    route: String,
    isIos: Boolean,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    if (!isIos) {
        composable(route) { content(it) }
        return
    }
    val slide = tween<IntOffset>(durationMillis = 350, easing = FastOutSlowInEasing)
    composable(
        route,
        enterTransition = { slideInHorizontally(slide) { it } },
        exitTransition = { slideOutHorizontally(slide) { -it / 3 } },
        popEnterTransition = { slideInHorizontally(slide) { -it / 3 } },
        popExitTransition = { slideOutHorizontally(slide) { it } }
    ) { content(it) }
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
        // Likewise Settings for its topic pages.
        selectedIndex = when (currentRoute) {
            ROUTE_MOOD -> AnchorTab.Home.ordinal
            ROUTE_SETTINGS_PAGE -> AnchorTab.Settings.ordinal
            else -> AnchorTab.entries.indexOfFirst { it.route == currentRoute }
        },
        onSelect = { index ->
            val tab = AnchorTab.entries[index]
            // Home acts as Back from the tracker: drop it rather than saving it for later.
            if (currentRoute == ROUTE_MOOD && tab == AnchorTab.Home &&
                navController.popBackStack(AnchorTab.Home.route, inclusive = false)
            ) return@AdaptiveTabBar
            // Settings again, from one of its pages, goes back to the main list, as on iOS.
            if (currentRoute == ROUTE_SETTINGS_PAGE && tab == AnchorTab.Settings &&
                navController.popBackStack(AnchorTab.Settings.route, inclusive = false)
            ) return@AdaptiveTabBar
            navController.navigateToTab(tab)
        }
    )
}

private fun NavHostController.navigateToTab(tab: AnchorTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
