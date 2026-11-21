package ch.bfh.anchor

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.bfh.anchor.data.AnchorDatabase
import ch.bfh.anchor.model.effectiveCrisisLine
import ch.bfh.anchor.platform.rememberDeviceRegion
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.tab_home
import ch.bfh.anchor.resources.tab_journal
import ch.bfh.anchor.resources.tab_settings
import ch.bfh.anchor.resources.tab_toolkit
import ch.bfh.anchor.settings.SettingsManager
import ch.bfh.anchor.ui.breathing.BreathingPracticeScreen
import ch.bfh.anchor.ui.crisis.CrisisScreen
import ch.bfh.anchor.ui.home.HomeScreen
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.journal.JournalScreen
import ch.bfh.anchor.ui.settings.SettingsScreen
import ch.bfh.anchor.ui.theme.AnchorTheme
import ch.bfh.anchor.ui.toolkit.ToolkitScreen
import ch.bfh.anchor.viewmodel.AnchorViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val ROUTE_CRISIS = "crisis"
private const val ROUTE_BREATHING = "breathing"

@Composable
fun App(database: AnchorDatabase, settingsManager: SettingsManager) {
    AnchorTheme {
        val vm: AnchorViewModel = viewModel { AnchorViewModel(database) }
        AnchorApp(vm = vm, settingsManager = settingsManager)
    }
}

@Composable
fun AnchorApp(vm: AnchorViewModel, settingsManager: SettingsManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // The crisis flow and the breathing practice are destinations of their own and own the whole
    // screen, so the bottom bar is gone while either is open: nothing to tap through, and the
    // system back button leaves them.
    val showBottomBar = currentRoute !in setOf(ROUTE_CRISIS, ROUTE_BREATHING)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AnchorBottomBar(currentRoute = currentRoute, navController = navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AnchorTab.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AnchorTab.Home.route) {
                HomeScreen(
                    vm = vm,
                    settingsManager = settingsManager,
                    onSosClick = { navController.navigate(ROUTE_CRISIS) },
                    onBreathingClick = { navController.navigate(ROUTE_BREATHING) }
                )
            }
            composable(ROUTE_CRISIS) {
                val region = rememberDeviceRegion()
                CrisisScreen(
                    onDismiss = { navController.popBackStack() },
                    vm = vm,
                    breathingPattern = settingsManager.crisisBreathingPattern,
                    crisisLine = effectiveCrisisLine(settingsManager.customCrisisLine, region),
                    trustedContact = settingsManager.trustedContact
                )
            }
            composable(ROUTE_BREATHING) {
                BreathingPracticeScreen(onDismiss = { navController.popBackStack() })
            }
            composable(AnchorTab.Toolkit.route) { ToolkitScreen(viewModel = vm) }
            composable(AnchorTab.Journal.route) { JournalScreen(viewModel = vm) }
            composable(AnchorTab.Settings.route) { SettingsScreen(settingsManager = settingsManager) }
        }
    }
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
    NavigationBar {
        AnchorTab.entries.forEach { tab ->
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
