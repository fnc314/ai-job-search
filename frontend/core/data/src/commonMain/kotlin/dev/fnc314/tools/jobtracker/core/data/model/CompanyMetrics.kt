package dev.fnc314.tools.jobtracker.core.data.model

import dev.fnc314.tools.jobtracker.core.data.util.InstantParser
import kotlin.time.Instant

/**
 * Pre-aggregated metrics for an employer across all job applications.
 *
 * Stored in Firestore document `companies/{companyId}.metrics` to guarantee O(1) reads
 * and zero cross-collection joining at UI render time.
 *
 * @property counts Application count breakdowns across funnel states.
 * @property percentages Application percentage breakdowns across funnel states.
 * @property salaryRange Aggregated compensation range offered by employer.
 * @property averageFitScore Average candidate fit score across applications.
 * @property primaryChannel Most frequently used submission channel.
 * @property locations Unique geographic locations where roles were posted.
 * @property lastCalculatedAt Timestamp when metrics were last aggregated.
 * @property lastApplicationSubmittedAt Timestamp of most recent application submission.
 */
public data class CompanyMetrics(
    public val counts: ApplicationCounts = ApplicationCounts(),
    public val percentages: ApplicationPercentages = ApplicationPercentages(),
    public val salaryRange: CompanySalaryRange = CompanySalaryRange(),
    public val averageFitScore: Double = 0.0,
    public val primaryChannel: ApplicationChannel = ApplicationChannel.DEFAULT,
    public val locations: List<String> = emptyList(),
    public val lastCalculatedAt: Instant = Instant.DISTANT_PAST,
    public val lastApplicationSubmittedAt: Instant? = null,
) {
    /**
     * Application counts broken down by funnel lifecycle stages.
     *
     * @property total Total applications tracked for employer.
     * @property draft Applications in draft state.
     * @property submitted Applications submitted and awaiting response.
     * @property interview Applications actively interviewing.
     * @property offer Applications with formal offers extended.
     * @property rejected Applications rejected or closed.
     */
    public data class ApplicationCounts(
        public val total: Int = 0,
        public val draft: Int = 0,
        public val submitted: Int = 0,
        public val interview: Int = 0,
        public val offer: Int = 0,
        public val rejected: Int = 0,
    )

    /**
     * Funnel stage percentage breakdown for employer.
     *
     * @property draft Percentage of applications in drafting.
     * @property submitted Percentage of applications submitted.
     * @property interview Percentage of applications interviewing.
     * @property offer Percentage of applications resulting in offers.
     * @property rejected Percentage of applications rejected.
     */
    public data class ApplicationPercentages(
        public val draft: Float = 0f,
        public val submitted: Float = 0f,
        public val interview: Float = 0f,
        public val offer: Float = 0f,
        public val rejected: Float = 0f,
    )

    /**
     * Aggregated salary range spanning all roles tracked for an employer.
     *
     * @property min Minimum salary offered.
     * @property max Maximum salary offered.
     * @property currency ISO currency symbol or code.
     * @property display Human-readable formatted compensation range.
     */
    public data class CompanySalaryRange(
        public val min: Double = 0.0,
        public val max: Double = 0.0,
        public val currency: String = "USD",
        public val display: String = "",
    )

    /**
     * Serialization and parsing utilities for [CompanyMetrics].
     */
    public companion object {
        /**
         * Safely parses a [CompanyMetrics] instance from a Firestore document map representation.
         */
        @Suppress("CyclomaticComplexMethod")
        public fun fromMap(map: Map<*, *>?): CompanyMetrics? {
            if (map == null) return null
            val countsMap = map["counts"] as? Map<*, *>
            val counts = ApplicationCounts(
                total = (countsMap?.get("total") as? Number)?.toInt() ?: 0,
                draft = (countsMap?.get("draft") as? Number)?.toInt() ?: 0,
                submitted = (countsMap?.get("submitted") as? Number)?.toInt() ?: 0,
                interview = (countsMap?.get("interview") as? Number)?.toInt() ?: 0,
                offer = (countsMap?.get("offer") as? Number)?.toInt() ?: 0,
                rejected = (countsMap?.get("rejected") as? Number)?.toInt() ?: 0,
            )

            val percMap = map["percentages"] as? Map<*, *>
            val percentages = ApplicationPercentages(
                draft = (percMap?.get("draft") as? Number)?.toFloat() ?: 0f,
                submitted = (percMap?.get("submitted") as? Number)?.toFloat() ?: 0f,
                interview = (percMap?.get("interview") as? Number)?.toFloat() ?: 0f,
                offer = (percMap?.get("offer") as? Number)?.toFloat() ?: 0f,
                rejected = (percMap?.get("rejected") as? Number)?.toFloat() ?: 0f,
            )

            val salaryMap = (map["salaryRange"] ?: map["salary_range"]) as? Map<*, *>
            val salaryRange = CompanySalaryRange(
                min = (salaryMap?.get("min") as? Number)?.toDouble() ?: 0.0,
                max = (salaryMap?.get("max") as? Number)?.toDouble() ?: 0.0,
                currency = salaryMap?.get("currency") as? String ?: "USD",
                display = salaryMap?.get("display") as? String ?: "",
            )

            val avgFit = ((map["averageFitScore"] ?: map["average_fit_score"]) as? Number)?.toDouble() ?: 0.0
            val channelStr = (map["primaryChannel"] ?: map["primary_channel"]) as? String
            val primaryChannel = ApplicationChannel.fromWireKey(channelStr)

            val locsList = ((map["locations"] as? List<*>)?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }) ?: emptyList()
            val lastCalc = InstantParser.parse(
                map["lastCalculatedAt"] ?: map["last_calculated_at"]
            )
            val lastSubmittedRaw = map["lastApplicationSubmittedAt"]
                ?: map["last_application_submitted_at"]
                ?: map["lastSubmittedAt"]
                ?: map["last_submitted_at"]
            val lastSubmitted = if (lastSubmittedRaw != null) {
                InstantParser.parse(lastSubmittedRaw).takeUnless { it == Instant.DISTANT_PAST }
            } else {
                null
            }

            return CompanyMetrics(
                counts = counts,
                percentages = percentages,
                salaryRange = salaryRange,
                averageFitScore = avgFit,
                primaryChannel = primaryChannel,
                locations = locsList,
                lastCalculatedAt = lastCalc,
                lastApplicationSubmittedAt = lastSubmitted,
            )
        }
    }
}
