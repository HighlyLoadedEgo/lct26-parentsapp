package ru.nksk.parentsapp.core.report.rewards

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class FrozenQuestReward(val requestId: String, val questId: String, val request: CreateQuestRewardRequest)

/** Commit the complete immutable request before HTTP; uncertain outcomes survive process death. */
@Singleton
internal class QuestRewardRequestStore internal constructor(
    private val preferences: DataStore<Preferences>, private val json: Json,
) {
    @Inject constructor(@ApplicationContext context: Context, json: Json) : this(
        PreferenceDataStoreFactory.create(produceFile = {
            File(context.noBackupFilesDir, "parent_quest_rewards.preferences_pb")
        }), json,
    )

    suspend fun pending(deviceId: String, runId: String, questId: String): FrozenQuestReward? =
        preferences.data.first()[key(deviceId, runId, questId)]?.let { json.decodeFromString(it) }

    suspend fun stage(value: FrozenQuestReward) {
        preferences.edit { saved ->
            val key = key(value.request.deviceId, value.request.gameRunId, value.questId)
            val previous = saved[key]?.let { json.decodeFromString<FrozenQuestReward>(it) }
            check(previous == null || previous == value) { "Запрос выдачи уже сохранён. Повторите его." }
            saved[key] = json.encodeToString(value)
        }
    }

    suspend fun clear(value: FrozenQuestReward) {
        preferences.edit { saved ->
            val key = key(value.request.deviceId, value.request.gameRunId, value.questId)
            val previous = saved[key]?.let { json.decodeFromString<FrozenQuestReward>(it) }
            if (previous == value) saved.remove(key)
        }
    }

    suspend fun blockedUntil(): Long = preferences.data.first()[RETRY_AFTER] ?: 0L
    suspend fun deferUntil(deadline: Long) {
        preferences.edit { it[RETRY_AFTER] = maxOf(it[RETRY_AFTER] ?: 0, deadline) }
    }

    private fun key(deviceId: String, runId: String, questId: String) =
        stringPreferencesKey("request:" + json.encodeToString(listOf(deviceId, runId, questId)))

    private companion object { val RETRY_AFTER = longPreferencesKey("rewards_retry_after") }
}
