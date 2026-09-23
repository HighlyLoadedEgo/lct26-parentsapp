package ru.nksk.parentsapp.feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.nksk.parentsapp.feature.home.ui.HomeScreen
import ru.nksk.parentsapp.feature.home.ui.HomeViewModel

/** Entry point of the home feature; owns the ViewModel, keeps the screen stateless. */
@Composable
fun HomeEntry(modifier: Modifier = Modifier) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, modifier)
}
