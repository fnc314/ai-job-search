package dev.fnc314.aijobtracker.frontend.features.aiapply.data.di

import dev.fnc314.aijobtracker.frontend.core.data.database.DatabaseHolder
import dev.fnc314.aijobtracker.frontend.core.data.source.DataResult
import dev.fnc314.aijobtracker.frontend.core.data.source.LocalDataSource
import dev.fnc314.aijobtracker.frontend.core.data.source.RemoteDataSource
import dev.fnc314.aijobtracker.frontend.core.data.source.RemoteRecord
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.LocalApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.NetworkApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.RemoteApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.impl.CanonicalArtifactHelper
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.di.aiApplyDomainModule
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.repository.ApplyAssistantRepository
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.EvaluateJobPostingUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class AiApplyDataModuleTest {

    @AfterTest
    fun tearDown(): Unit {
        stopKoin()
    }

    @Test
    fun testAiApplyDataModule_resolvesDependenciesWithDatabaseHolder(): Unit {
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
            modules(testCoreDataModule, aiApplyDataModule, aiApplyDomainModule)
        }

        assertNotNull(app.koin.getOrNull<LocalApplyAssistantDataSource>(), "LocalApplyAssistantDataSource must resolve")
        assertNotNull(app.koin.getOrNull<NetworkApplyAssistantDataSource>(), "NetworkApplyAssistantDataSource must resolve")
        assertNotNull(app.koin.getOrNull<RemoteApplyAssistantDataSource>(), "RemoteApplyAssistantDataSource must resolve")
        assertNotNull(app.koin.getOrNull<ApplyAssistantRepository>(), "ApplyAssistantRepository must resolve")
        assertNotNull(app.koin.getOrNull<EvaluateJobPostingUseCase>(), "EvaluateJobPostingUseCase must resolve")
    }

    @Test
    fun canonicalArtifactHelper_ProducesCanonicalSlugsDocumentIdAndPaths(): Unit {
        assertEquals("pnc", CanonicalArtifactHelper.slug("PNC Financial Services", "company"))
        assertEquals("ford", CanonicalArtifactHelper.slug("Ford Motor Company", "company"))
        assertEquals("bjak--senior-android-software-engineer", CanonicalArtifactHelper.documentId("BJAK", "Sr. Android Software Engineer"))

        val spec = CanonicalArtifactHelper.forJob(
            company = "BJAK",
            role = "Android Software Engineer",
            applicationId = "bjak--android-software-engineer",
            dateDir = "2026-11-18",
        )
        assertEquals("cv_bjak_android_software_engineer", spec.cvArtifactId)
        assertEquals("main_bjak_android_software_engineer.tex", spec.cvFilename)
        assertEquals("job-applications/bjak/android-software-engineer/2026-11-18/main_bjak_android_software_engineer.tex", spec.cvPath)
        assertEquals("cover_bjak_android_software_engineer", spec.coverArtifactId)
        assertEquals("cover_bjak_android_software_engineer.tex", spec.coverFilename)
        assertEquals("job-applications/bjak/android-software-engineer/2026-11-18/cover_bjak_android_software_engineer.tex", spec.coverPath)
        assertEquals("outcome_bjak_android_software_engineer", spec.outcomeArtifactId)
        assertEquals("outcome.md", spec.outcomeFilename)
        assertEquals("job-applications/bjak/android-software-engineer/2026-11-18/outcome.md", spec.outcomePath)
    }

    @Test
    fun canonicalArtifactHelper_ParsesRemotePostingTextAndRejectsLegacyStub(): Unit {
        val legacyStub = buildJsonObject {
            put("status", "SUCCESS")
            put("fitScore", 85)
        }
        assertNull(CanonicalArtifactHelper.parseRemoteDeterministic(legacyStub, null, null, null))

        val validRemote = buildJsonObject {
            put("applicationId", "amazon--software-development-engineer")
            put("company", "Amazon")
            put("role", "Software Development Engineer")
            put("score", 82)
            put("verdict", "GOOD_FIT")
            put("postingText", "Extracted backend description for Amazon SDE role.")
        }
        val parsed = assertNotNull(CanonicalArtifactHelper.parseRemoteDeterministic(validRemote, null, null, null))
        assertEquals("Amazon", parsed.company)
        assertEquals("Software Development Engineer", parsed.role)
        assertEquals(82, parsed.score)
        assertEquals("Extracted backend description for Amazon SDE role.", parsed.extractedText)
    }
}
