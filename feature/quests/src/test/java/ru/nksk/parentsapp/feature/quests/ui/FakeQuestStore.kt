package ru.nksk.parentsapp.feature.quests.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.nksk.parentsapp.feature.quests.data.ActiveQuest
import ru.nksk.parentsapp.feature.quests.data.QuestStore

internal class FakeQuestStore : QuestStore {
    val store = MutableStateFlow<ActiveQuest?>(null)

    /** Non-suspend mirror for assertions. */
    var current: ActiveQuest?
        get() = store.value
        set(value) { store.value = value }

    override fun observeActiveQuest(): Flow<ActiveQuest?> = store
    override suspend fun setActiveQuest(quest: ActiveQuest) { store.value = quest }
    override suspend fun clearActiveQuest() { store.value = null }
}
