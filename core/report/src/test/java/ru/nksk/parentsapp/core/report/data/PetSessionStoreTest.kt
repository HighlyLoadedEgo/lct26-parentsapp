package ru.nksk.parentsapp.core.report.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PetSessionStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun `selected device survives reopening and reset stays cleared`() = runTest {
        val file = File(temporaryFolder.root, "report.preferences_pb")
        suspend fun openAndClose(block: suspend (PetSessionStore) -> Unit) {
            val scope = CoroutineScope(coroutineContext + SupervisorJob())
            try {
                block(PetSessionStoreImpl(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })))
            } finally {
                scope.coroutineContext[Job]!!.cancel()
                scope.coroutineContext[Job]!!.join()
            }
        }
        openAndClose { it.savePetId("9f1c2d3e4a5b6078") }
        openAndClose {
            assertEquals("9f1c2d3e4a5b6078", it.lastPetId())
            it.clearPetId()
        }
        openAndClose { assertNull(it.lastPetId()) }
    }

    @Test fun `linked pets persist and switching moves the pet to the front`() = runTest {
        val file = File(temporaryFolder.root, "report.preferences_pb")
        suspend fun openAndClose(block: suspend (PetSessionStore) -> Unit) {
            val scope = CoroutineScope(coroutineContext + SupervisorJob())
            try {
                block(PetSessionStoreImpl(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })))
            } finally {
                scope.coroutineContext[Job]!!.cancel()
                scope.coroutineContext[Job]!!.join()
            }
        }
        openAndClose { store ->
            store.savePetId("aaa")
            store.savePetId("bbb")
            assertEquals(listOf("bbb", "aaa"), store.observePets().first().map(LinkedPet::petId))
            assertEquals("bbb", store.lastPetId())
        }
        openAndClose { store ->
            assertEquals(listOf("bbb", "aaa"), store.observePets().first().map(LinkedPet::petId))
            store.renamePet("aaa", "Рыжик")
        }
        openAndClose { store ->
            assertEquals("Рыжик", store.observePets().first().single { it.petId == "aaa" }.name)
        }
    }

    @Test fun `legacy single pet install shows its pet without stored roster`() = runTest {
        val file = File(temporaryFolder.root, "report.preferences_pb")
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            // Simulate an old install: only the selected id exists, no roster key yet.
            dataStore.edit {
                it[stringPreferencesKey("last_pet_id")] = "legacy"
            }
            val store = PetSessionStoreImpl(dataStore)
            assertEquals(listOf(LinkedPet("legacy")), store.observePets().first())
            // Saving again adopts the legacy pet into the roster.
            store.savePetId("legacy")
            assertEquals(listOf(LinkedPet("legacy")), store.observePets().first())
        } finally {
            scope.coroutineContext[Job]!!.cancel()
            scope.coroutineContext[Job]!!.join()
        }
    }

    @Test fun `removing the selected pet selects the next one or none`() = runTest {
        val file = File(temporaryFolder.root, "report.preferences_pb")
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val store = PetSessionStoreImpl(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
            store.savePetId("aaa")
            store.savePetId("bbb")
            store.savePetId("ccc")
            store.removePet("ccc")
            assertEquals("bbb", store.lastPetId())
            assertEquals(listOf("bbb", "aaa"), store.observePets().first().map(LinkedPet::petId))
            store.removePet("bbb")
            store.removePet("aaa")
            assertNull(store.lastPetId())
            assertEquals(emptyList<LinkedPet>(), store.observePets().first())
        } finally {
            scope.coroutineContext[Job]!!.cancel()
            scope.coroutineContext[Job]!!.join()
        }
    }
}
