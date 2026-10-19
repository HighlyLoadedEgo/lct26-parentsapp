package ru.nksk.parentsapp.core.report.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One linked pet: stable identity plus the display name cached from the last report. */
@Serializable
data class LinkedPet(val petId: String, val name: String? = null)

/** Remembers the linked pets of this family and whose report opens by default. */
interface PetSessionStore {
    suspend fun lastPetId(): String?

    /** Emits on every change of the remembered pet, including clears. */
    fun observePetId(): Flow<String?>

    /** Links the pet if needed, moves it to the front of the roster and selects it. */
    suspend fun savePetId(petId: String)
    suspend fun clearPetId()

    /** All linked pets, most recently selected first. */
    fun observePets(): Flow<List<LinkedPet>>

    /** Forgets a pet; if it was selected, the next one is selected or none stays. */
    suspend fun removePet(petId: String)

    /** Caches the display name shown in the switcher. */
    suspend fun renamePet(petId: String, name: String)
}

private val Context.reportDataStore by preferencesDataStore(name = "report_store")

@Singleton
class PetSessionStoreImpl internal constructor(private val store: DataStore<Preferences>) : PetSessionStore {
    @Inject constructor(@ApplicationContext context: Context) : this(context.reportDataStore)
    private companion object {
        val LAST_PET_ID = stringPreferencesKey("last_pet_id")
        val LINKED_PETS = stringPreferencesKey("linked_pets")
        val json = Json { ignoreUnknownKeys = true }
    }

    override suspend fun lastPetId(): String? =
        store.data.first()[LAST_PET_ID]

    override fun observePetId(): Flow<String?> =
        store.data.map { preferences -> preferences[LAST_PET_ID] }

    override suspend fun savePetId(petId: String) {
        store.edit { preferences ->
            preferences[LAST_PET_ID] = petId
            val pets = roster(preferences).filterNot { it.petId == petId }
            preferences[LINKED_PETS] = json.encodeToString(listOf(LinkedPet(petId)) + pets)
        }
    }

    override suspend fun clearPetId() {
        store.edit { preferences ->
            preferences.remove(LAST_PET_ID)
            preferences.remove(LINKED_PETS)
        }
    }

    override fun observePets(): Flow<List<LinkedPet>> = store.data.map { preferences ->
        // A legacy single-pet install shows its pet without rewriting storage.
        roster(preferences).ifEmpty {
            preferences[LAST_PET_ID]?.let { listOf(LinkedPet(it)) } ?: emptyList()
        }
    }

    override suspend fun removePet(petId: String) {
        store.edit { preferences ->
            val rest = roster(preferences).filterNot { it.petId == petId }
            preferences[LINKED_PETS] = json.encodeToString(rest)
            if (preferences[LAST_PET_ID] == petId) {
                val next = rest.firstOrNull()
                if (next == null) preferences.remove(LAST_PET_ID) else preferences[LAST_PET_ID] = next.petId
            }
        }
    }

    override suspend fun renamePet(petId: String, name: String) {
        store.edit { preferences ->
            val pets = roster(preferences)
            val updated = pets.map { if (it.petId == petId && it.name != name) it.copy(name = name) else it }
            if (updated != pets) preferences[LINKED_PETS] = json.encodeToString(updated)
        }
    }

    private fun roster(preferences: Preferences): List<LinkedPet> =
        preferences[LINKED_PETS]?.let { raw ->
            runCatching { json.decodeFromString<List<LinkedPet>>(raw) }.getOrDefault(emptyList())
        } ?: emptyList()
}
