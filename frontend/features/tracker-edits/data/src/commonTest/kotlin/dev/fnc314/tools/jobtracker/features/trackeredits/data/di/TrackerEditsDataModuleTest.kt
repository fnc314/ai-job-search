package dev.fnc314.tools.jobtracker.features.trackeredits.data.di

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseHolder
import dev.fnc314.tools.jobtracker.core.data.source.DataResult
import dev.fnc314.tools.jobtracker.core.data.source.LocalDataSource
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.core.data.source.RemoteRecord
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.LocalTrackerEditsDataSource
import dev.fnc314.tools.jobtracker.features.trackeredits.data.source.RemoteTrackerEditsDataSource
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository.TrackerEditsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class TrackerEditsDataModuleTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testTrackerEditsDataModule_resolvesDependenciesWithDatabaseHolder() {
        val testCoreDataModule = module {
            single { DatabaseHolder(null) }
            single<LocalDataSource> {
                object : LocalDataSource {
                    override fun getString(key: String): String? = null
                    override fun putString(key: String, value: String) {}
                    override fun remove(key: String) {}
                    override fun clear() {}
                }
            }
            single<RemoteDataSource> {
                object : RemoteDataSource {
                    override fun observeCollection(collectionPath: String): Flow<List<RemoteRecord>> = emptyFlow()
                    override suspend fun getCollection(collectionPath: String): List<RemoteRecord> = emptyList()
                    override suspend fun getDocument(collectionPath: String, documentId: String): RemoteRecord? = null
                    override suspend fun setDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult = DataResult.Success
                    override suspend fun updateDocument(collectionPath: String, documentId: String, record: RemoteRecord): DataResult = DataResult.Success
                }
            }
        }

        val app = startKoin {
            modules(testCoreDataModule, trackerEditsDataModule)
        }

        val localSource = app.koin.getOrNull<LocalTrackerEditsDataSource>()
        val remoteSource = app.koin.getOrNull<RemoteTrackerEditsDataSource>()
        val repository = app.koin.getOrNull<TrackerEditsRepository>()

        assertNotNull(localSource, "LocalTrackerEditsDataSource must resolve")
        assertNotNull(remoteSource, "RemoteTrackerEditsDataSource must resolve")
        assertNotNull(repository, "TrackerEditsRepository must resolve")
    }
}
