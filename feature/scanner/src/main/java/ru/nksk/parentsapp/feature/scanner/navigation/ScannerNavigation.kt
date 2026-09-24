package ru.nksk.parentsapp.feature.scanner.navigation

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("scanner")
data object Scanner : NavKey

fun EntryProviderScope<NavKey>.scannerEntry(
    onScanned: (Scanner, String) -> Unit,
    onBack: (Scanner) -> Unit,
) {
    entry<Scanner> { source ->
        ScannerEntry(
            onScanned = { value -> onScanned(source, value) },
            onBack = dropUnlessResumed { onBack(source) },
        )
    }
}
