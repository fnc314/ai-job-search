package dev.fnc314.aijobtracker.backend.ai.client

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class AiClientServicesTest {
    @Test
    fun `ai ranking service refines baseline score`(): Unit = runBlocking {
        val service = AiClientFactory.createAiRankingService()
        val result = service(
            AiRankingRequest(
                postingText = "Principal Android Architect with Kotlin, Jetpack Compose, modular Gradle architecture, fintech payments.",
                company = "FintechCorp",
                title = "Principal Android Architect",
                deterministicScore = 82,
            )
        )
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response.isAiEvaluated)
        assertTrue(response.score in 0..100)
        assertTrue(response.aiRationale.isNotBlank())
    }

    @Test
    fun `artifact drafting service generates grounded tex sources`(): Unit = runBlocking {
        val service = AiClientFactory.createArtifactDraftingService()
        val result = service(
            DraftingRequest(
                company = "Stripe",
                role = "Staff Android Engineer",
                postingText = "Build reliable mobile payment architecture using Kotlin and Jetpack Compose.",
            )
        )
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response.cvTex.contains("FRANCO N. COLAIZZI"))
        assertTrue(response.coverLetterTex.contains("Stripe"))
        assertTrue(response.coverLetterTex.contains("Staff Android Engineer"))
    }
}
