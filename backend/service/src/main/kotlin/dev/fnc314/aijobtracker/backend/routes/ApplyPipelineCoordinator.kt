package dev.fnc314.aijobtracker.backend.routes

import com.google.cloud.Timestamp
import com.google.cloud.firestore.SetOptions
import dev.fnc314.aijobtracker.backend.BackendService
import dev.fnc314.aijobtracker.backend.ai.client.AiClientFactory
import dev.fnc314.aijobtracker.backend.ai.client.AiRankingRequest
import dev.fnc314.aijobtracker.backend.ai.client.AiRankingResponse
import dev.fnc314.aijobtracker.backend.ai.client.AiRankingService
import dev.fnc314.aijobtracker.backend.ai.client.ArtifactDraftingService
import dev.fnc314.aijobtracker.backend.ai.client.DraftingRequest
import dev.fnc314.aijobtracker.backend.compilation.DocumentCompilerService
import dev.fnc314.aijobtracker.backend.ranking.RankResult
import dev.fnc314.aijobtracker.backend.ranking.RankScorer
import dev.fnc314.aijobtracker.records.parser.DocumentIdGenerator
import dev.fnc314.aijobtracker.records.parser.PostingSimilarity
import dev.fnc314.aijobtracker.records.parser.StatusNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Response payload returned by `POST /applications/apply` for Tier 1 deterministic evaluation.
 */
@Serializable
public data class DeterministicApplyResponse(
    /** Canonical application identifier if generated or supplied. */
    public val applicationId: String,
    /** Normalized target company name. */
    public val company: String,
    /** Target job role title. */
    public val role: String,
    /** Overall deterministic fit score (0..100). */
    public val score: Int,
    /** Qualitative fit verdict from RankScorer. */
    public val verdict: String,
    /** False because this is the Tier 1 deterministic score. */
    public val isAiEvaluated: Boolean = false,
    /** Identified candidate strengths. */
    public val strengths: List<String>,
    /** Identified skill or experience gaps. */
    public val gaps: List<String>,
    /** Identified disqualifiers. */
    public val dealBreakers: List<String>,
    /** Location evaluation summary. */
    public val locationVerdict: String = "",
    /** Full deterministic RankResult breakdown. */
    public val rankResult: RankResult,
    /** Extracted and massaged job description text for downstream AI calibration and drafting. */
    public val postingText: String = "",
    /** True when the record was persisted to Firestore as RANKED. */
    public val persisted: Boolean = false,
    /** True when Gate 0 found similar records and halted before any write; user must resolve. */
    public val blockedForReview: Boolean = false,
    /** Existing applications that collide with this posting. */
    public val similarApplications: List<SimilarApplicationSummary> = emptyList(),
)

/** A pre-existing application that Gate 0 considers identical or similar to an incoming posting. */
@Serializable
public data class SimilarApplicationSummary(
    public val applicationId: String,
    public val role: String,
    public val status: String,
    public val score: Double,
    public val reason: String,
)

/** User decision for a Gate 0 collision. */
public enum class DuplicateResolution {
    /** Treat as the existing record (re-post / URL update); never creates a new document. */
    MERGE,

    /** A distinct opening; creates a new document (suffixing the id if it collides). */
    NEW,

    /** Duplicate; stop without writing. */
    SKIP,
}

/**
 * Immediate HTTP 202 response returned when an asynchronous artifact job is accepted.
 */
@Serializable
public data class ArtifactJobAcceptedResponse(
    /** Unique job identifier used to track progress in `artifact_jobs/{jobId}`. */
    public val jobId: String,
    /** Current application/job lifecycle status (DRAFTING). */
    public val status: String,
    /** Associated application identifier in Firestore. */
    public val applicationId: String,
)

/**
 * Snapshot state of an asynchronous artifact generation job.
 */
@Serializable
public data class ArtifactJobSnapshot(
    /** Unique job identifier. */
    public val jobId: String,
    /** Associated application identifier in Firestore. */
    public val applicationId: String,
    /** Current job status: PENDING, IN_PROGRESS, SUCCESS, or FAILED. */
    public val status: String,
    /** Generated CV artifact identifier when ready. */
    public val cvArtifactId: String? = null,
    /** Path or URL to compiled CV PDF or source when ready. */
    public val cvPdfUrl: String? = null,
    /** Generated cover letter artifact identifier when ready. */
    public val coverLetterArtifactId: String? = null,
    /** Path or URL to compiled cover letter PDF or source when ready. */
    public val coverLetterPdfUrl: String? = null,
    /** Generated fit evaluation outcome artifact identifier when ready. */
    public val outcomeArtifactId: String? = null,
    /** Path or URL to fit evaluation outcome.md when ready. */
    public val outcomeUrl: String? = null,
    /** Error message when job status is FAILED. */
    public val error: String? = null,
    /** ISO-8601 timestamp of the latest update. */
    public val updatedAt: String,
)

/**
 * Structured metadata and massaged posting content extracted from a job posting URL or raw text.
 */
public data class ParsedPostingData(
    /** Extracted employer name, if discovered. */
    public val company: String? = null,
    /** Extracted job title / role, if discovered. */
    public val role: String? = null,
    /** Extracted job location string, if discovered. */
    public val location: String? = null,
    /** Extracted company LinkedIn profile URL, if discovered. */
    public val companyLinkedinUrl: String? = null,
    /** Extracted company website URL hint for Hunter.io enrichment, if discovered. */
    public val companyWebsite: String? = null,
    /** Cleaned, normalized posting text ready for ranking, AI calibration, and artifact drafting. */
    public val postingText: String = "",
)

/**
 * Centralized pipeline coordinator shared by Ktor HTTP routes and CLI commands for
 * URL scraping, Hunter.io company enrichment, deterministic + AI fit ranking, and artifact generation.
 */
public object ApplyPipelineCoordinator {
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    private val lenientJson: Json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val authWallTitles = setOf(
        "sign in", "log in", "login", "linkedin", "join linkedin", "just a moment", "access denied",
    )
    private val thirdPartyJobBoardHosts = setOf(
        "linkedin.com", "ashbyhq.com", "greenhouse.io", "lever.co", "myworkdayjobs.com",
        "workday.com", "smartrecruiters.com", "icims.com", "jobvite.com", "indeed.com",
    )

    private val jobs = ConcurrentHashMap<String, ArtifactJobSnapshot>()
    internal var artifactsRootDir: File = File("job-applications")

    internal fun resolveFetchUrl(rawUrl: String): String {
        val linkedInId = Regex(
            "(?:linkedin\\.com/jobs/view/(?:[^/?#]*-)?|urn:li:jobPosting:|currentJobId=)(\\d{6,})",
            RegexOption.IGNORE_CASE,
        ).find(rawUrl)?.groupValues?.getOrNull(1)
        return if (linkedInId != null) {
            "https://www.linkedin.com/jobs-guest/jobs/api/jobPosting/$linkedInId"
        } else {
            rawUrl.trim()
        }
    }

    /**
     * Resolves clean posting text from raw text or by fetching & parsing a remote URL.
     */
    public fun fetchPostingTextIfUrl(url: String?, rawText: String?): String =
        fetchAndParsePosting(url, rawText).postingText

    /**
     * Visits a job posting URL (or parses raw text) and extracts structured company, role, location,
     * social/website links, and clean posting copy.
     */
    public fun fetchAndParsePosting(url: String?, rawText: String?): ParsedPostingData {
        if (!rawText.isNullOrBlank()) {
            val extracted = RankScorer.extractJobInfo(rawText)
            return ParsedPostingData(
                company = extracted.company.takeIf { it.isNotBlank() },
                role = extracted.role.takeIf { it.isNotBlank() },
                location = extracted.location.takeIf { it.isNotBlank() },
                companyWebsite = url?.let(::extractDirectCompanyWebsite),
                postingText = rawText.trim(),
            )
        }
        if (url.isNullOrBlank()) return ParsedPostingData()
        val primaryUrl = resolveFetchUrl(url)
        val html = fetchHtmlText(primaryUrl)
            ?: if (primaryUrl != url.trim()) fetchHtmlText(url.trim()) else null
        return parsePostingHtml(html.orEmpty(), url)
    }

    internal fun parsePostingHtml(html: String, originalUrl: String): ParsedPostingData {
        val jsonLd = extractJsonLd(html)
        val roleFromCard = Regex(
            "class=[\"'](?:top-card-layout__title|topcard__title)[^\"']*[\"'][^>]*>([\\s\\S]*?)</h[12]>",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.getOrNull(1)?.let(::cleanInlineHtml)
        val firstH1Role = Regex("<h1[^>]*>([\\s\\S]*?)</h1>", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.let(::cleanInlineHtml)

        val orgLinkMatch = Regex(
            "class=[\"']topcard__org-name-link[^\"']*[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*>([\\s\\S]*?)</a>",
            RegexOption.IGNORE_CASE,
        ).find(html)
        val companyFromCard = (
            orgLinkMatch?.groupValues?.getOrNull(2)
                ?: Regex("class=[\"']topcard__flavor[^\"']*[\"'][^>]*>([\\s\\S]*?)</span>", RegexOption.IGNORE_CASE)
                    .find(html)?.groupValues?.getOrNull(1)
            )?.let(::cleanInlineHtml)
        val companyLinkedinUrl = orgLinkMatch?.groupValues?.getOrNull(1)
            ?.let { decodeHtmlEntities(it).substringBefore("?") }
            ?.takeIf { it.isNotBlank() }
            ?: jsonLd.linkedinUrl

        val locationFromCard = Regex(
            "class=[\"']topcard__flavor topcard__flavor--bullet[^\"']*[\"'][^>]*>([\\s\\S]*?)</span>",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.getOrNull(1)?.let(::cleanInlineHtml)

        val applyHref = Regex(
            "class=[\"']topcard__link[^\"']*[\"'][^>]*href=[\"']([^\"']+)[\"']",
            RegexOption.IGNORE_CASE,
        ).find(html)?.groupValues?.getOrNull(1)?.let(::decodeHtmlEntities)

        val ogSiteName = extractMetaContent(html, "property", "og:site_name")
            ?.let { cleanSiteCompanyName(decodeHtmlEntities(it)) }
        val urlHints = parseUrlHints(originalUrl)
        val knownCompanyHint = companyFromCard?.takeIf { !isAuthWall(it) }
            ?: jsonLd.company?.takeIf { !isAuthWall(it) }
            ?: ogSiteName?.takeIf { !isAuthWall(it) }
            ?: urlHints.company

        val rawTitle = extractMetaContent(html, "property", "og:title") ?: extractTitleTag(html) ?: ""
        val cleanedTitle = decodeHtmlEntities(rawTitle).trim()
        val (titleCompany, titleRole) = parsePostingTitle(cleanedTitle, hasKnownCompany = knownCompanyHint != null)

        val resolvedRole = roleFromCard?.takeIf { !isAuthWall(it) }
            ?: jsonLd.role?.takeIf { !isAuthWall(it) }
            ?: firstH1Role?.takeIf { !isAuthWall(it) }
            ?: titleRole?.takeIf { !isAuthWall(it) }
            ?: urlHints.role
        val resolvedCompany = knownCompanyHint
            ?: titleCompany?.takeIf { !isAuthWall(it) }
        val resolvedLocation = locationFromCard ?: jsonLd.location
        val companyWebsite = jsonLd.website
            ?: applyHref?.let(::extractDirectCompanyWebsite)
            ?: urlHints.website
            ?: extractDirectCompanyWebsite(originalUrl)

        val descriptionDiv = extractDivContent(html, "show-more-less-html__markup")
            ?: extractDivContent(html, "description__text")
        val metaDescription = extractMetaContent(html, "property", "og:description")
            ?: extractMetaContent(html, "name", "description")
            ?: ""
        val bodyText = descriptionDiv?.let(::cleanBlockHtml)
            ?: jsonLd.description?.let(::cleanBlockHtml)
            ?: stripHtml(html)
        val combinedBody = decodeHtmlEntities(listOf(metaDescription, bodyText).filter { it.isNotBlank() }.joinToString("\n\n")).trim()

        val structuredPosting = buildString {
            if (!resolvedRole.isNullOrBlank() && !resolvedCompany.isNullOrBlank()) {
                appendLine("$resolvedRole at $resolvedCompany")
            } else if (!resolvedRole.isNullOrBlank()) {
                appendLine(resolvedRole)
            }
            if (!resolvedLocation.isNullOrBlank()) {
                appendLine("Location: $resolvedLocation")
            }
            if (combinedBody.isNotBlank()) {
                if (isNotEmpty()) appendLine()
                append(combinedBody)
            }
        }.trim()

        return ParsedPostingData(
            company = resolvedCompany,
            role = resolvedRole,
            location = resolvedLocation,
            companyLinkedinUrl = companyLinkedinUrl,
            companyWebsite = companyWebsite,
            postingText = structuredPosting,
        )
    }

    private fun fetchHtmlText(targetUrl: String): String? = runCatching {
        val request = HttpRequest.newBuilder(URI.create(targetUrl))
            .timeout(Duration.ofSeconds(15))
            .header(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            )
            .header("Accept-Language", "en-US,en;q=0.9")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) response.body() else null
    }.getOrNull()

    private data class JsonLdFields(
        internal val company: String? = null,
        internal val role: String? = null,
        internal val location: String? = null,
        internal val website: String? = null,
        internal val linkedinUrl: String? = null,
        internal val description: String? = null,
    )

    private fun extractJsonLd(html: String): JsonLdFields {
        val blocks = Regex("<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>([\\s\\S]*?)</script>", RegexOption.IGNORE_CASE)
            .findAll(html)
        for (match in blocks) {
            val raw = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (raw.isBlank()) continue
            val elem = runCatching { lenientJson.parseToJsonElement(raw) }.getOrNull() ?: continue
            val candidates = when (elem) {
                is JsonObject -> listOf(elem)
                is JsonArray -> elem.mapNotNull { it as? JsonObject }
                else -> emptyList()
            }
            val jobObj = candidates.firstOrNull { obj ->
                val type = obj["@type"]?.jsonPrimitive?.contentOrNull.orEmpty()
                type.equals("JobPosting", ignoreCase = true) || obj.containsKey("hiringOrganization")
            } ?: continue
            val orgObj = jobObj["hiringOrganization"] as? JsonObject
            val sameAs = orgObj?.get("sameAs")?.jsonPrimitive?.contentOrNull
            val locObj = jobObj["jobLocation"] as? JsonObject
            val addrObj = locObj?.get("address") as? JsonObject
            val city = addrObj?.get("addressLocality")?.jsonPrimitive?.contentOrNull
            val region = addrObj?.get("addressRegion")?.jsonPrimitive?.contentOrNull
            val loc = listOfNotNull(city, region).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }
            return JsonLdFields(
                company = orgObj?.get("name")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                role = jobObj["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
                location = loc,
                website = sameAs?.takeIf { "linkedin.com" !in it.lowercase() }?.let(::extractDirectCompanyWebsite),
                linkedinUrl = sameAs?.takeIf { "linkedin.com" in it.lowercase() },
                description = jobObj["description"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            )
        }
        return JsonLdFields()
    }

    private data class UrlHints(
        internal val company: String? = null,
        internal val role: String? = null,
        internal val website: String? = null,
    )

    private fun extractDirectCompanyWebsite(rawUrl: String): String? {
        val host = runCatching { URI.create(rawUrl.trim()).host }.getOrNull()?.lowercase() ?: return null
        val stripped = host.removePrefix("www.")
        val isThirdParty = thirdPartyJobBoardHosts.any { board -> stripped == board || stripped.endsWith(".$board") }
        if (isThirdParty || "." !in stripped) return null
        val rootDomain = stripped
            .removePrefix("careers.")
            .removePrefix("jobs.")
            .removePrefix("apply.")
            .removePrefix("join.")
        val brand = rootDomain.substringBefore(".")
        return if (rootDomain.endsWith(".jobs") || rootDomain.endsWith(".careers")) {
            "https://$brand.com"
        } else {
            "https://$rootDomain"
        }
    }

    private fun parseUrlHints(rawUrl: String): UrlHints {
        val uri = runCatching { URI.create(rawUrl.trim()) }.getOrNull() ?: return UrlHints()
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: ""
        val path = uri.path.orEmpty()

        val (linkedInCompany, linkedInRole) = parseLinkedInSlugHint(rawUrl)
        if ("linkedin.com" in host) {
            return UrlHints(company = linkedInCompany, role = linkedInRole)
        }

        val atsCompanyMatch = Regex("(?:jobs\\.ashbyhq\\.com|(?:boards|job-boards)\\.greenhouse\\.io|jobs\\.lever\\.co)/([^/?#]+)")
            .find(rawUrl)?.groupValues?.getOrNull(1)
        if (!atsCompanyMatch.isNullOrBlank()) {
            return UrlHints(company = humanizeSlug(atsCompanyMatch))
        }

        val workdayMatch = Regex("([^./]+)\\.wd\\d+\\.myworkdayjobs\\.com", RegexOption.IGNORE_CASE).find(host)
        if (workdayMatch != null) {
            val rawRoleSeg = path.trimEnd('/').substringAfterLast('/').replace(Regex("_JR[0-9A-Za-z-]+$"), "")
            val roleHint = rawRoleSeg.takeIf { "-" in it && it.length > 4 }?.let(::humanizeSlug)
            return UrlHints(company = humanizeSlug(workdayMatch.groupValues[1]), role = roleHint)
        }

        val domainWebsite = extractDirectCompanyWebsite(rawUrl)
        val cleanBrandHost = host
            .removePrefix("careers.")
            .removePrefix("jobs.")
            .removePrefix("apply.")
            .removePrefix("join.")
        val domainCompany = cleanBrandHost.substringBefore(".").takeIf { it.length > 1 }?.let(::humanizeSlug)
        val slugSegment = path.split('/').lastOrNull { seg ->
            seg.contains('-') && !seg.matches(Regex("^\\d+$")) && seg.length > 4
        }
        val roleHint = slugSegment?.let(::humanizeSlug)
        return UrlHints(company = domainCompany, role = roleHint, website = domainWebsite)
    }

    private fun parseLinkedInSlugHint(url: String): Pair<String?, String?> {
        val slugMatch = Regex("linkedin\\.com/jobs/view/([a-z0-9-]+?)-\\d{6,}", RegexOption.IGNORE_CASE).find(url)
            ?: return null to null
        val slug = slugMatch.groupValues[1]
        val atParts = slug.split("-at-", limit = 2)
        return if (atParts.size == 2) {
            humanizeSlug(atParts[1]) to humanizeSlug(atParts[0])
        } else {
            null to humanizeSlug(slug)
        }
    }

    private fun humanizeSlug(slug: String): String =
        slug.split('-').filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    private fun extractDivContent(html: String, className: String): String? {
        val escaped = Regex.escape(className)
        val openRe = Regex("<div[^>]*class=[\"'][^\"']*$escaped[^\"']*[\"'][^>]*>", RegexOption.IGNORE_CASE)
        val open = openRe.find(html) ?: return null
        var i = open.range.last + 1
        var depth = 1
        while (depth > 0 && i < html.length) {
            val nextOpen = html.indexOf("<div", i, ignoreCase = true)
            val nextClose = html.indexOf("</div>", i, ignoreCase = true)
            if (nextClose == -1) return null
            if (nextOpen != -1 && nextOpen < nextClose) {
                depth++
                i = nextOpen + 4
            } else {
                depth--
                i = nextClose + 6
            }
        }
        return html.substring(open.range.last + 1, i - 6)
    }

    private fun extractTitleTag(html: String): String? =
        Regex("<title[^>]*>([\\s\\S]*?)</title>", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)

    private fun extractMetaContent(html: String, attrName: String, attrValue: String): String? {
        val escaped = Regex.escape(attrValue)
        val forward = Regex("<meta[^>]+$attrName=[\"']$escaped[\"'][^>]+content=[\"']([^\"']*)[\"'][^>]*>", RegexOption.IGNORE_CASE)
        val reverse = Regex("<meta[^>]+content=[\"']([^\"']*)[\"'][^>]+$attrName=[\"']$escaped[\"'][^>]*>", RegexOption.IGNORE_CASE)
        return forward.find(html)?.groupValues?.getOrNull(1) ?: reverse.find(html)?.groupValues?.getOrNull(1)
    }

    private fun cleanSiteCompanyName(raw: String): String? {
        val cleaned = raw
            .replace(Regex("\\.(jobs|careers|com)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(Careers|Jobs)$", RegexOption.IGNORE_CASE), "")
            .trim()
        val lower = cleaned.lowercase()
        val boardNames = setOf("linkedin", "indeed", "glassdoor", "greenhouse", "lever", "ashby", "workday")
        if (cleaned.isBlank() || lower in boardNames) return null
        return if (cleaned == lower) humanizeSlug(cleaned.replace(' ', '-')) else cleaned
    }

    private fun parsePostingTitle(rawTitle: String, hasKnownCompany: Boolean = false): Pair<String?, String?> {
        val stripped = rawTitle
            .replace(Regex("\\s*\\|\\s*LinkedIn.*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*-\\s*Job\\s*ID:\\s*[A-Za-z0-9-]+", RegexOption.IGNORE_CASE), "")
            .trim()
        if (stripped.isBlank() || isAuthWall(stripped)) return null to null

        val pipeParts = stripped.split(Regex("\\s*\\|\\s*"), limit = 2)
        if (pipeParts.size == 2) {
            val left = pipeParts[0].trim().takeIf { it.isNotBlank() }
            val rightCompany = cleanSiteCompanyName(pipeParts[1])
            return rightCompany to left
        }

        val hiringSplit = stripped.split(Regex("\\s+hiring\\s+", RegexOption.IGNORE_CASE), limit = 2)
        if (hiringSplit.size == 2) {
            val comp = hiringSplit[0].trim().takeIf { it.isNotBlank() && !isAuthWall(it) }
            val role = hiringSplit[1].replace(Regex("\\s+in\\s+.*$", RegexOption.IGNORE_CASE), "").trim()
            return comp to role.takeIf { it.isNotBlank() }
        }

        val atSplit = stripped.split(Regex("\\s+at\\s+", RegexOption.IGNORE_CASE), limit = 2)
        if (atSplit.size == 2) {
            return cleanSiteCompanyName(atSplit[1]) to atSplit[0].trim().takeIf { it.isNotBlank() }
        }

        if (!hasKnownCompany) {
            val dashSplit = stripped.split(Regex("\\s+-\\s+|\\s+–\\s+"), limit = 2)
            if (dashSplit.size == 2) {
                return dashSplit[1].trim().takeIf { it.isNotBlank() } to dashSplit[0].trim().takeIf { it.isNotBlank() }
            }
        }
        return null to stripped
    }

    private fun isAuthWall(text: String): Boolean = text.trim().lowercase() in authWallTitles

    private fun cleanInlineHtml(fragment: String): String? =
        decodeHtmlEntities(fragment.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim())
            .takeIf { it.isNotBlank() }

    private fun cleanBlockHtml(fragment: String): String =
        decodeHtmlEntities(
            fragment
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</(p|li|ul|ol|div|h\\d)>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("[ \\t]+"), " ")
                .replace(Regex("\\n{3,}"), "\n\n")
                .trim(),
        )

    private fun decodeHtmlEntities(input: String): String = input
        .replace("&amp;", "&")
        .replace("&#x27;", "'")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&quot;", "\"")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    private fun stripHtml(html: String): String = html
        .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    /**
     * Runs deterministic Tier 1 fit evaluation from a URL or raw posting text, performing
     * centralized HTML parsing, Hunter.io company enrichment, and Firestore persistence.
     */
    public fun evaluateDeterministic(
        url: String?,
        rawText: String?,
        companyOverride: String?,
        roleOverride: String?,
        providedApplicationId: String?,
        persistToRemote: Boolean,
        resolution: DuplicateResolution? = null,
    ): DeterministicApplyResponse {
        val parsed = fetchAndParsePosting(url, rawText)
        val initialEval = RankScorer.evaluate(parsed.postingText)
        val initialCompany = companyOverride?.takeIf { it.isNotBlank() }
            ?: parsed.company?.takeIf { it.isNotBlank() }
            ?: initialEval.company.ifBlank { "Target Company" }
        val role = roleOverride?.takeIf { it.isNotBlank() }
            ?: parsed.role?.takeIf { it.isNotBlank() }
            ?: initialEval.role.ifBlank { "Software Engineer" }

        val enrichedCompanyData = if (persistToRemote && initialCompany != "Target Company") {
            runCatching {
                BackendService.ensureCompanyEnriched(
                    companyName = initialCompany,
                    websiteHint = parsed.companyWebsite,
                    linkedinUrlHint = parsed.companyLinkedinUrl,
                )
            }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }

        val officialHunterName = (enrichedCompanyData["name"] as? String)?.takeIf { it.isNotBlank() }
        val company = companyOverride?.takeIf { it.isNotBlank() }
            ?: officialHunterName
            ?: initialCompany

        val hunterSectorContext = listOfNotNull(
            enrichedCompanyData["sector"] as? String,
            enrichedCompanyData["subIndustry"] as? String,
            (enrichedCompanyData["metadata"] as? Map<*, *>)?.get("hunterIndustry") as? String,
        ).distinct().joinToString(" ")

        val scoringInput = buildString {
            if (role != "Software Engineer" || company != "Target Company") {
                appendLine("$role at $company")
            }
            if (!parsed.location.isNullOrBlank()) {
                appendLine("Location: ${parsed.location}")
            }
            if (hunterSectorContext.isNotBlank()) {
                appendLine("Company Sector: $hunterSectorContext")
            }
            if (parsed.postingText.isNotBlank()) {
                append(parsed.postingText)
            }
        }.trim()

        val evaluated = RankScorer.evaluate(scoringInput)
        val proposedId = providedApplicationId?.takeIf { it.isNotBlank() }
            ?: DocumentIdGenerator.documentId(company, role)

        // Gate 0: never create or mutate a record before deduplication. Ambiguity blocks.
        val similar = if (persistToRemote && providedApplicationId.isNullOrBlank()) {
            runCatching { findSimilarApplications(company, role, url) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        var applicationId = proposedId
        var blocked = false
        if (similar.isNotEmpty()) {
            when (resolution) {
                null, DuplicateResolution.SKIP -> blocked = true
                DuplicateResolution.MERGE -> applicationId = similar.first().candidate.id
                DuplicateResolution.NEW -> if (similar.any { it.candidate.id == proposedId }) {
                    applicationId = "$proposedId--${LocalDate.now()}"
                }
            }
        }

        var persisted = false
        if (persistToRemote && !blocked) {
            persisted = runCatching {
                persistRankedApplication(
                    applicationId = applicationId,
                    company = company,
                    role = role,
                    url = url,
                    location = parsed.location,
                    result = evaluated,
                )
            }.isSuccess
        }

        return DeterministicApplyResponse(
            applicationId = applicationId,
            company = company,
            role = role,
            score = evaluated.overall,
            verdict = evaluated.verdict,
            isAiEvaluated = false,
            strengths = evaluated.strengths,
            gaps = evaluated.gaps,
            dealBreakers = evaluated.deal_breakers,
            locationVerdict = evaluated.locationVerdict,
            rankResult = evaluated.copy(company = company, role = role),
            postingText = parsed.postingText,
            persisted = persisted,
            blockedForReview = blocked,
            similarApplications = similar.map {
                SimilarApplicationSummary(
                    applicationId = it.candidate.id,
                    role = it.candidate.role,
                    status = it.candidate.status,
                    score = it.score,
                    reason = it.reason,
                )
            },
        )
    }

    /** Loads same-company and same-URL applications and runs [PostingSimilarity] against them. */
    private fun findSimilarApplications(
        company: String,
        role: String,
        url: String?,
    ): List<PostingSimilarity.Match> {
        val apps = BackendService.remoteDatabase.collection("applications")
        val companyId = DocumentIdGenerator.slug(company)
        val docs = buildList {
            if (companyId.isNotEmpty()) addAll(apps.whereEqualTo("companyId", companyId).get().get().documents)
            if (!url.isNullOrBlank()) addAll(apps.whereEqualTo("jobUrl", url).get().get().documents)
        }.distinctBy { it.id }
        val candidates = docs.map { doc ->
            PostingSimilarity.Candidate(
                id = doc.id,
                companyId = doc.getString("companyId").orEmpty(),
                role = doc.getString("role").orEmpty(),
                jobUrl = doc.getString("jobUrl"),
                status = doc.getString("status").orEmpty(),
            )
        }
        return PostingSimilarity.findSimilar(company, role, url, candidates)
    }

    /**
     * Runs Tier 2 AI semantic ranking calibration and updates Firestore fit metadata.
     */
    public suspend fun evaluateWithAi(
        request: AiRankingRequest,
        applicationId: String?,
        aiRankingService: AiRankingService = AiClientFactory.createAiRankingService(),
    ): AiRankingResponse {
        val aiResponse = aiRankingService(request).getOrThrow()
        if (!applicationId.isNullOrBlank()) {
            runCatching {
                val db = BackendService.remoteDatabase
                db.collection("applications").document(applicationId).set(
                    mapOf(
                        "fitRating" to aiResponse.score,
                        "fitEvaluation" to mapOf(
                            "score" to aiResponse.score,
                            "verdict" to aiResponse.verdict,
                            "aiRationale" to aiResponse.aiRationale,
                            "isAiEvaluated" to true,
                        ),
                        "updatedAt" to Timestamp.now(),
                    ),
                    SetOptions.merge(),
                ).get()
            }
        }
        return aiResponse
    }

    /**
     * Starts asynchronous tailored CV, cover letter, and outcome artifact drafting.
     */
    public fun startArtifactGenerationJob(
        applicationId: String,
        company: String,
        role: String,
        postingText: String,
        draftingService: ArtifactDraftingService = AiClientFactory.createArtifactDraftingService(),
        compilerService: DocumentCompilerService = DocumentCompilerService(),
        persistToRemote: Boolean = true,
        outputRootDir: File = artifactsRootDir,
    ): ArtifactJobAcceptedResponse {
        val jobId = "job_art_${UUID.randomUUID()}"
        val nowIso = java.time.Instant.now().toString()
        val initialSnapshot = ArtifactJobSnapshot(
            jobId = jobId,
            applicationId = applicationId,
            status = "IN_PROGRESS",
            updatedAt = nowIso,
        )
        jobs[jobId] = initialSnapshot

        if (persistToRemote) {
            runCatching {
                transitionApplicationStatus(
                    applicationId = applicationId,
                    newStatus = "DRAFTING",
                    note = "Started background tailored CV and cover letter drafting ($jobId).",
                )
                writeArtifactJobDocument(initialSnapshot)
            }
        }

        backgroundScope.launch {
            executeArtifactJob(
                jobId = jobId,
                applicationId = applicationId,
                company = company,
                role = role,
                postingText = postingText,
                draftingService = draftingService,
                compilerService = compilerService,
                persistToRemote = persistToRemote,
                outputRootDir = outputRootDir,
            )
        }

        return ArtifactJobAcceptedResponse(
            jobId = jobId,
            status = "DRAFTING",
            applicationId = applicationId,
        )
    }

    /**
     * Executes tailored CV, cover letter, and outcome artifact generation synchronously and optionally persists to Firestore.
     */
    public suspend fun executeArtifactJob(
        jobId: String = "job_art_${UUID.randomUUID()}",
        applicationId: String,
        company: String,
        role: String,
        postingText: String,
        draftingService: ArtifactDraftingService = AiClientFactory.createArtifactDraftingService(),
        compilerService: DocumentCompilerService = DocumentCompilerService(),
        persistToRemote: Boolean = true,
        outputRootDir: File = artifactsRootDir,
    ): ArtifactJobSnapshot {
        if (persistToRemote) {
            runCatching {
                transitionApplicationStatus(
                    applicationId = applicationId,
                    newStatus = "DRAFTING",
                    note = "Tailoring CV and cover letter sources ($jobId).",
                )
            }
        }
        val draftedResult = draftingService(
            DraftingRequest(
                applicationId = applicationId,
                company = company,
                role = role,
                postingText = postingText,
            )
        )

        val dateDir = LocalDate.now().toString()
        val companySlug = DocumentIdGenerator.slug(company, "company")
        val roleSlug = DocumentIdGenerator.slug(role, "role")
        val artifactDir = File(outputRootDir, "$companySlug/$roleSlug/$dateDir").apply { mkdirs() }
        val fileStem = "${companySlug.replace('-', '_')}_${roleSlug.replace('-', '_')}"
        val cvBaseName = "main_$fileStem"
        val coverBaseName = "cover_$fileStem"
        val outcomeContent = buildString {
            appendLine("# Job Application: $company — $role")
            appendLine()
            appendLine("**Date:** $dateDir")
            appendLine("**Application ID:** $applicationId")
            appendLine()
            appendLine("## Job Posting Summary")
            appendLine(postingText.take(2000))
        }.trimEnd()

        var writtenCvTex = ""
        var writtenCoverTex = ""

        val finalSnapshot = draftedResult.fold(
            onSuccess = { drafted ->
                writtenCvTex = drafted.cvTex
                writtenCoverTex = drafted.coverLetterTex
                val cvTexFile = File(artifactDir, "$cvBaseName.tex").apply { writeText(drafted.cvTex) }
                val coverTexFile = File(artifactDir, "$coverBaseName.tex").apply { writeText(drafted.coverLetterTex) }
                val outcomeFile = File(artifactDir, "outcome.md").apply { writeText(outcomeContent) }

                val cvCompile = runCatching {
                    compilerService.compileLatex(drafted.cvTex, "$cvBaseName.pdf").first()
                }.getOrNull()
                val coverCompile = runCatching {
                    compilerService.compileLatex(drafted.coverLetterTex, "$coverBaseName.pdf").first()
                }.getOrNull()

                val cvDest = if (cvCompile?.success == true && !cvCompile.artifactUrl.isNullOrBlank()) {
                    val out = File(artifactDir, "$cvBaseName.pdf")
                    runCatching { File(cvCompile.artifactUrl!!).copyTo(out, overwrite = true) }
                    out.path
                } else {
                    cvTexFile.path
                }

                val coverDest = if (coverCompile?.success == true && !coverCompile.artifactUrl.isNullOrBlank()) {
                    val out = File(artifactDir, "$coverBaseName.pdf")
                    runCatching { File(coverCompile.artifactUrl!!).copyTo(out, overwrite = true) }
                    out.path
                } else {
                    coverTexFile.path
                }

                ArtifactJobSnapshot(
                    jobId = jobId,
                    applicationId = applicationId,
                    status = "SUCCESS",
                    cvArtifactId = "cv_$fileStem",
                    cvPdfUrl = cvDest,
                    coverLetterArtifactId = "cover_$fileStem",
                    coverLetterPdfUrl = coverDest,
                    outcomeArtifactId = "outcome_$fileStem",
                    outcomeUrl = outcomeFile.path,
                    updatedAt = java.time.Instant.now().toString(),
                )
            },
            onFailure = { err ->
                ArtifactJobSnapshot(
                    jobId = jobId,
                    applicationId = applicationId,
                    status = "FAILED",
                    error = err.message ?: "Artifact drafting failed",
                    updatedAt = java.time.Instant.now().toString(),
                )
            },
        )

        jobs[jobId] = finalSnapshot

        if (persistToRemote) {
            runCatching {
                writeArtifactJobDocument(finalSnapshot)
                if (finalSnapshot.status == "SUCCESS") {
                    val postingFile = File(artifactDir, "job-posting.md").apply { writeText(postingText) }
                    val uploadTargets = listOfNotNull(
                        finalSnapshot.cvPdfUrl, File(artifactDir, "$cvBaseName.tex").path,
                        finalSnapshot.coverLetterPdfUrl, File(artifactDir, "$coverBaseName.tex").path,
                        finalSnapshot.outcomeUrl, postingFile.path,
                    ).map(::File).filter { it.isFile }.distinctBy { it.absolutePath }
                    val remoteUrls = uploadArtifactBlobs(applicationId, uploadTargets)
                    writeArtifactSubcollectionDocuments(
                        applicationId = applicationId,
                        cvArtifactId = finalSnapshot.cvArtifactId ?: "cv_$fileStem",
                        cvFilename = "$cvBaseName.tex",
                        cvPath = finalSnapshot.cvPdfUrl ?: File(artifactDir, "$cvBaseName.tex").path,
                        cvContent = writtenCvTex,
                        coverArtifactId = finalSnapshot.coverLetterArtifactId ?: "cover_$fileStem",
                        coverFilename = "$coverBaseName.tex",
                        coverPath = finalSnapshot.coverLetterPdfUrl ?: File(artifactDir, "$coverBaseName.tex").path,
                        coverContent = writtenCoverTex,
                        outcomeArtifactId = finalSnapshot.outcomeArtifactId ?: "outcome_$fileStem",
                        outcomePath = finalSnapshot.outcomeUrl ?: File(artifactDir, "outcome.md").path,
                        outcomeContent = outcomeContent,
                        postingPath = postingFile.path,
                        postingContent = postingText,
                        remoteUrls = remoteUrls,
                    )
                    // DRAFTED only once every artifact lives on the remote, bound to this record.
                    if (remoteUrls.size == uploadTargets.size) {
                        transitionApplicationStatus(
                            applicationId = applicationId,
                            newStatus = "DRAFTED",
                            note = "Tailored CV and cover letter drafted; ${remoteUrls.size} artifacts uploaded ($jobId).",
                        )
                    } else {
                        transitionApplicationStatus(
                            applicationId = applicationId,
                            newStatus = "DRAFTING",
                            note = "Remained DRAFTING: uploaded ${remoteUrls.size}/${uploadTargets.size} artifacts ($jobId).",
                        )
                    }
                }
            }
        }

        return finalSnapshot
    }

    /** Uploads files to blob storage under `artifacts/<applicationId>/`; returns filename -> remote URL for successes. */
    private fun uploadArtifactBlobs(
        applicationId: String,
        files: List<File>,
    ): Map<String, String> {
        val bucketName = System.getenv("TRACKER_BUCKET") ?: "dev-fnc314-ai-job-tracker-artifacts"
        val bucket = runCatching {
            com.google.firebase.cloud.StorageClient.getInstance().bucket(bucketName)
        }.getOrNull() ?: return emptyMap()
        return files.mapNotNull { file ->
            runCatching {
                val blobPath = "artifacts/$applicationId/${file.name}"
                bucket.create(blobPath, file.readBytes(), contentTypeFor(file.name))
                file.name to "gs://$bucketName/$blobPath"
            }.getOrNull()
        }.toMap()
    }

    private fun contentTypeFor(name: String): String = when (name.substringAfterLast('.').lowercase()) {
        "pdf" -> "application/pdf"
        "md" -> "text/markdown"
        "tex" -> "text/x-tex"
        else -> "application/octet-stream"
    }

    private fun writeArtifactSubcollectionDocuments(
        applicationId: String,
        cvArtifactId: String,
        cvFilename: String,
        cvPath: String,
        cvContent: String,
        coverArtifactId: String,
        coverFilename: String,
        coverPath: String,
        coverContent: String,
        outcomeArtifactId: String,
        outcomePath: String,
        outcomeContent: String,
        postingPath: String,
        postingContent: String,
        remoteUrls: Map<String, String>,
    ) {
        val db = BackendService.remoteDatabase
        val now = Timestamp.now()
        val artifactsCol = db.collection("applications").document(applicationId).collection("artifacts")
        val entries = listOf(
            mapOf(
                "artifactId" to cvArtifactId,
                "applicationId" to applicationId,
                "kind" to "artifact:cv",
                "filename" to cvFilename,
                "contentType" to "text/x-tex",
                "localPath" to cvPath,
                "content" to cvContent,
                "size" to cvContent.length.toLong(),
                "createdAt" to now,
            ),
            mapOf(
                "artifactId" to coverArtifactId,
                "applicationId" to applicationId,
                "kind" to "artifact:cover-letter",
                "filename" to coverFilename,
                "contentType" to "text/x-tex",
                "localPath" to coverPath,
                "content" to coverContent,
                "size" to coverContent.length.toLong(),
                "createdAt" to now,
            ),
            mapOf(
                "artifactId" to outcomeArtifactId,
                "applicationId" to applicationId,
                "kind" to "artifact:outcome",
                "filename" to "outcome.md",
                "contentType" to "text/markdown",
                "localPath" to outcomePath,
                "content" to outcomeContent,
                "size" to outcomeContent.length.toLong(),
                "createdAt" to now,
            ),
            mapOf(
                "artifactId" to "posting_$applicationId",
                "applicationId" to applicationId,
                "kind" to "artifact:posting",
                "filename" to "job-posting.md",
                "contentType" to "text/markdown",
                "localPath" to postingPath,
                "content" to postingContent,
                "size" to postingContent.length.toLong(),
                "createdAt" to now,
            ),
        )
        entries.forEach { data ->
            val id = data["artifactId"] as String
            val localName = File(data["localPath"] as String).name
            val pdfName = localName.substringBeforeLast('.') + ".pdf"
            val enriched: MutableMap<String, Any> = data.toMutableMap()
            (remoteUrls[localName] ?: remoteUrls[data["filename"] as String])?.let { enriched["remoteUrl"] = it }
            remoteUrls[pdfName]?.let { enriched["pdfRemoteUrl"] = it }
            artifactsCol.document(id).set(enriched as Map<String, Any>, SetOptions.merge()).get()
        }
    }

    /**
     * Retrieves the live or persisted snapshot for a background artifact drafting job.
     */
    public fun getJobSnapshot(jobId: String): ArtifactJobSnapshot? {
        jobs[jobId]?.let { return it }
        return runCatching {
            val doc = BackendService.remoteDatabase.collection("artifact_jobs").document(jobId).get().get()
            if (!doc.exists()) return null
            val data = doc.data ?: return null
            ArtifactJobSnapshot(
                jobId = data["jobId"] as? String ?: jobId,
                applicationId = data["applicationId"] as? String ?: "",
                status = data["status"] as? String ?: "PENDING",
                cvArtifactId = data["cvArtifactId"] as? String,
                cvPdfUrl = data["cvPdfUrl"] as? String,
                coverLetterArtifactId = data["coverLetterArtifactId"] as? String,
                coverLetterPdfUrl = data["coverLetterPdfUrl"] as? String,
                outcomeArtifactId = data["outcomeArtifactId"] as? String,
                outcomeUrl = data["outcomeUrl"] as? String,
                error = data["error"] as? String,
                updatedAt = data["updatedAt"] as? String ?: java.time.Instant.now().toString(),
            )
        }.getOrNull()
    }

    private fun persistRankedApplication(
        applicationId: String,
        company: String,
        role: String,
        url: String?,
        location: String?,
        result: RankResult,
    ) {
        val db = BackendService.remoteDatabase
        val now = Timestamp.now()
        val companyId = DocumentIdGenerator.slug(company, "unknown-company")
        val docRef = db.collection("applications").document(applicationId)
        val snap = docRef.get().get()
        if (!snap.exists()) {
            val payload = mutableMapOf<String, Any>(
                "id" to applicationId,
                "company" to company,
                "companyId" to companyId,
                "role" to role,
                "status" to "RANKED",
                "statusRank" to 4,
                "isOpen" to true,
                "fitRating" to result.overall,
                "schemaVersion" to 9,
                "emailMessageIds" to emptyList<String>(),
                "createdAt" to now,
                "updatedAt" to now,
            )
            if (!url.isNullOrBlank()) {
                payload["jobUrl"] = url
            }
            if (!location.isNullOrBlank()) {
                payload["location"] = mapOf("raw" to location)
            }
            docRef.set(payload, SetOptions.merge()).get()
            db.collection("companies").document(companyId).set(
                mapOf(
                    "companyId" to companyId,
                    "name" to company,
                    "updatedAt" to now,
                ),
                SetOptions.merge(),
            ).get()
        }
        // Never regress a record that has already moved past ranking (e.g. DRAFTED, SUBMITTED).
        val priorStatus = if (snap.exists()) snap.getString("status").orEmpty() else ""
        val rankable = priorStatus.isBlank() ||
            StatusNormalizer.normalizeStatus(priorStatus) in setOf(StatusNormalizer.UNSPECIFIED_STATUS, "RANKED")
        if (!rankable) {
            docRef.set(mapOf("fitRating" to result.overall, "updatedAt" to now), SetOptions.merge()).get()
            return
        }
        if (snap.exists()) {
            docRef.set(mapOf("status" to "RANKED", "fitRating" to result.overall, "updatedAt" to now), SetOptions.merge()).get()
        }
        BackendService.appendEventTransactional(
            applicationId = applicationId,
            eventPayload = mapOf(
                "type" to "RECALCULATE_FIT",
                "actor" to "AGENT",
                "statusTo" to "RANKED",
                "note" to "Evaluated via RankScorer: ${result.overall}/100 (${result.verdict})",
            ),
            idempotencyKey = "rank-$applicationId-${result.overall}",
        )
    }

    private fun transitionApplicationStatus(
        applicationId: String,
        newStatus: String,
        note: String,
        extraFields: Map<String, Any?> = emptyMap(),
    ) {
        val db = BackendService.remoteDatabase
        val appRef = db.collection("applications").document(applicationId)
        val updateMap = mutableMapOf<String, Any?>(
            "status" to newStatus,
            "updatedAt" to Timestamp.now(),
        )
        updateMap.putAll(extraFields)
        appRef.set(updateMap, SetOptions.merge()).get()

        BackendService.appendEventTransactional(
            applicationId = applicationId,
            eventPayload = mapOf(
                "type" to "CORRECT_STATUS",
                "actor" to "AGENT",
                "statusTo" to newStatus,
                "note" to note,
            ),
        )
    }

    /**
     * Transitions the application status to `SUBMITTED` and appends an `APPLY` event.
     */
    public fun markApplicationSubmitted(
        applicationId: String,
        note: String = "Application submitted via apply pipeline",
    ): Unit {
        val db = BackendService.remoteDatabase
        db.collection("applications").document(applicationId).set(
            mapOf(
                "status" to "SUBMITTED",
                "statusRank" to 5,
                "isOpen" to true,
                "updatedAt" to Timestamp.now(),
            ),
            SetOptions.merge(),
        ).get()
        BackendService.appendEventTransactional(
            applicationId = applicationId,
            eventPayload = mapOf(
                "type" to "APPLY",
                "actor" to "USER",
                "statusTo" to "SUBMITTED",
                "note" to note,
            ),
        )
    }

    private fun writeArtifactJobDocument(snapshot: ArtifactJobSnapshot) {
        val db = BackendService.remoteDatabase
        val data = buildMap<String, Any?> {
            put("jobId", snapshot.jobId)
            put("applicationId", snapshot.applicationId)
            put("status", snapshot.status)
            put("cvArtifactId", snapshot.cvArtifactId)
            put("cvPdfUrl", snapshot.cvPdfUrl)
            put("coverLetterArtifactId", snapshot.coverLetterArtifactId)
            put("coverLetterPdfUrl", snapshot.coverLetterPdfUrl)
            put("outcomeArtifactId", snapshot.outcomeArtifactId)
            put("outcomeUrl", snapshot.outcomeUrl)
            put("error", snapshot.error)
            put("updatedAt", snapshot.updatedAt)
        }
        db.collection("artifact_jobs").document(snapshot.jobId).set(data, SetOptions.merge()).get()
    }
}
