package ru.nksk.parentsapp.feature.pin.access

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LegacyPinMigrationTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun `existing SHA256 PIN survives migration and datastore reopen`() = runTest {
        val legacyFile = File(temporaryFolder.root, "legacy.preferences_pb")
        val destination = File(temporaryFolder.root, "new.preferences_pb")
        val salt = ByteArray(16) { it.toByte() }
        val hash = ru.nksk.parentsapp.feature.pin.security.PinHasher.hash("1234".toCharArray(), salt, 210_000)
        val record = "1|210000|${kotlin.io.encoding.Base64.encode(salt)}|${kotlin.io.encoding.Base64.encode(hash)}"
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val legacy = PreferenceDataStoreFactory.create(scope = scope, produceFile = { legacyFile })
        legacy.edit { it[stringPreferencesKey("pin_record")] = record }
        scope.coroutineContext[Job]!!.cancel()
        scope.coroutineContext[Job]!!.join()
        val migratedScope = CoroutineScope(coroutineContext + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = migratedScope,
            migrations = listOf(LegacyPinMigration(legacyFile)), produceFile = { destination })
        val repository = DataStorePinRepository(store, { 100_000 }, PinHasher())
        assertEquals(PinStatus.Configured(), repository.readStatus())
        assertEquals(VerifyPinResult.Accepted, repository.verifyPin("1234"))
        assertEquals(CreatePinResult.AlreadyConfigured, repository.createPin("9876"))
        migratedScope.coroutineContext[Job]!!.cancel()
        migratedScope.coroutineContext[Job]!!.join()
        val reopened = DataStorePinRepository(PreferenceDataStoreFactory.create(scope = backgroundScope,
            produceFile = { destination }), { 100_000 }, PinHasher())
        assertEquals(VerifyPinResult.Accepted, reopened.verifyPin("1234"))
        assertEquals(VerifyPinResult.Wrong, reopened.verifyPin("9876"))
    }
}
