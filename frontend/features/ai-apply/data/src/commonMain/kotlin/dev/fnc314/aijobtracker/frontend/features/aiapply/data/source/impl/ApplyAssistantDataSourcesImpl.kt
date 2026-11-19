package dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.impl

import dev.fnc314.aijobtracker.frontend.core.data.database.DatabaseHolder
import dev.fnc314.aijobtracker.frontend.core.data.database.entity.ApplyMessageEntity
import dev.fnc314.aijobtracker.frontend.core.data.database.entity.ApplySessionEntity
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.core.data.model.Uuid7
import dev.fnc314.aijobtracker.frontend.core.data.source.MapRemoteRecord
import dev.fnc314.aijobtracker.frontend.core.data.source.RemoteDataSource
import dev.fnc314.aijobtracker.frontend.core.network.RemoteNetworkConfig
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.LocalApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.NetworkAiRankingResult
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.NetworkApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.NetworkDeterministicResult
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.RemoteApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyArtifactJobState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyChatMessage
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyEvaluationResult
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyInputMode
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyMessageSender
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyMessageType
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplySessionState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyWorkflowStep
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Clock

private const val ISO_DATE_LENGTH = 10
private const val FALLBACK_BASE_SCORE = 65
private const val FALLBACK_STRENGTH_BONUS = 5
private const val FALLBACK_MIN_SCORE = 50
private const val FALLBACK_MAX_SCORE = 92
private const val STRONG_MATCH_THRESHOLD = 85
private const val AI_FALLBACK_DELTA = 4
private const val AI_FALLBACK_MAX_SCORE = 98
private const val ROLE_BONUS_PRINCIPAL = 15
private const val ROLE_BONUS_SENIOR = 10
private const val PLACEHOLDER_COMPANY = "Target Company"
private const val PLACEHOLDER_ROLE = "Software Engineer"
private const val DEFAULT_STATUS_RANK = 13

private val internalJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
private data class SerializedMessagePayload(
    val evaluationStatus: String? = null,
    val promptStep: String? = null,
    val artifactJobId: String? = null,
    val artifactJobStatus: String? = null,
    val cvArtifactId: String? = null,
    val cvPdfUrl: String? = null,
    val coverLetterArtifactId: String? = null,
    val coverLetterPdfUrl: String? = null,
    val outcomeArtifactId: String? = null,
    val outcomeUrl: String? = null,
    val error: String? = null,
) {
    fun toArtifactJob(fallbackAppId: String, sessionJob: ApplyArtifactJobState?): ApplyArtifactJobState? {
        val id = artifactJobId ?: return null
        return ApplyArtifactJobState(
            jobId = id,
            applicationId = sessionJob?.applicationId ?: fallbackAppId,
            status = artifactJobStatus ?: sessionJob?.status ?: "IN_PROGRESS",
            cvPdfUrl = cvPdfUrl ?: sessionJob?.cvPdfUrl,
            coverLetterPdfUrl = coverLetterPdfUrl ?: sessionJob?.coverLetterPdfUrl,
            error = error,
            cvArtifactId = cvArtifactId ?: sessionJob?.cvArtifactId,
            coverLetterArtifactId = coverLetterArtifactId ?: sessionJob?.coverLetterArtifactId,
            outcomeArtifactId = outcomeArtifactId ?: sessionJob?.outcomeArtifactId,
            outcomeUrl = outcomeUrl ?: sessionJob?.outcomeUrl,
        )
    }
}

internal data class CanonicalArtifactSpec(
    val applicationId: String,
    val companySlug: String,
    val roleSlug: String,
    val dateDir: String,
    val fileStem: String,
    val cvArtifactId: String,
    val cvFilename: String,
    val cvPath: String,
    val coverArtifactId: String,
    val coverFilename: String,
    val coverPath: String,
    val outcomeArtifactId: String,
    val outcomeFilename: String,
    val outcomePath: String,
)

internal object CanonicalArtifactHelper {
    private val contractions = mapOf(
        "sr" to "senior",
        "jr" to "junior",
        "asst" to "assistant",
        "assoc" to "associate",
    )
    private val stopWords = setOf("the", "a", "an", "of", "and")
    private val corpSuffixes = setOf("inc", "llc", "corp", "corporation", "ltd", "co", "company")
    private val companyAliases = mapOf(
        "meta-platforms" to "meta",
        "facebook" to "meta",
        "ford-motor-company" to "ford",
        "ford-motor" to "ford",
        "alphabet" to "google",
        "square" to "block",
        "pnc-financial-services" to "pnc",
        "pnc-bank" to "pnc",
        "jpmorgan" to "jpmorgan-chase",
        "jp-morgan" to "jpmorgan-chase",
        "jp-morgan-chase" to "jpmorgan-chase",
        "capitalone" to "capital-one",
        "edwardjones" to "edward-jones",
        "eightsleep" to "eight-sleep",
        "jpmorganchase" to "jpmorgan-chase",
        "homedepot" to "home-depot",
        "thompsonreuters" to "thomson-reuters",
        "thompson-reuters" to "thomson-reuters",
        "washpost" to "washington-post",
        "open-ai" to "openai",
        "yinz-cam" to "yinzcam",
        "on-x" to "onx",
        "spot-on" to "spoton",
    )

    fun currentIsoDate(): String = Clock.System.now().toString().take(ISO_DATE_LENGTH)

    fun slug(value: String, fallback: String): String {
        if (value.isBlank()) return fallback
        val stripped = value.trim().replace(Regex("(?<=\\w)\\s*\\([A-Za-z0-9]+\\)"), "")
        val rawTokens = stripped.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').split("-")
        val expanded = rawTokens.mapNotNull { token ->
            token.takeIf { it.isNotEmpty() }?.let { contractions[it] ?: it }
        }
        val filtered = expanded.filter { it !in stopWords && it !in corpSuffixes }.ifEmpty { expanded }
        val deduped = mutableListOf<String>()
        for (word in filtered) {
            if (deduped.lastOrNull() != word) deduped.add(word)
        }
        val joined = deduped.joinToString("-")
        return (companyAliases[joined] ?: joined).ifEmpty { fallback }
    }

    fun documentId(company: String, role: String?): String {
        val companySlug = slug(company, "unknown-company")
        val roleSlug = role?.let { slug(it, "") }.orEmpty()
        return if (roleSlug.isNotEmpty()) "$companySlug--$roleSlug" else companySlug
    }

    fun forJob(company: String, role: String, applicationId: String? = null, dateDir: String = currentIsoDate()): CanonicalArtifactSpec {
        val companySlug = slug(company, "target-company")
        val roleSlug = slug(role, "target-role")
        val fileStem = "${companySlug.replace('-', '_')}_${roleSlug.replace('-', '_')}"
        val dir = "job-applications/$companySlug/$roleSlug/$dateDir"
        val resolvedAppId = applicationId?.takeIf { it.isNotBlank() } ?: "$companySlug--$roleSlug"
        return CanonicalArtifactSpec(
            applicationId = resolvedAppId,
            companySlug = companySlug,
            roleSlug = roleSlug,
            dateDir = dateDir,
            fileStem = fileStem,
            cvArtifactId = "cv_$fileStem",
            cvFilename = "main_$fileStem.tex",
            cvPath = "$dir/main_$fileStem.tex",
            coverArtifactId = "cover_$fileStem",
            coverFilename = "cover_$fileStem.tex",
            coverPath = "$dir/cover_$fileStem.tex",
            outcomeArtifactId = "outcome_$fileStem",
            outcomeFilename = "outcome.md",
            outcomePath = "$dir/outcome.md",
        )
    }

    fun buildGroundedCvTex(company: String, role: String): String {
        val safeCompany = escapeLatex(company.ifBlank { "Hiring Team" })
        val safeRole = escapeLatex(role.ifBlank { "Principal Android Software Engineer" })
        return """
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

            Principal Android Engineer and Mobile Architect with 10+ years of experience leading large-scale banking and enterprise mobile platforms.

            \begin{itemize}
            \item \textbf{Freelance Developer --- Independent (01/2026--Present):} Built location-based dating software with React Native (Expo), Firebase, and Google Cloud Platform; authored ``Taming the Elephant Herd'' at \href{https://blog.fnc314.dev}{\uline{blog.fnc314.dev}}.
            \item \textbf{Android Mobile Project Tech Lead --- PNC Financial Services via CGI (01/2020--06/2025):} Lead Architect for flagship Android app (3.5M+ MAU); modularized codebase reducing Gradle build times by 70\% and automated onboarding reducing setup time by 80\%.
            \item \textbf{Mobile Redesign Architect Lead --- PNC Financial Services via CGI (11/2023--06/2025):} Directed architectural transformations, design system governance, and automated module-generation workflows.
            \item \textbf{Android Security \& Auth Tech Lead --- PNC Financial Services via CGI (08/2019--01/2020):} Migrated 10M+ active users with zero downtime and secured 98\% of mobile fraud avenues.
            \end{itemize}
            \end{document}
        """.trimIndent()
    }

    fun buildGroundedCoverTex(company: String, role: String): String {
        val safeCompany = escapeLatex(company.ifBlank { "Hiring Team" })
        val safeRole = escapeLatex(role.ifBlank { "Principal Android Software Engineer" })
        return """
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
            Dear Hiring Manager,

            \vspace{8pt}
            I am writing to express strong interest in the $safeRole position at $safeCompany. Across my tenure at CGI leading native Android architecture for enterprise clients—most notably serving as Lead Architect for PNC Financial Services' flagship Android application (3.5M+ MAU)—I have focused on building resilient, modular mobile systems and high-leverage developer workflows.

            \vspace{8pt}
            At PNC, I modularized a monolithic repository to reduce Gradle build times by 70\%, engineered a zero-downtime authentication platform migrating 10M+ active users while securing 98\% of mobile fraud avenues, and automated environment setup to cut developer onboarding time by 80\%. I would welcome the opportunity to bring this combination of hands-on Kotlin architecture and pragmatic platform leadership to $safeCompany.

            \vspace{12pt}
            Sincerely,\\
            Franco N. Colaizzi
            \end{document}
        """.trimIndent()
    }

    fun buildOutcomeMarkdown(
        company: String,
        role: String,
        jobUrl: String?,
        evaluation: ApplyEvaluationResult?,
    ): String {
        val date = currentIsoDate()
        val scoreLine = evaluation?.let { "${it.score} / 100 (${it.verdict})" } ?: "Pending"
        val strengths = evaluation?.strengths?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { "- $it" } ?: "- Native Android & Kotlin Architecture"
        val gaps = evaluation?.gaps?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { "- $it" } ?: "- None identified"
        val dealBreakers = evaluation?.dealBreakers?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") ?: "None"
        return buildString {
            appendLine("# Job Application: $company — $role")
            appendLine()
            appendLine("**Date:** $date")
            if (!jobUrl.isNullOrBlank()) appendLine("**URL:** $jobUrl")
            appendLine("**Fit Score:** $scoreLine")
            appendLine("**Deal-Breakers:** $dealBreakers")
            appendLine()
            if (!evaluation?.aiRationale.isNullOrBlank()) {
                appendLine("## Calibration Rationale")
                appendLine(evaluation.aiRationale)
                appendLine()
            }
            appendLine("## Strengths")
            appendLine(strengths)
            appendLine()
            appendLine("## Gaps")
            appendLine(gaps)
        }.trimEnd()
    }

    fun buildRestoredJobState(
        entity: ApplySessionEntity,
    ): ApplyArtifactJobState? {
        val jobId = entity.artifactJobId?.takeIf { it.isNotBlank() } ?: return null
        val resolvedStatus = entity.artifactJobStatus ?: "IN_PROGRESS"
        val isSuccess = resolvedStatus == "SUCCESS"
        val spec = forJob(entity.company, entity.role, entity.applicationId)
        val outcomeFallback = entity.cvPdfUrl?.substringBeforeLast('/')?.let { "$it/outcome.md" } ?: spec.outcomePath
        return ApplyArtifactJobState(
            jobId = jobId,
            applicationId = entity.applicationId.orEmpty(),
            status = resolvedStatus,
            cvPdfUrl = entity.cvPdfUrl,
            coverLetterPdfUrl = entity.coverLetterPdfUrl,
            cvArtifactId = if (isSuccess) spec.cvArtifactId else null,
            coverLetterArtifactId = if (isSuccess) spec.coverArtifactId else null,
            outcomeArtifactId = if (isSuccess) spec.outcomeArtifactId else null,
            outcomeUrl = if (isSuccess) outcomeFallback else null,
        )
    }

    fun parseRemoteDeterministic(
        obj: JsonObject,
        text: String?,
        companyOverride: String?,
        roleOverride: String?,
    ): NetworkDeterministicResult? {
        val remoteAppId = obj["applicationId"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
        val remoteScore = obj["score"]?.jsonPrimitive?.intOrNull ?: return null
        val resolvedCompany = companyOverride?.takeIf { it.isNotBlank() }
            ?: obj["company"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != PLACEHOLDER_COMPANY }
            ?: return null
        val resolvedRole = roleOverride?.takeIf { it.isNotBlank() }
            ?: obj["role"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != PLACEHOLDER_ROLE }
            ?: "Principal Android Engineer"
        val finalAppId = if (remoteAppId.startsWith("apply-") || PLACEHOLDER_COMPANY.lowercase() in remoteAppId) {
            documentId(resolvedCompany, resolvedRole)
        } else {
            remoteAppId
        }
        return NetworkDeterministicResult(
            applicationId = finalAppId,
            company = resolvedCompany,
            role = resolvedRole,
            score = remoteScore,
            verdict = obj["verdict"]?.jsonPrimitive?.contentOrNull ?: "GOOD_FIT",
            strengths = obj["strengths"]?.toStringList().orEmpty(),
            gaps = obj["gaps"]?.toStringList().orEmpty(),
            dealBreakers = obj["dealBreakers"]?.toStringList().orEmpty(),
            extractedText = obj["postingText"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: text.orEmpty(),
        )
    }

    fun buildDraftArtifactEntries(
        applicationId: String,
        company: String,
        role: String,
        evaluation: ApplyEvaluationResult?,
        artifactJob: ApplyArtifactJobState?,
        jobUrl: String?,
        nowIso: String,
    ): List<Pair<String, Map<String, Any?>>> {
        val spec = forJob(company, role, applicationId)
        val cvRemote = remotePdfUrl(artifactJob?.cvPdfUrl)
        val coverRemote = remotePdfUrl(artifactJob?.coverLetterPdfUrl)
        return listOf(
            artifactRecord(
                docId = artifactJob?.cvArtifactId ?: spec.cvArtifactId,
                applicationId = applicationId,
                kind = "artifact:cv",
                filename = if (cvRemote != null) spec.cvFilename.replace(".tex", ".pdf") else spec.cvFilename,
                contentType = if (cvRemote != null) "application/pdf" else "text/x-tex",
                localPath = spec.cvPath,
                externalUrl = cvRemote,
                content = if (cvRemote == null) buildGroundedCvTex(company, role) else null,
                nowIso = nowIso,
            ),
            artifactRecord(
                docId = artifactJob?.coverLetterArtifactId ?: spec.coverArtifactId,
                applicationId = applicationId,
                kind = "artifact:cover-letter",
                filename = if (coverRemote != null) spec.coverFilename.replace(".tex", ".pdf") else spec.coverFilename,
                contentType = if (coverRemote != null) "application/pdf" else "text/x-tex",
                localPath = spec.coverPath,
                externalUrl = coverRemote,
                content = if (coverRemote == null) buildGroundedCoverTex(company, role) else null,
                nowIso = nowIso,
            ),
            artifactRecord(
                docId = artifactJob?.outcomeArtifactId ?: spec.outcomeArtifactId,
                applicationId = applicationId,
                kind = "artifact:outcome",
                filename = spec.outcomeFilename,
                contentType = "text/markdown",
                localPath = spec.outcomePath,
                externalUrl = null,
                content = buildOutcomeMarkdown(company, role, jobUrl, evaluation),
                nowIso = nowIso,
            ),
        )
    }

    private fun remotePdfUrl(url: String?): String? = url?.takeIf {
        (it.startsWith("http://") || it.startsWith("https://")) && it.endsWith(".pdf", ignoreCase = true)
    }

    private fun artifactRecord(
        docId: String,
        applicationId: String,
        kind: String,
        filename: String,
        contentType: String,
        localPath: String,
        externalUrl: String?,
        content: String?,
        nowIso: String,
    ): Pair<String, Map<String, Any?>> {
        val map = mutableMapOf<String, Any?>(
            "artifactId" to docId,
            "applicationId" to applicationId,
            "kind" to kind,
            "filename" to filename,
            "contentType" to contentType,
            "localPath" to localPath,
            "createdAt" to nowIso,
        )
        if (!externalUrl.isNullOrBlank()) map["externalUrl"] = externalUrl
        if (!content.isNullOrBlank()) {
            map["content"] = content
            map["size"] = content.length.toLong()
        }
        return docId to map
    }

    private fun escapeLatex(value: String): String =
        value.replace("\\", "\\textbackslash{}")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
}

private fun kotlinx.serialization.json.JsonElement.toStringList(): List<String> =
    (this as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf(String::isNotBlank) }.orEmpty()

internal class LocalApplyAssistantDataSourceImpl(
    private val databaseHolder: DatabaseHolder? = null,
) : LocalApplyAssistantDataSource {

    private val memorySession = MutableStateFlow<ApplySessionState?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLatestSession(): Flow<ApplySessionState?> {
        val dao = databaseHolder?.database?.applyAssistantDao() ?: return memorySession
        return dao.observeLatestSession().flatMapLatest { sessionEntity ->
            if (sessionEntity == null) {
                flowOf(null)
            } else {
                combine(flowOf(sessionEntity), dao.observeMessages(sessionEntity.id)) { entity, messageEntities ->
                    entity.toDomain(messageEntities)
                }
            }
        }
    }

    override suspend fun getLatestSession(): ApplySessionState? {
        val dao = databaseHolder?.database?.applyAssistantDao() ?: return memorySession.value
        val session = dao.getLatestSession() ?: return null
        return session.toDomain(emptyList())
    }

    override suspend fun getSession(sessionId: String): ApplySessionState? {
        val memory = memorySession.value
        if (memory?.id == sessionId) return memory
        val dao = databaseHolder?.database?.applyAssistantDao() ?: return null
        return dao.getSession(sessionId)?.toDomain(emptyList())
    }

    override suspend fun saveSession(session: ApplySessionState) {
        memorySession.value = session
        val dao = databaseHolder?.database?.applyAssistantDao() ?: return
        dao.upsertSession(session.toEntity())
        session.messages.forEach { msg ->
            dao.upsertMessage(msg.toEntity(session.id))
        }
    }

    override suspend fun clearSessions() {
        memorySession.value = null
        val dao = databaseHolder?.database?.applyAssistantDao() ?: return
        dao.clearMessages()
        dao.clearSessions()
    }

    private fun ApplySessionState.toEntity(): ApplySessionEntity = ApplySessionEntity(
        id = id,
        applicationId = evaluation?.applicationId,
        postingUrl = postingUrl.ifBlank { null },
        postingText = postingText.ifBlank { null },
        company = company,
        role = role,
        deterministicScore = evaluation?.deterministicScore,
        aiScore = evaluation?.aiScore,
        verdict = evaluation?.verdict,
        aiRationale = evaluation?.aiRationale?.ifBlank { null },
        strengthsJson = evaluation?.strengths?.let { encodeStringList(it) },
        gapsJson = evaluation?.gaps?.let { encodeStringList(it) },
        dealBreakersJson = evaluation?.dealBreakers?.let { encodeStringList(it) },
        artifactJobId = artifactJob?.jobId,
        artifactJobStatus = artifactJob?.status,
        cvPdfUrl = artifactJob?.cvPdfUrl,
        coverLetterPdfUrl = artifactJob?.coverLetterPdfUrl,
        currentStep = currentStep.name,
        updatedAt = updatedAt,
    )

    private fun ApplyChatMessage.toEntity(sessionId: String): ApplyMessageEntity {
        val payload = SerializedMessagePayload(
            evaluationStatus = evaluation?.status?.wireKey,
            promptStep = promptStep?.name,
            artifactJobId = artifactJob?.jobId,
            artifactJobStatus = artifactJob?.status,
            cvArtifactId = artifactJob?.cvArtifactId,
            cvPdfUrl = artifactJob?.cvPdfUrl,
            coverLetterArtifactId = artifactJob?.coverLetterArtifactId,
            coverLetterPdfUrl = artifactJob?.coverLetterPdfUrl,
            outcomeArtifactId = artifactJob?.outcomeArtifactId,
            outcomeUrl = artifactJob?.outcomeUrl,
            error = artifactJob?.error,
        )
        return ApplyMessageEntity(
            id = id,
            sessionId = sessionId,
            sender = sender.name,
            messageType = messageType.name,
            text = text,
            payloadJson = internalJson.encodeToString(payload),
            timestamp = timestamp,
        )
    }

    private fun ApplySessionEntity.toDomain(messageEntities: List<ApplyMessageEntity>): ApplySessionState {
        val step = runCatching { ApplyWorkflowStep.valueOf(currentStep) }.getOrDefault(ApplyWorkflowStep.AWAITING_INPUT)
        val status = when (step) {
            ApplyWorkflowStep.DRAFTING -> TrackerStatus.DRAFTING
            ApplyWorkflowStep.DRAFTED, ApplyWorkflowStep.AWAITING_SUBMISSION_CONFIRMATION -> TrackerStatus.DRAFTED
            ApplyWorkflowStep.SUBMITTED -> TrackerStatus.SUBMITTED
            else -> TrackerStatus.RANKED
        }
        val eval = deterministicScore?.let { detScore ->
            ApplyEvaluationResult(
                applicationId = applicationId.orEmpty(),
                company = company,
                role = role,
                score = aiScore ?: detScore,
                deterministicScore = detScore,
                aiScore = aiScore,
                isAiEvaluated = aiScore != null,
                verdict = verdict.orEmpty(),
                aiRationale = aiRationale.orEmpty(),
                strengths = decodeStringList(strengthsJson),
                gaps = decodeStringList(gapsJson),
                dealBreakers = decodeStringList(dealBreakersJson),
                status = status,
            )
        }
        val job = CanonicalArtifactHelper.buildRestoredJobState(this)
        val messages = messageEntities.takeIf { it.isNotEmpty() }?.map { it.toDomain(eval, job) }
            ?: memorySession.value?.takeIf { it.id == id }?.messages.orEmpty()
        return ApplySessionState(
            id = id,
            inputMode = if (!postingUrl.isNullOrBlank()) ApplyInputMode.URL else ApplyInputMode.DESCRIPTION,
            postingUrl = postingUrl.orEmpty(),
            postingText = postingText.orEmpty(),
            company = company,
            role = role,
            evaluation = eval,
            artifactJob = job,
            currentStep = step,
            messages = messages,
            updatedAt = updatedAt,
        )
    }

    private fun ApplyMessageEntity.toDomain(
        sessionEval: ApplyEvaluationResult?,
        sessionJob: ApplyArtifactJobState?,
    ): ApplyChatMessage {
        val parsed = payloadJson?.let {
            runCatching { internalJson.decodeFromString<SerializedMessagePayload>(it) }.getOrNull()
        }
        val msgType = runCatching { ApplyMessageType.valueOf(messageType) }.getOrDefault(ApplyMessageType.TEXT)
        val senderEnum = runCatching { ApplyMessageSender.valueOf(sender) }.getOrDefault(ApplyMessageSender.ASSISTANT)
        val eval = if (msgType == ApplyMessageType.EVALUATION_CARD && sessionEval != null) {
            val status = parsed?.evaluationStatus?.let { TrackerStatus.fromRemote(it) } ?: sessionEval.status
            sessionEval.copy(status = status)
        } else {
            null
        }
        val job = if (msgType == ApplyMessageType.ARTIFACTS_CARD) {
            parsed?.toArtifactJob(sessionEval?.applicationId.orEmpty(), sessionJob)
        } else {
            null
        }
        return ApplyChatMessage(
            id = id,
            sender = senderEnum,
            messageType = msgType,
            text = text,
            evaluation = eval,
            promptStep = parsed?.promptStep?.let { s -> runCatching { ApplyWorkflowStep.valueOf(s) }.getOrNull() },
            artifactJob = job,
            timestamp = timestamp,
        )
    }

    private fun encodeStringList(items: List<String>): String =
        buildJsonArray { items.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }.toString()

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            internalJson.parseToJsonElement(raw).jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
        }.getOrDefault(emptyList())
    }
}

internal class NetworkApplyAssistantDataSourceImpl(
    private val httpClient: HttpClient? = null,
) : NetworkApplyAssistantDataSource {

    private val localFallbackJobs = mutableMapOf<String, ApplyArtifactJobState>()

    override suspend fun evaluateDeterministic(
        url: String?,
        text: String?,
        company: String?,
        role: String?,
    ): NetworkDeterministicResult {
        return fetchRemoteDeterministic(url, text, company, role)
            ?: localFallbackDeterministic(url, text, company, role)
    }

    private suspend fun fetchRemoteDeterministic(
        url: String?,
        text: String?,
        company: String?,
        role: String?,
    ): NetworkDeterministicResult? {
        val reqBody = buildJsonObject {
            url?.takeIf { it.isNotBlank() }?.let { put("url", it) }
            text?.takeIf { it.isNotBlank() }?.let { put("text", it) }
            company?.takeIf { it.isNotBlank() }?.let { put("company", it) }
            role?.takeIf { it.isNotBlank() }?.let { put("title", it) }
            put("persist", true)
        }.toString()
        val obj = postJson("/applications/apply", reqBody) ?: return null
        return CanonicalArtifactHelper.parseRemoteDeterministic(obj, text, company, role)
    }

    private fun localFallbackDeterministic(
        url: String?,
        text: String?,
        company: String?,
        role: String?,
    ): NetworkDeterministicResult {
        val combined = "${role.orEmpty()} ${company.orEmpty()} ${text.orEmpty()} ${url.orEmpty()}".lowercase()
        val matchedStrengths = listOfNotNull(
            "Kotlin".takeIf { "kotlin" in combined },
            "Jetpack Compose".takeIf { "compose" in combined },
            "Android SDK".takeIf { "android" in combined },
            "Gradle Architecture".takeIf { "gradle" in combined },
            "FinTech & Payments".takeIf { "fintech" in combined || "payment" in combined || "banking" in combined },
        ).ifEmpty { listOf("Native Android Architecture") }

        val resolvedCompany = company?.takeIf { it.isNotBlank() } ?: PLACEHOLDER_COMPANY
        val resolvedRole = role?.takeIf { it.isNotBlank() } ?: "Principal Android Engineer"
        val roleLower = resolvedRole.lowercase()
        val roleBonus = when {
            "principal" in roleLower || "staff" in roleLower || "architect" in roleLower -> ROLE_BONUS_PRINCIPAL
            "senior" in roleLower || "lead" in roleLower -> ROLE_BONUS_SENIOR
            else -> 0
        }
        val baseScore = (FALLBACK_BASE_SCORE + matchedStrengths.size * FALLBACK_STRENGTH_BONUS + roleBonus)
            .coerceIn(FALLBACK_MIN_SCORE, FALLBACK_MAX_SCORE)
        return NetworkDeterministicResult(
            applicationId = CanonicalArtifactHelper.documentId(resolvedCompany, resolvedRole),
            company = resolvedCompany,
            role = resolvedRole,
            score = baseScore,
            verdict = if (baseScore >= STRONG_MATCH_THRESHOLD) "STRONG_MATCH" else "GOOD_FIT",
            strengths = matchedStrengths,
            gaps = emptyList(),
            dealBreakers = if ("consulting" in combined) listOf("Consulting") else emptyList(),
            extractedText = text.orEmpty(),
        )
    }

    override suspend fun refineWithAi(
        applicationId: String,
        postingText: String,
        company: String,
        role: String,
        deterministicScore: Int,
        strengths: List<String>,
        gaps: List<String>,
        dealBreakers: List<String>,
    ): NetworkAiRankingResult {
        return fetchRemoteAiRank(
            applicationId = applicationId,
            postingText = postingText,
            company = company,
            role = role,
            deterministicScore = deterministicScore,
            strengths = strengths,
            gaps = gaps,
            dealBreakers = dealBreakers,
        ) ?: localFallbackAiRank(deterministicScore, strengths, gaps, dealBreakers)
    }

    private suspend fun fetchRemoteAiRank(
        applicationId: String,
        postingText: String,
        company: String,
        role: String,
        deterministicScore: Int,
        strengths: List<String>,
        gaps: List<String>,
        dealBreakers: List<String>,
    ): NetworkAiRankingResult? {
        val reqBody = buildJsonObject {
            put("applicationId", applicationId)
            put("postingText", postingText)
            put("company", company)
            put("title", role)
            put("deterministicScore", deterministicScore)
            put("strengths", buildJsonArray { strengths.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            put("gaps", buildJsonArray { gaps.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            put("dealBreakers", buildJsonArray { dealBreakers.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
        }.toString()
        val obj = postJson("/ai/rank", reqBody) ?: return null
        return NetworkAiRankingResult(
            score = obj["score"]?.jsonPrimitive?.intOrNull ?: deterministicScore,
            verdict = obj["verdict"]?.jsonPrimitive?.contentOrNull ?: "STRONG_MATCH",
            aiRationale = obj["aiRationale"]?.jsonPrimitive?.contentOrNull
                ?: "AI calibration refined alignment to $deterministicScore.",
            strengths = obj["strengths"]?.toStringList()?.ifEmpty { strengths } ?: strengths,
            gaps = obj["gaps"]?.toStringList()?.ifEmpty { gaps } ?: gaps,
            dealBreakers = obj["dealBreakers"]?.toStringList()?.ifEmpty { dealBreakers } ?: dealBreakers,
        )
    }

    private fun localFallbackAiRank(
        deterministicScore: Int,
        strengths: List<String>,
        gaps: List<String>,
        dealBreakers: List<String>,
    ): NetworkAiRankingResult {
        val refinedScore = (deterministicScore + AI_FALLBACK_DELTA).coerceAtMost(AI_FALLBACK_MAX_SCORE)
        return NetworkAiRankingResult(
            score = refinedScore,
            verdict = if (refinedScore >= STRONG_MATCH_THRESHOLD) "STRONG_MATCH" else "GOOD_FIT",
            aiRationale = "Semantic alignment across Android architecture and FinTech platform scale calibrated score from $deterministicScore to $refinedScore.",
            strengths = strengths,
            gaps = gaps,
            dealBreakers = dealBreakers,
        )
    }

    private suspend fun postJson(path: String, reqBody: String): JsonObject? {
        val client = httpClient ?: return null
        val baseUrl = RemoteNetworkConfig.baseUrl.trimEnd('/')
        if (baseUrl.isBlank()) return null
        return runCatching {
            val response = client.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                RemoteNetworkConfig.apiKey?.takeIf { it.isNotBlank() }?.let {
                    header(RemoteNetworkConfig.API_KEY_HEADER, it)
                }
                setBody(reqBody)
            }
            if (!response.status.isSuccess()) error("HTTP ${response.status}")
            internalJson.parseToJsonElement(response.bodyAsText()).jsonObject
        }.getOrNull()
    }

    override suspend fun startArtifactJob(
        applicationId: String,
        company: String,
        role: String,
        postingText: String,
    ): ApplyArtifactJobState {
        val spec = CanonicalArtifactHelper.forJob(company, role, applicationId)
        val reqBody = buildJsonObject {
            put("applicationId", applicationId)
            put("company", company)
            put("role", role)
            put("postingText", postingText)
            put("persist", true)
        }.toString()
        val obj = postJson("/artifacts/create", reqBody)
        if (obj != null) {
            return ApplyArtifactJobState(
                jobId = obj["jobId"]?.jsonPrimitive?.contentOrNull ?: "job_${Uuid7.generate()}",
                applicationId = applicationId,
                status = "IN_PROGRESS",
                cvArtifactId = spec.cvArtifactId,
                coverLetterArtifactId = spec.coverArtifactId,
                outcomeArtifactId = spec.outcomeArtifactId,
                outcomeUrl = spec.outcomePath,
            )
        }

        val jobId = "job_art_${Uuid7.generate()}"
        localFallbackJobs[jobId] = ApplyArtifactJobState(
            jobId = jobId,
            applicationId = applicationId,
            status = "SUCCESS",
            cvPdfUrl = spec.cvPath,
            coverLetterPdfUrl = spec.coverPath,
            cvArtifactId = spec.cvArtifactId,
            coverLetterArtifactId = spec.coverArtifactId,
            outcomeArtifactId = spec.outcomeArtifactId,
            outcomeUrl = spec.outcomePath,
        )
        return ApplyArtifactJobState(
            jobId = jobId,
            applicationId = applicationId,
            status = "IN_PROGRESS",
            cvArtifactId = spec.cvArtifactId,
            coverLetterArtifactId = spec.coverArtifactId,
            outcomeArtifactId = spec.outcomeArtifactId,
            outcomeUrl = spec.outcomePath,
        )
    }

    override suspend fun getArtifactJob(jobId: String): ApplyArtifactJobState? {
        localFallbackJobs[jobId]?.let { return it }
        val client = httpClient
        val baseUrl = RemoteNetworkConfig.baseUrl.trimEnd('/')
        if (client == null || baseUrl.isBlank()) return localFallbackJobs[jobId]
        val fetched = runCatching {
            val response = client.get("$baseUrl/artifacts/jobs/$jobId") {
                RemoteNetworkConfig.apiKey?.takeIf { it.isNotBlank() }?.let {
                    header(RemoteNetworkConfig.API_KEY_HEADER, it)
                }
            }
            if (!response.status.isSuccess()) return@runCatching null
            val obj = internalJson.parseToJsonElement(response.bodyAsText()).jsonObject
            ApplyArtifactJobState(
                jobId = obj["jobId"]?.jsonPrimitive?.contentOrNull ?: jobId,
                applicationId = obj["applicationId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "IN_PROGRESS",
                cvPdfUrl = obj["cvPdfUrl"]?.jsonPrimitive?.contentOrNull,
                coverLetterPdfUrl = obj["coverLetterPdfUrl"]?.jsonPrimitive?.contentOrNull,
                error = obj["error"]?.jsonPrimitive?.contentOrNull,
                cvArtifactId = obj["cvArtifactId"]?.jsonPrimitive?.contentOrNull,
                coverLetterArtifactId = obj["coverLetterArtifactId"]?.jsonPrimitive?.contentOrNull,
                outcomeArtifactId = obj["outcomeArtifactId"]?.jsonPrimitive?.contentOrNull,
                outcomeUrl = obj["outcomeUrl"]?.jsonPrimitive?.contentOrNull,
            )
        }.getOrNull()
        return fetched ?: localFallbackJobs[jobId]
    }
}

internal class RemoteApplyAssistantDataSourceImpl(
    private val remoteDataSource: RemoteDataSource? = null,
) : RemoteApplyAssistantDataSource {

    override fun observeArtifactJob(jobId: String): Flow<ApplyArtifactJobState?> {
        val source = remoteDataSource ?: return emptyFlow()
        return source.observeCollection("artifact_jobs")
            .map { records ->
                records.firstOrNull { it.id == jobId || it.getString("jobId") == jobId }?.let { doc ->
                    ApplyArtifactJobState(
                        jobId = doc.getString("jobId") ?: doc.id,
                        applicationId = doc.getString("applicationId").orEmpty(),
                        status = doc.getString("status") ?: "IN_PROGRESS",
                        cvPdfUrl = doc.getString("cvPdfUrl"),
                        coverLetterPdfUrl = doc.getString("coverLetterPdfUrl"),
                        error = doc.getString("error"),
                        cvArtifactId = doc.getString("cvArtifactId"),
                        coverLetterArtifactId = doc.getString("coverLetterArtifactId"),
                        outcomeArtifactId = doc.getString("outcomeArtifactId"),
                        outcomeUrl = doc.getString("outcomeUrl"),
                    )
                }
            }
            .catch { emit(null) }
    }

    override suspend fun syncApplicationStatus(
        applicationId: String,
        company: String,
        role: String,
        status: TrackerStatus,
        fitRating: Int?,
        jobUrl: String?,
        note: String,
        evaluation: ApplyEvaluationResult?,
        artifactJob: ApplyArtifactJobState?,
    ) {
        val source = remoteDataSource ?: return
        val existing = runCatching { source.getDocument("applications", applicationId) }.getOrNull()
        val nowIso = Clock.System.now().toString()
        val fields = mutableMapOf<String, Any?>(
            "id" to applicationId,
            "company" to company,
            "companyId" to CanonicalArtifactHelper.slug(company, "unknown-company"),
            "role" to role,
            "status" to status.wireKey,
            "statusRank" to DEFAULT_STATUS_RANK,
            "isOpen" to !status.isTerminal,
            "updatedAt" to nowIso,
        )
        if (fitRating != null) fields["fitRating"] = fitRating
        if (!jobUrl.isNullOrBlank()) fields["jobUrl"] = jobUrl
        runCatching {
            if (existing == null) {
                fields["createdAt"] = nowIso
                source.setDocument("applications", applicationId, MapRemoteRecord(id = applicationId, fields = fields))
            } else {
                source.updateDocument("applications", applicationId, MapRemoteRecord(id = applicationId, fields = fields))
            }
        }
        if (status == TrackerStatus.DRAFTED) {
            writeDraftArtifactMetadata(source, applicationId, company, role, evaluation, artifactJob, jobUrl, nowIso)
        }
    }

    private suspend fun writeDraftArtifactMetadata(
        source: RemoteDataSource,
        applicationId: String,
        company: String,
        role: String,
        evaluation: ApplyEvaluationResult?,
        artifactJob: ApplyArtifactJobState?,
        jobUrl: String?,
        nowIso: String,
    ) {
        val collectionPath = "applications/$applicationId/artifacts"
        val entries = CanonicalArtifactHelper.buildDraftArtifactEntries(
            applicationId = applicationId,
            company = company,
            role = role,
            evaluation = evaluation,
            artifactJob = artifactJob,
            jobUrl = jobUrl,
            nowIso = nowIso,
        )
        entries.forEach { (docId, fields) ->
            val existingDoc = runCatching { source.getDocument(collectionPath, docId) }.getOrNull()
            val hasPayload = existingDoc != null && (
                !existingDoc.getString("content").isNullOrBlank() ||
                    !existingDoc.getString("externalUrl").isNullOrBlank()
                )
            if (!hasPayload) {
                runCatching { source.setDocument(collectionPath, docId, MapRemoteRecord(id = docId, fields = fields)) }
            }
        }
    }
}
