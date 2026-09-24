package ru.nksk.parentsapp.feature.pin.navigation

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("pin_setup")
data object PinSetup : NavKey

@Serializable
@SerialName("pin_lock")
data object PinLock : NavKey

fun EntryProviderScope<NavKey>.pinSetupEntry(onDone: (PinSetup) -> Unit) {
    entry<PinSetup> { source ->
        PinSetupEntry(onDone = dropUnlessResumed { onDone(source) })
    }
}

fun EntryProviderScope<NavKey>.pinLockEntry(onUnlocked: (PinLock) -> Unit) {
    entry<PinLock> { source ->
        PinLockEntry(onUnlocked = dropUnlessResumed { onUnlocked(source) })
    }
}
