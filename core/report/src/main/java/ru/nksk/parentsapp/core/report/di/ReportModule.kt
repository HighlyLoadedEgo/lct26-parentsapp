package ru.nksk.parentsapp.core.report.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ru.nksk.parentsapp.core.report.data.ParentReportApi
import ru.nksk.parentsapp.core.report.data.ParentReportRepository
import ru.nksk.parentsapp.core.report.data.ParentReportRepositoryImpl
import ru.nksk.parentsapp.core.report.data.PetSessionStore
import ru.nksk.parentsapp.core.report.data.PetSessionStoreImpl
import ru.nksk.parentsapp.core.report.rewards.ParentQuestRewardsApi
import ru.nksk.parentsapp.core.report.rewards.ParentQuestRewardsRepository
import ru.nksk.parentsapp.core.report.rewards.RemoteParentQuestRewardsRepository

@Module
@InstallIn(SingletonComponent::class)
internal object ReportNetworkModule {
    private const val BASE_URL = "https://fin-api.mortypython.ru/"
    private const val TIMEOUT_SECONDS = 15L

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideParentQuestRewardsApi(retrofit: Retrofit): ParentQuestRewardsApi =
        retrofit.create(ParentQuestRewardsApi::class.java)

    @Provides
    @Singleton
    fun provideParentReportApi(retrofit: Retrofit): ParentReportApi =
        retrofit.create(ParentReportApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ReportRepositoryModule {
    @Binds
    @Singleton
    internal abstract fun bindParentQuestRewardsRepository(impl: RemoteParentQuestRewardsRepository): ParentQuestRewardsRepository

    @Binds
    @Singleton
    internal abstract fun bindParentReportRepository(impl: ParentReportRepositoryImpl): ParentReportRepository

    @Binds
    @Singleton
    internal abstract fun bindPetSessionStore(impl: PetSessionStoreImpl): PetSessionStore
}
