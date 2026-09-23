package ru.nksk.parentsapp.app

import androidx.compose.runtime.Composable
import ru.nksk.parentsapp.core.ui.theme.ParentsAppTheme
import ru.nksk.parentsapp.feature.home.navigation.HomeEntry

/** Composition root for shared presentation and app-owned navigation. */
@Composable
fun ParentsApp() {
    ParentsAppTheme {
        HomeEntry()
    }
}
