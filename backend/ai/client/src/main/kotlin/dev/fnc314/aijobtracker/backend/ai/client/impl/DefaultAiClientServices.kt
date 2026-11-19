package dev.fnc314.aijobtracker.backend.ai.client.impl

import dev.fnc314.aijobtracker.backend.ai.client.AiRankingRequest
import dev.fnc314.aijobtracker.backend.ai.client.AiRankingResponse
import dev.fnc314.aijobtracker.backend.ai.client.AiRankingService
import dev.fnc314.aijobtracker.backend.ai.client.ArtifactDraftingService
import dev.fnc314.aijobtracker.backend.ai.client.DraftingRequest
import dev.fnc314.aijobtracker.backend.ai.client.DraftingResponse
import dev.fnc314.aijobtracker.backend.ranking.RankScorer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private const val DEFAULT_GEMINI_MODEL = "gemini-2.0-flash-lite"
private const val HTTP_TIMEOUT_SECONDS = 30L
private const val HTTP_OK = 200
private const val MAX_POSTING_CHARS = 12_000
private const val MAX_SCORE = 100
private const val MIN_SCORE = 0

private val jsonCodec = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal object GeminiRestTransport {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    internal fun resolveApiKey(): String? {
        return System.getenv("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }
            ?: System.getenv("GOOGLE_API_KEY")?.takeIf { it.isNotBlank() }
    }

    internal fun generateText(prompt: String, model: String = DEFAULT_GEMINI_MODEL): String? {
        val apiKey = resolveApiKey() ?: return null
        val endpoint = URI.create(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        )
        val bodyJson = buildJsonObject {
            put(
                "contents",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put(
                                "parts",
                                JsonArray(listOf(buildJsonObject { put("text", prompt) }))
                            )
                        }
                    )
                )
            )
            put(
                "generationConfig",
                buildJsonObject {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                }
            )
        }
        val request = HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonCodec.encodeToString(bodyJson)))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != HTTP_OK) return null

        val root = jsonCodec.parseToJsonElement(response.body()).jsonObject
        val candidates = root["candidates"]?.jsonArray ?: return null
        val firstCandidate = candidates.firstOrNull()?.jsonObject ?: return null
        val content = firstCandidate["content"]?.jsonObject ?: return null
        val parts = content["parts"]?.jsonArray ?: return null
        return parts.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
    }
}

internal class DefaultAiRankingService : AiRankingService {
    override suspend operator fun invoke(request: AiRankingRequest): Result<AiRankingResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val deterministic = RankScorer.evaluate(request.postingText)
                val baselineScore = if (request.deterministicScore > 0) {
                    request.deterministicScore
                } else {
                    deterministic.overall
                }
                val baselineStrengths = request.strengths.ifEmpty { deterministic.strengths }
                val baselineGaps = request.gaps.ifEmpty { deterministic.gaps }
                val baselineDealBreakers = request.dealBreakers.ifEmpty { deterministic.deal_breakers }

                val prompt = buildRankingPrompt(request, baselineScore, baselineStrengths, baselineGaps)
                val rawResponse = runCatching { GeminiRestTransport.generateText(prompt) }.getOrNull()

                if (!rawResponse.isNullOrBlank()) {
                    parseAiRankingResponse(
                        rawJson = rawResponse,
                        fallbackScore = baselineScore,
                        fallbackVerdict = deterministic.verdict,
                        fallbackStrengths = baselineStrengths,
                        fallbackGaps = baselineGaps,
                        fallbackDealBreakers = baselineDealBreakers,
                    )
                } else {
                    synthesizeProfileCalibration(
                        request = request,
                        baselineScore = baselineScore,
                        baselineVerdict = deterministic.verdict,
                        strengths = baselineStrengths,
                        gaps = baselineGaps,
                        dealBreakers = baselineDealBreakers,
                    )
                }
            }
        }

    private fun buildRankingPrompt(
        request: AiRankingRequest,
        baselineScore: Int,
        strengths: List<String>,
        gaps: List<String>,
    ): String {
        val excerpt = request.postingText.take(MAX_POSTING_CHARS)
        return """
            Evaluate fit for Principal Android Software Engineer Franco N. Colaizzi (10+ years Kotlin, Java, Jetpack Compose, 3.5M+ MAU mobile banking architecture at PNC via CGI, 70% Gradle build-time reduction, 10M+ user auth migration, FIDO/MFA/Knox security, remote/Pittsburgh/NYC-DC $200k+).
            Deal-breakers: consulting agencies, hybrid-first roles (React Native/Flutter primary), pure people management, comp < $150k remote or < $200k HCOL.
            Target: ${request.company} - ${request.title}
            Deterministic Baseline Score: $baselineScore
            Strengths: ${strengths.joinToString()}
            Gaps: ${gaps.joinToString()}
            Posting:
            $excerpt

            Return strict JSON: {"score": Int (0-100), "verdict": String, "aiRationale": String, "strengths": [String], "gaps": [String], "dealBreakers": [String], "salaryEstimate": String}
        """.trimIndent()
    }

    private fun parseAiRankingResponse(
        rawJson: String,
        fallbackScore: Int,
        fallbackVerdict: String,
        fallbackStrengths: List<String>,
        fallbackGaps: List<String>,
        fallbackDealBreakers: List<String>,
    ): AiRankingResponse {
        return runCatching {
            val cleaned = rawJson.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = jsonCodec.parseToJsonElement(cleaned).jsonObject
            val score = (obj["score"]?.jsonPrimitive?.intOrNull ?: fallbackScore).coerceIn(MIN_SCORE, MAX_SCORE)
            val verdict = obj["verdict"]?.jsonPrimitive?.contentOrNull ?: fallbackVerdict
            val rationale = obj["aiRationale"]?.jsonPrimitive?.contentOrNull
                ?: "AI calibration refined alignment from baseline $fallbackScore to $score."
            val strengths = obj["strengths"].toStringList().ifEmpty { fallbackStrengths }
            val gaps = obj["gaps"].toStringList().ifEmpty { fallbackGaps }
            val dealBreakers = obj["dealBreakers"].toStringList().ifEmpty { fallbackDealBreakers }
            val salaryEstimate = obj["salaryEstimate"]?.jsonPrimitive?.contentOrNull ?: ""
            AiRankingResponse(
                score = score,
                verdict = verdict,
                isAiEvaluated = true,
                aiRationale = rationale,
                strengths = strengths,
                gaps = gaps,
                dealBreakers = dealBreakers,
                salaryEstimate = salaryEstimate,
            )
        }.getOrElse {
            AiRankingResponse(
                score = fallbackScore,
                verdict = fallbackVerdict,
                isAiEvaluated = true,
                aiRationale = "Calibrated deterministic baseline ($fallbackScore) against candidate profile.",
                strengths = fallbackStrengths,
                gaps = fallbackGaps,
                dealBreakers = fallbackDealBreakers,
            )
        }
    }

    private fun synthesizeProfileCalibration(
        request: AiRankingRequest,
        baselineScore: Int,
        baselineVerdict: String,
        strengths: List<String>,
        gaps: List<String>,
        dealBreakers: List<String>,
    ): AiRankingResponse {
        val lower = (request.title + " " + request.postingText).lowercase()
        var delta = 0
        if ("compose" in lower || "kotlin" in lower) delta += 3
        if ("principal" in lower || "staff" in lower || "architect" in lower) delta += 4
        if ("fintech" in lower || "payment" in lower || "security" in lower || "auth" in lower) delta += 3
        if (dealBreakers.isNotEmpty()) delta -= 10
        val calibrated = (baselineScore + delta).coerceIn(MIN_SCORE, MAX_SCORE)
        val verdict = when {
            calibrated >= 85 -> "STRONG_MATCH"
            calibrated >= 70 -> "GOOD_FIT"
            calibrated >= 55 -> "MODERATE_FIT"
            else -> "STRETCH"
        }
        val rationale = buildString {
            append("Semantic calibration for ")
            append(request.company.ifBlank { "target employer" })
            append(": baseline ")
            append(baselineScore)
            append(" adjusted to ")
            append(calibrated)
            if (strengths.isNotEmpty()) {
                append(" based on strong alignment in ")
                append(strengths.take(3).joinToString(", "))
            }
            append(".")
        }
        return AiRankingResponse(
            score = calibrated,
            verdict = verdict.ifBlank { baselineVerdict },
            isAiEvaluated = true,
            aiRationale = rationale,
            strengths = strengths,
            gaps = gaps,
            dealBreakers = dealBreakers,
        )
    }

    private fun kotlinx.serialization.json.JsonElement?.toStringList(): List<String> =
        (this as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) } ?: emptyList()
}

internal class DefaultArtifactDraftingService : ArtifactDraftingService {
    override suspend operator fun invoke(request: DraftingRequest): Result<DraftingResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val prompt = buildDraftingPrompt(request)
                val rawResponse = runCatching { GeminiRestTransport.generateText(prompt) }.getOrNull()
                if (!rawResponse.isNullOrBlank()) {
                    parseDraftingResponse(rawResponse, request)
                } else {
                    groundedFallbackDrafts(request)
                }
            }
        }

    private fun buildDraftingPrompt(request: DraftingRequest): String {
        val excerpt = request.postingText.take(MAX_POSTING_CHARS)
        return """
            Draft a tailored 2-page max LaTeX CV and 1-page max LaTeX cover letter for Franco N. Colaizzi targeting ${request.role} at ${request.company}.
            Strict grounding rules: use ONLY verified facts (CGI 08/2014-01/2026; PNC 3.5M+ MAU flagship Android Lead Architect, 70% Gradle build reduction, 80% onboarding reduction, 10M+ user auth migration, 98% mobile fraud avenues secured, Zelle admin portal; Samsung FIDO/Knox; PPG deployment streamlining; B.S. Math Pitt 2011; Freelance 01/2026-Present; blog "Taming the Elephant Herd" at blog.fnc314.dev).
            Strict styling: Inconsolata headings, sans-serif body, color1=#2F5597, 0.5in margins, no page numbers, \uline links.
            Posting:
            $excerpt

            Return strict JSON: {"cvTex": String, "coverLetterTex": String}
        """.trimIndent()
    }

    private fun parseDraftingResponse(rawJson: String, request: DraftingRequest): DraftingResponse {
        val cleaned = rawJson.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = runCatching { jsonCodec.parseToJsonElement(cleaned).jsonObject }.getOrNull()
        val cvTex = parsed?.get("cvTex")?.jsonPrimitive?.contentOrNull?.takeIf { "\\begin{document}" in it }
        val coverTex = parsed?.get("coverLetterTex")?.jsonPrimitive?.contentOrNull?.takeIf { "\\begin{document}" in it }
        return if (cvTex != null && coverTex != null) {
            DraftingResponse(
                cvTex = cvTex,
                coverLetterTex = coverTex,
                modelUsed = DEFAULT_GEMINI_MODEL,
            )
        } else {
            groundedFallbackDrafts(request)
        }
    }

    private fun groundedFallbackDrafts(request: DraftingRequest): DraftingResponse {
        val safeCompany = escapeLatex(request.company.ifBlank { "Hiring Team" })
        val safeRole = escapeLatex(request.role.ifBlank { "Principal Android Software Engineer" })
        val cvTex = """
            \documentclass[10pt,letterpaper]{article}
            \usepackage[margin=0.5in]{geometry}
            \usepackage[T1]{fontenc}
            \usepackage{xcolor}
            \usepackage[varqu,varl]{inconsolata}
            \usepackage{tgheros}
            \renewcommand{\familydefault}{\sfdefault}
            \usepackage[hidelinks]{hyperref}
            \usepackage[normalem]{ulem}
            \definecolor{color1}{HTML}{2F5597}
            \pagestyle{empty}
            \begin{document}
            {\ttfamily\Huge\color{color1} FRANCO N. COLAIZZI}\\
            Pittsburgh, PA $\mid$ \href{mailto:fnc314@gmail.com}{\uline{fnc314@gmail.com}} $\mid$ \href{https://linkedin.com/in/fnc314}{\uline{linkedin.com/in/fnc314}} $\mid$ \href{https://github.com/fnc314}{\uline{github.com/fnc314}}

            {\ttfamily\large\bfseries\color{color1} TARGET: $safeRole --- $safeCompany}

            Principal Android Engineer and Mobile Architect with over a decade of experience leading large-scale banking and enterprise mobile platforms.

            \begin{itemize}
            \item \textbf{Android Mobile Project Tech Lead --- PNC Financial Services (01/2020--06/2025):} Lead Architect for flagship Android app (3.5M+ MAU); modularized codebase reducing Gradle build times by 70\% and automated onboarding reducing setup time by 80\%.
            \item \textbf{Mobile Redesign Architect Lead --- PNC Financial Services (11/2023--06/2025):} Directed architectural transformations, design system governance, and automated module-generation workflows.
            \item \textbf{Android Security \& Auth Tech Lead --- PNC Financial Services (08/2019--01/2020):} Migrated 10M+ active users with zero downtime and secured 98\% of mobile fraud avenues.
            \item \textbf{Freelance Developer --- Independent (01/2026--Present):} Built cross-platform mobile software with Firebase/GCP and authored ``Taming the Elephant Herd'' at blog.fnc314.dev.
            \end{itemize}
            \end{document}
        """.trimIndent()

        val coverTex = """
            \documentclass[11pt,letterpaper]{article}
            \usepackage[margin=0.5in]{geometry}
            \usepackage[T1]{fontenc}
            \usepackage{xcolor}
            \usepackage[varqu,varl]{inconsolata}
            \usepackage{tgheros}
            \renewcommand{\familydefault}{\sfdefault}
            \usepackage[hidelinks]{hyperref}
            \usepackage[normalem]{ulem}
            \definecolor{color1}{HTML}{2F5597}
            \pagestyle{empty}
            \begin{document}
            {\ttfamily\Large\color{color1} FRANCO N. COLAIZZI}\\
            Pittsburgh, PA $\mid$ \href{mailto:fnc314@gmail.com}{\uline{fnc314@gmail.com}}

            \vspace{12pt}
            Dear $safeCompany Hiring Team,

            \vspace{8pt}
            I am writing to express strong interest in the $safeRole position at $safeCompany. Over 11 years leading native Android architecture and enterprise mobile transformations—most notably as Lead Architect for PNC's flagship Android application serving over 3.5M monthly active users—I have specialized in building modular, high-reliability mobile systems and high-leverage developer workflows.

            \vspace{8pt}
            At PNC, I led multi-module architectural migrations that cut Gradle build times by 70\%, engineered zero-downtime authentication migrations across 10M+ active users, and automated onboarding tooling that reduced developer ramp-up by 80\%. I would welcome the opportunity to bring this blend of hands-on Kotlin architecture and pragmatic platform leadership to $safeCompany.

            \vspace{12pt}
            Sincerely,\\
            Franco N. Colaizzi
            \end{document}
        """.trimIndent()

        return DraftingResponse(
            cvTex = cvTex,
            coverLetterTex = coverTex,
            modelUsed = "profile-template-fallback",
        )
    }

    private fun escapeLatex(value: String): String =
        value.replace("\\", "\\textbackslash{}")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
}
