package ru.nksk.parentsapp.feature.pin.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.nksk.parentsapp.feature.pin.data.PinRepository
import ru.nksk.parentsapp.feature.pin.data.PinRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PinRepositoryModule {
    @Binds
    @Singleton
    internal abstract fun bindPinRepository(impl: PinRepositoryImpl): PinRepository
    @Binds
    @Singleton
    internal abstract fun bindAccessRepository(impl: ru.nksk.parentsapp.feature.pin.access.DataStorePinRepository):
        ru.nksk.parentsapp.feature.pin.access.PinRepository
}
