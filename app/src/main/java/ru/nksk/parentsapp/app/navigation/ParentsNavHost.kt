package ru.nksk.parentsapp.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ru.nksk.parentsapp.feature.pin.navigation.PinLock
import ru.nksk.parentsapp.feature.pin.navigation.PinSetup
import ru.nksk.parentsapp.feature.pin.navigation.pinLockEntry
import ru.nksk.parentsapp.feature.pin.navigation.pinSetupEntry
import ru.nksk.parentsapp.feature.questions.navigation.QuestionTopic
import ru.nksk.parentsapp.feature.questions.navigation.questionTopicEntry
import ru.nksk.parentsapp.feature.quests.navigation.Quests
import ru.nksk.parentsapp.feature.quests.navigation.questsEntry
import ru.nksk.parentsapp.feature.report.navigation.PetIdInput
import ru.nksk.parentsapp.feature.report.navigation.Statistics
import ru.nksk.parentsapp.feature.report.navigation.petIdInputEntry
import ru.nksk.parentsapp.feature.report.navigation.statisticsEntry
import ru.nksk.parentsapp.feature.scanner.navigation.Scanner
import ru.nksk.parentsapp.feature.scanner.navigation.scannerEntry

/** Navigation composition root. Features own their keys and entries; screens receive callbacks. */
@Composable
fun ParentsNavHost(
    startKey: NavKey,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(ParentsNavigationSavedStateConfiguration, startKey)
    val navigator = remember(backStack) { AppNavigator(backStack) }
    NavDisplay(
        modifier = modifier.fillMaxSize(),
        backStack = backStack,
        onBack = navigator::goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            pinSetupEntry(onDone = { navigator.resetTo(Statistics()) })
            pinLockEntry(onUnlocked = { navigator.resetTo(Statistics()) })
            statisticsEntry(
                onScanQr = { source -> navigator.navigate(source, Scanner) },
                onManualInput = { source -> navigator.navigate(source, PetIdInput) },
                onChangePet = { source -> navigator.replace(source, Statistics()) },
                onOpenTopic = { source, skillId, _ ->
                    // The report feature stays route-agnostic: the host picks the destination.
                    navigator.navigate(source, QuestionTopic(skillId))
                },
                onOpenQuests = { source -> navigator.navigate(source, Quests) },
            )
            petIdInputEntry(
                onBack = navigator::goBack,
                onOpen = { source, petId -> navigator.replace(source, Statistics()) },
            )
            scannerEntry(
                onScanned = { source, value ->
                    navigator.replace(source, Statistics(petId = value))
                },
                onBack = navigator::goBack,
            )
            questionTopicEntry(onBack = navigator::goBack)
            questsEntry(onBack = navigator::goBack)
        },
    )
}
