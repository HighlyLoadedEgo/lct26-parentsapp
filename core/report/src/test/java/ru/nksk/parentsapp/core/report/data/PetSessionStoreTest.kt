package ru.nksk.parentsapp.core.report.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
}
