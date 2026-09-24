package ru.nksk.parentsapp.feature.pin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.nksk.parentsapp.feature.pin.security.PinHasher

/** Contract for the device-stored parent PIN; implementations must never return the PIN. */
interface PinRepository {
    suspend fun hasPin(): Boolean
    suspend fun savePin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
}

private val Context.pinDataStore by preferencesDataStore(name = "pin_store")

/**
 * Stores only a salted PBKDF2 hash of the PIN in Preferences DataStore; the
 * record embeds its iteration count so the cost can grow without migrations.
 */
@Singleton
class PinRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PinRepository {
    private companion object {
        val PIN_RECORD = stringPreferencesKey("pin_record")
        const val ITERATIONS = 210_000
        const val FORMAT_VERSION = 1
        const val SEPARATOR = "|"
    }

    override suspend fun hasPin(): Boolean =
        context.pinDataStore.data.first().contains(PIN_RECORD)

    override suspend fun savePin(pin: String) = withContext(Dispatchers.Default) {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin.toCharArray(), salt, ITERATIONS)
        val record = listOf(
            FORMAT_VERSION.toString(),
            ITERATIONS.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(hash),
        ).joinToString(SEPARATOR)
        context.pinDataStore.edit { preferences -> preferences[PIN_RECORD] = record }
        Unit
    }

    override suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.Default) {
        val record = context.pinDataStore.data.first()[PIN_RECORD] ?: return@withContext false
        val parts = record.split(SEPARATOR)
        if (parts.size != 4) return@withContext false
        val iterations = parts[1].toIntOrNull() ?: return@withContext false
        val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull()
            ?: return@withContext false
        val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull()
            ?: return@withContext false
        val actual = PinHasher.hash(pin.toCharArray(), salt, iterations)
        MessageDigest.isEqual(actual, expected)
    }
}
