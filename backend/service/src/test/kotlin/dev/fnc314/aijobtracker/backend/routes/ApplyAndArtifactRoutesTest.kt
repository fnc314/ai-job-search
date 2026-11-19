package dev.fnc314.aijobtracker.backend.routes

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplyAndArtifactRoutesTest {
    private val jsonCodec = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST applications apply returns deterministic RankScorer score`(): Unit = testApplication {
        application {
            routing {
                applicationRoutes()
            }
        }
        val response = client.post("/applications/apply") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"company":"Walmart","role":"Principal Android Engineer","text":"Principal Android Engineer using Kotlin, Jetpack Compose, modular Gradle architecture, mobile security, remote.", "persist": false}"""
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = jsonCodec.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("Walmart", json["company"]?.jsonPrimitive?.content)
        assertFalse(json["isAiEvaluated"]?.jsonPrimitive?.boolean ?: true)
        assertTrue((json["score"]?.jsonPrimitive?.int ?: -1) in 0..100)
    }

    @Test
    fun `POST ai rank returns AI evaluated score and rationale`(): Unit = testApplication {
        application {
            routing {
                rankingRoutes()
            }
        }
        val response = client.post("/ai/rank") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"company":"Walmart","title":"Principal Android Engineer","postingText":"Kotlin Jetpack Compose architecture","deterministicScore":78}"""
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = jsonCodec.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(json["isAiEvaluated"]?.jsonPrimitive?.boolean == true)
        assertTrue((json["aiRationale"]?.jsonPrimitive?.content ?: "").isNotBlank())
    }

    @Test
    fun `POST artifacts create returns 202 and GET artifacts jobs returns snapshot`(): Unit = testApplication {
        val originalRoot = ApplyPipelineCoordinator.artifactsRootDir
        val tempRoot = kotlin.io.path.createTempDirectory("artifact-route-test").toFile()
        ApplyPipelineCoordinator.artifactsRootDir = tempRoot
        try {
            application {
                routing {
                    artifactRoutes()
                }
            }
            val createResp = client.post("/artifacts/create") {
                contentType(ContentType.Application.Json)
                setBody(
                    """{"applicationId":"walmart-principal-android-2026-11-18","company":"Walmart","role":"Principal Android Engineer","postingText":"Kotlin Jetpack Compose","persist":false}"""
                )
            }
            assertEquals(HttpStatusCode.Accepted, createResp.status)
            val acceptedJson = jsonCodec.parseToJsonElement(createResp.bodyAsText()).jsonObject
            val jobId = acceptedJson["jobId"]?.jsonPrimitive?.content ?: ""
            assertTrue(jobId.startsWith("job_art_"))
            assertEquals("DRAFTING", acceptedJson["status"]?.jsonPrimitive?.content)

            val statusResp = client.get("/artifacts/jobs/$jobId")
            assertEquals(HttpStatusCode.OK, statusResp.status)
        } finally {
            ApplyPipelineCoordinator.artifactsRootDir = originalRoot
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `ApplyPipelineCoordinator resolves LinkedIn guest URL and parses LinkedIn HTML`() {
        assertEquals(
            "https://www.linkedin.com/jobs-guest/jobs/api/jobPosting/4491327366",
            ApplyPipelineCoordinator.resolveFetchUrl("https://www.linkedin.com/jobs/view/4491327366/"),
        )
        val guestHtml = """
            <html>
              <body>
                <h2 class="top-card-layout__title font-sans">Principal Android Software Engineer</h2>
                <a class="topcard__org-name-link topcard__flavor--black-link" href="https://linkedin.com/company/stripe?trk=public">Stripe</a>
                <span class="topcard__flavor topcard__flavor--bullet">Remote, United States</span>
                <div class="show-more-less-html__markup">Build Kotlin and Jetpack Compose mobile payments architecture with modular Gradle.</div>
              </body>
            </html>
        """.trimIndent()
        val parsed = ApplyPipelineCoordinator.parsePostingHtml(guestHtml, "https://www.linkedin.com/jobs/view/4491327366/")
        assertEquals("Stripe", parsed.company)
        assertEquals("Principal Android Software Engineer", parsed.role)
        assertEquals("Remote, United States", parsed.location)
        assertEquals("https://linkedin.com/company/stripe", parsed.companyLinkedinUrl)
        assertTrue(parsed.postingText.contains("Jetpack Compose"))
    }

    @Test
    fun `ApplyPipelineCoordinator parses non-LinkedIn Amazon Jobs HTML and SPA fallback`() {
        val url = "https://www.amazon.jobs/en/jobs/10594512/software-development-engineer-app-performance-mobile-app-foundations"
        val amazonHtml = """
            <html>
              <head>
                <title>Software Development Engineer - App Performance, Mobile App Foundations - Job ID: 10594512 | Amazon.jobs</title>
                <meta property="og:site_name" content="amazon.jobs" />
              </head>
              <body>
                <h1>Software Development Engineer - App Performance, Mobile App Foundations</h1>
                <div class="description__text">Work on Android Kotlin performance, startup optimization, and mobile foundations.</div>
              </body>
            </html>
        """.trimIndent()
        val parsedHtml = ApplyPipelineCoordinator.parsePostingHtml(amazonHtml, url)
        assertEquals("Amazon", parsedHtml.company)
        assertEquals("Software Development Engineer - App Performance, Mobile App Foundations", parsedHtml.role)
        assertEquals("https://amazon.com", parsedHtml.companyWebsite)
        assertTrue(parsedHtml.postingText.contains("Android Kotlin performance"))

        val parsedSpaFallback = ApplyPipelineCoordinator.parsePostingHtml("", url)
        assertEquals("Amazon", parsedSpaFallback.company)
        assertEquals(
            "Software Development Engineer App Performance Mobile App Foundations",
            parsedSpaFallback.role,
        )
        assertEquals("https://amazon.com", parsedSpaFallback.companyWebsite)

        val greenhouseParsed = ApplyPipelineCoordinator.parsePostingHtml(
            "<html><head><title>Staff Android Engineer</title></head></html>",
            "https://boards.greenhouse.io/affirm/jobs/1234567",
        )
        assertEquals("Affirm", greenhouseParsed.company)
        assertEquals("Staff Android Engineer", greenhouseParsed.role)
    }
}
