package ru.nksk.parentsapp.app.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import ru.nksk.parentsapp.feature.pin.navigation.PinLock
import ru.nksk.parentsapp.feature.pin.navigation.PinSetup
import ru.nksk.parentsapp.feature.questions.navigation.QuestionTopic
import ru.nksk.parentsapp.feature.quests.navigation.Quests
import ru.nksk.parentsapp.feature.report.navigation.PetIdInput
import ru.nksk.parentsapp.feature.report.navigation.Statistics
import ru.nksk.parentsapp.feature.scanner.navigation.Scanner

/** Saves stable route IDs for the Navigation 3 back stack. */
internal val ParentsNavigationSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Statistics::class, Statistics.serializer())
            subclass(QuestionTopic::class, QuestionTopic.serializer())
            subclass(Quests::class, Quests.serializer())
            subclass(PinSetup::class, PinSetup.serializer())
            subclass(PinLock::class, PinLock.serializer())
            subclass(PetIdInput::class, PetIdInput.serializer())
            subclass(Scanner::class, Scanner.serializer())
        }
    }
}
