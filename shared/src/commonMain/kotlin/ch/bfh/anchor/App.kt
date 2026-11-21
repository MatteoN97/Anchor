package ch.bfh.anchor



import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Icon

import androidx.compose.material3.NavigationBar

import androidx.compose.material3.NavigationBarItem

import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.ui.Modifier

import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.navigation.NavGraph.Companion.findStartDestination

import androidx.navigation.compose.NavHost

import androidx.navigation.compose.composable

import androidx.navigation.compose.currentBackStackEntryAsState

import androidx.navigation.compose.rememberNavController

import ch.bfh.anchor.data.AnchorDatabase
import ch.bfh.anchor.notifications.MoodReminderController
import ch.bfh.anchor.notifications.NoMoodReminderController
import ch.bfh.anchor.ui.mood.MoodTrackerScreen

import ch.bfh.anchor.resources.Res

import ch.bfh.anchor.resources.tab_home

import ch.bfh.anchor.resources.tab_journal

import ch.bfh.anchor.resources.tab_toolkit

import ch.bfh.anchor.settings.SettingsManager

import ch.bfh.anchor.ui.home.HomeScreen

import ch.bfh.anchor.ui.icons.AnchorIcons

import ch.bfh.anchor.ui.journal.JournalScreen

import ch.bfh.anchor.ui.settings.SettingsScreen

import ch.bfh.anchor.ui.theme.AnchorTheme

import ch.bfh.anchor.ui.toolkit.ToolkitScreen

import ch.bfh.anchor.viewmodel.AnchorViewModel

import org.jetbrains.compose.resources.stringResource



@Composable

fun App(
    database: AnchorDatabase,
    settingsManager: SettingsManager,
    reminderController: MoodReminderController = NoMoodReminderController,
    openMoodRequest: Int = 0
) {

    AnchorTheme {

        val vm: AnchorViewModel = viewModel { AnchorViewModel(database) }

        AnchorApp(vm = vm, settingsManager = settingsManager,
            reminderController = reminderController, openMoodRequest = openMoodRequest)

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

    LaunchedEffect(openMoodRequest) {
        if (openMoodRequest > 0 && navController.currentDestination?.route != "mood") {
            navController.navigate("mood") { launchSingleTop = true }
        }
    }



    Scaffold(

        bottomBar = {

            NavigationBar {

                NavigationBarItem(

                    selected = currentRoute == "home" || currentRoute == "mood",

                    onClick = {
                        if (currentRoute == "mood") {
                            // Home acts as Back from the tracker. Drop the mood destination
                            // (and any intervening destinations) rather than restoring it.
                            if (!navController.popBackStack("home", inclusive = false)) {
                                navController.navigate("home") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },

                    icon = {

                        Icon(

                            AnchorIcons.Home,

                            contentDescription = stringResource(Res.string.tab_home)

                        )

                    },

                    label = { Text(stringResource(Res.string.tab_home)) }

                )

                NavigationBarItem(

                    selected = currentRoute == "toolkit",

                    onClick = {

                        navController.navigate("toolkit") {

                            popUpTo(navController.graph.findStartDestination().id) {

                                saveState = true

                            }

                            launchSingleTop = true

                            restoreState = true

                        }

                    },

                    icon = {

                        Icon(

                            AnchorIcons.Favorite,

                            contentDescription = stringResource(Res.string.tab_toolkit)

                        )

                    },

                    label = { Text(stringResource(Res.string.tab_toolkit)) }

                )

                NavigationBarItem(

                    selected = currentRoute == "journal",

                    onClick = {

                        navController.navigate("journal") {

                            popUpTo(navController.graph.findStartDestination().id) {

                                saveState = true

                            }

                            launchSingleTop = true

                            restoreState = true

                        }

                    },

                    icon = {

                        Icon(

                            AnchorIcons.MenuBook,

                            contentDescription = stringResource(Res.string.tab_journal)

                        )

                    },

                    label = { Text(stringResource(Res.string.tab_journal)) }

                )

                NavigationBarItem(

                    selected = currentRoute == "settings",

                    onClick = {

                        navController.navigate("settings") {

                            popUpTo(navController.graph.findStartDestination().id) {

                                saveState = true

                            }

                            launchSingleTop = true

                            restoreState = true

                        }

                    },

                    icon = { Icon(AnchorIcons.Settings, contentDescription = "Settings") },

                    label = { Text("Settings") }

                )

            }

        }

    ) { padding ->

        NavHost(

            navController = navController,

            startDestination = "home",

            modifier = Modifier.padding(padding)

        ) {

            composable("home") {
                HomeScreen(vm = vm, settingsManager = settingsManager,
                    onMoodClick = { navController.navigate("mood") })
            }
            composable("mood") {
                MoodTrackerScreen(vm = vm, settingsManager = settingsManager,
                    reminderController = reminderController,
                    onBack = { navController.popBackStack() })
            }

            composable("toolkit") { ToolkitScreen(viewModel = vm) }

            composable("journal") { JournalScreen(viewModel = vm) }

            composable("settings") { SettingsScreen(settingsManager = settingsManager) }

        }

    }

}
