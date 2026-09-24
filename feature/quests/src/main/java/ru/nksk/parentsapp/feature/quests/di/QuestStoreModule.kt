package ru.nksk.parentsapp.feature.quests.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.nksk.parentsapp.feature.quests.data.QuestStore
import ru.nksk.parentsapp.feature.quests.data.QuestStoreImpl

@Module
@InstallIn(SingletonComponent::class)
internal abstract class QuestStoreModule {
    @Binds
    @Singleton
    internal abstract fun bindQuestStore(impl: QuestStoreImpl): QuestStore
}
