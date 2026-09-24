package ru.nksk.parentsapp.feature.quests.navigation

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.nksk.parentsapp.feature.quests.ui.QuestsScreen

/** Standalone quests screen, pushed from the parent-mode report. */
@Serializable
@SerialName("quests")
data object Quests : NavKey

fun EntryProviderScope<NavKey>.questsEntry(onBack: (Quests) -> Unit) {
    entry<Quests> { source ->
        QuestsScreen(onBack = dropUnlessResumed { onBack(source) })
    }
}
