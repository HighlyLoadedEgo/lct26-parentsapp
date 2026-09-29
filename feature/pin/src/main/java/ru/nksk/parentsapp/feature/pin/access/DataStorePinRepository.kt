package ru.nksk.parentsapp.feature.pin.access

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataMigration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class DataStorePinRepository internal constructor(
    private val preferences: DataStore<Preferences>,
    private val nowMillis: () -> Long,
    private val hasher: PinHasher,
) : PinRepository {
    @Inject constructor(@ApplicationContext context: Context) : this(
        preferences = PreferenceDataStoreFactory.create(
            migrations = listOf(LegacyPinMigration(File(context.filesDir, "datastore/pin_store.preferences_pb"))),
            produceFile = { File(context.noBackupFilesDir, "parents_pin.preferences_pb") },
        ),
        nowMillis = System::currentTimeMillis,
        hasher = PinHasher(),
    )

    override suspend fun readStatus(): PinStatus = withContext(Dispatchers.Default) {
        val saved = preferences.data.first()
        saved.validatedRecord() ?: return@withContext PinStatus.NotConfigured
        PinStatus.Configured(saved.retryAfterMillis())
    }

    override suspend fun createPin(pin: String): CreatePinResult = withContext(Dispatchers.Default) {
        requireValidPin(pin)
        val record = hasher.create(pin)
        var result = CreatePinResult.AlreadyConfigured
        preferences.edit { saved ->
            if (saved.validatedRecord() == null) {
                saved[PIN_RECORD] = record
                saved[FAILED_ATTEMPTS] = 0
                saved[BLOCKED_UNTIL] = 0
                result = CreatePinResult.Created
            }
        }
        result
    }

    override suspend fun verifyPin(pin: String): VerifyPinResult = withContext(Dispatchers.Default) {
        requireValidPin(pin)
        var result: VerifyPinResult = VerifyPinResult.Wrong
        // DataStore serializes this read/verify/write, including concurrent Activity instances.
        // Access is granted only after the attempt/reset has durably committed.
        preferences.edit { saved ->
            val record = saved.validatedRecord()
                ?: throw IOException("Parent PIN is not configured")
            val waitMillis = saved.retryAfterMillis()
            result = when {
                waitMillis > 0 -> VerifyPinResult.Throttled(waitMillis)
                hasher.verify(pin, record) -> {
                    saved[FAILED_ATTEMPTS] = 0
                    saved[BLOCKED_UNTIL] = 0
                    VerifyPinResult.Accepted
                }
                else -> {
                    val failures = (saved[FAILED_ATTEMPTS] ?: 0) + 1
                    if (failures >= MAX_ATTEMPTS) {
                        saved[FAILED_ATTEMPTS] = 0
                        saved[BLOCKED_UNTIL] = nowMillis() + RETRY_DELAY_MILLIS
                        VerifyPinResult.Throttled(RETRY_DELAY_MILLIS)
                    } else {
                        saved[FAILED_ATTEMPTS] = failures
                        saved[BLOCKED_UNTIL] = 0
                        VerifyPinResult.Wrong
                    }
                }
            }
        }
        result
    }

    private fun Preferences.validatedRecord(): String? {
        val record = this[PIN_RECORD]
        if (record == null) {
            if (contains(FAILED_ATTEMPTS) || contains(BLOCKED_UNTIL)) {
                throw IOException("Incomplete parent PIN preferences")
            }
            return null
        }
        hasher.validate(record)
        val failures = this[FAILED_ATTEMPTS] ?: throw IOException("Missing parent PIN attempt count")
        val blockedUntil = this[BLOCKED_UNTIL] ?: throw IOException("Missing parent PIN retry deadline")
        if (failures !in 0 until MAX_ATTEMPTS || blockedUntil < 0) {
            throw IOException("Invalid parent PIN retry state")
        }
        return record
    }

    private fun Preferences.retryAfterMillis(): Long =
        ((this[BLOCKED_UNTIL] ?: 0L) - nowMillis()).coerceIn(0L, RETRY_DELAY_MILLIS)

    private companion object {
        val PIN_RECORD = stringPreferencesKey("pin_record")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val BLOCKED_UNTIL = longPreferencesKey("blocked_until")
        const val MAX_ATTEMPTS = 5
        const val RETRY_DELAY_MILLIS = 30_000L
    }
}

/** Copies the existing salted verifier once. A malformed legacy record never enables setup. */
internal class LegacyPinMigration(private val legacyFile: File) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        !currentData.contains(stringPreferencesKey("pin_record")) && legacyFile.exists()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return try {
            val legacy = PreferenceDataStoreFactory.create(scope = scope, produceFile = { legacyFile })
            val record = legacy.data.first()[stringPreferencesKey("pin_record")]
                ?: throw IOException("Missing legacy PIN record")
            PinHasher().validate(record)
            currentData.toMutablePreferences().apply {
                this[stringPreferencesKey("pin_record")] = record
                this[intPreferencesKey("failed_attempts")] = 0
                this[longPreferencesKey("blocked_until")] = 0
            }
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            scope.coroutineContext[kotlinx.coroutines.Job]?.join()
        }
    }

    // The old file is retained for data preservation; the new record prevents another migration.
    override suspend fun cleanUp() = Unit
}
