package ru.nksk.parentsapp.feature.quests.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One real-life quest created by the parent; the reward is paid out in the kids app. */
data class ActiveQuest(
    val title: String,
    val rewardCoins: Int,
)

/** Device-local storage of the parent-created quest; sync with the backend is future work. */
interface QuestStore {
    fun observeActiveQuest(): Flow<ActiveQuest?>
    suspend fun setActiveQuest(quest: ActiveQuest)
    suspend fun clearActiveQuest()
}

private val Context.questsDataStore by preferencesDataStore(name = "quests_store")

@Singleton
class QuestStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : QuestStore {
    private companion object {
        val QUEST_TITLE = stringPreferencesKey("quest_title")
        val QUEST_REWARD = intPreferencesKey("quest_reward")
    }

    override fun observeActiveQuest(): Flow<ActiveQuest?> =
        context.questsDataStore.data.map { preferences ->
            val title = preferences[QUEST_TITLE] ?: return@map null
            ActiveQuest(title = title, rewardCoins = preferences[QUEST_REWARD] ?: 0)
        }

    override suspend fun setActiveQuest(quest: ActiveQuest) {
        context.questsDataStore.edit { preferences ->
            preferences[QUEST_TITLE] = quest.title
            preferences[QUEST_REWARD] = quest.rewardCoins
        }
    }

    override suspend fun clearActiveQuest() {
        context.questsDataStore.edit { preferences ->
            preferences.remove(QUEST_TITLE)
            preferences.remove(QUEST_REWARD)
        }
    }
}
