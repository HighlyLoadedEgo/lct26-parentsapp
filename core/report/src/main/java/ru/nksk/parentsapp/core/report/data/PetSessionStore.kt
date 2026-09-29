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

/** Remembers the pet whose report was opened last, so the next launch lands on it. */
interface PetSessionStore {
    suspend fun lastPetId(): String?

    /** Emits on every change of the remembered pet, including clears. */
    fun observePetId(): Flow<String?>

    suspend fun savePetId(petId: String)
    suspend fun clearPetId()
}

private val Context.reportDataStore by preferencesDataStore(name = "report_store")

@Singleton
class PetSessionStoreImpl internal constructor(private val store: DataStore<Preferences>) : PetSessionStore {
    @Inject constructor(@ApplicationContext context: Context) : this(context.reportDataStore)
    private companion object {
        val LAST_PET_ID = stringPreferencesKey("last_pet_id")
    }

    override suspend fun lastPetId(): String? =
        store.data.first()[LAST_PET_ID]

    override fun observePetId(): Flow<String?> =
        store.data.map { preferences -> preferences[LAST_PET_ID] }

    override suspend fun savePetId(petId: String) {
        store.edit { it[LAST_PET_ID] = petId }
    }

    override suspend fun clearPetId() {
        store.edit { it.remove(LAST_PET_ID) }
    }
}
