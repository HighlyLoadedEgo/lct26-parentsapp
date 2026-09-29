package ru.nksk.parentsapp.feature.pin.data

import javax.inject.Inject
import javax.inject.Singleton
import ru.nksk.parentsapp.feature.pin.access.DataStorePinRepository
import ru.nksk.parentsapp.feature.pin.access.PinStatus
import ru.nksk.parentsapp.feature.pin.access.CreatePinResult
import ru.nksk.parentsapp.feature.pin.access.VerifyPinResult

/** Compatibility contract for restored legacy PIN routes. New access is gated outside navigation. */
interface PinRepository {
    suspend fun hasPin(): Boolean
    suspend fun savePin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
}

@Singleton
class PinRepositoryImpl @Inject constructor(private val repository: DataStorePinRepository) : PinRepository {
    override suspend fun hasPin() = repository.readStatus() is PinStatus.Configured
    override suspend fun savePin(pin: String) {
        check(repository.createPin(pin) == CreatePinResult.Created) { "PIN already configured" }
    }
    override suspend fun verifyPin(pin: String) = repository.verifyPin(pin) == VerifyPinResult.Accepted
}
