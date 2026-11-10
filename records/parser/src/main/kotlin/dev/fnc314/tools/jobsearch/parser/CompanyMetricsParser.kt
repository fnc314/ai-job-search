package dev.fnc314.tools.jobsearch.parser

import com.google.protobuf.Timestamp
import dev.fnc314.tools.jobsearch.tracker.v1.ApplicationChannel
import dev.fnc314.tools.jobsearch.tracker.v1.CompanyMetrics

/**
 * Serializer and deserializer mapping between [CompanyMetrics] proto3 messages
 * and Firestore/JSON document maps.
 */
public object CompanyMetricsParser {

    /**
     * Parses a raw map (from Firestore or JSON) into a strongly-typed [CompanyMetrics] proto message.
     */
    @Suppress("UNCHECKED_CAST")
    public fun fromMap(map: Map<String, Any?>): CompanyMetrics {
        val builder = CompanyMetrics.newBuilder()

        val countsMap = map["counts"] as? Map<String, Any?>
        if (countsMap != null) {
            val countsBuilder = CompanyMetrics.Counts.newBuilder()
            (countsMap["draft"] as? Number)?.let { countsBuilder.draft = it.toInt() }
            (countsMap["submitted"] as? Number)?.let { countsBuilder.submitted = it.toInt() }
            (countsMap["interviewing"] as? Number)?.let { countsBuilder.interviewing = it.toInt() }
            (countsMap["offer"] as? Number)?.let { countsBuilder.offer = it.toInt() }
            (countsMap["rejected"] as? Number)?.let { countsBuilder.rejected = it.toInt() }
            (countsMap["total"] as? Number)?.let { countsBuilder.total = it.toInt() }
            builder.counts = countsBuilder.build()
        }

        val percentagesMap = map["percentages"] as? Map<String, Any?>
        if (percentagesMap != null) {
            val percentagesBuilder = CompanyMetrics.Percentages.newBuilder()
            (percentagesMap["draft"] as? Number)?.let { percentagesBuilder.draft = it.toFloat() }
            (percentagesMap["submitted"] as? Number)?.let { percentagesBuilder.submitted = it.toFloat() }
            (percentagesMap["interviewing"] as? Number)?.let { percentagesBuilder.interviewing = it.toFloat() }
            (percentagesMap["offer"] as? Number)?.let { percentagesBuilder.offer = it.toFloat() }
            (percentagesMap["rejected"] as? Number)?.let { percentagesBuilder.rejected = it.toFloat() }
            builder.percentages = percentagesBuilder.build()
        }

        (map["averageFitScore"] as? Number)?.let {
            builder.averageFitScore = it.toInt()
        }

        val salaryRangeMap = map["salaryRange"] as? Map<String, Any?>
        if (salaryRangeMap != null) {
            val salaryBuilder = CompanyMetrics.SalaryRange.newBuilder()
            (salaryRangeMap["min"] as? Number)?.let { salaryBuilder.min = it.toDouble() }
            (salaryRangeMap["max"] as? Number)?.let { salaryBuilder.max = it.toDouble() }
            (salaryRangeMap["currency"] as? String)?.let { salaryBuilder.currency = it }
            (salaryRangeMap["display"] as? String)?.let { salaryBuilder.display = it }
            builder.salaryRange = salaryBuilder.build()
        }

        val primaryChannelStr = map["primaryChannel"] as? String
        if (!primaryChannelStr.isNullOrBlank()) {
            val channel = try {
                ApplicationChannel.valueOf(primaryChannelStr.uppercase())
            } catch (_: IllegalArgumentException) {
                ApplicationChannel.APPLICATION_CHANNEL_UNSPECIFIED
            }
            builder.primaryChannel = channel
        }

        val locationsList = (map["locations"] as? List<*>)?.filterIsInstance<String>()
        if (locationsList != null) {
            builder.addAllLocations(locationsList)
        }

        val lastCalc = map["lastCalculatedAt"]
        when (lastCalc) {
            is String -> {
                try {
                    builder.lastCalculatedAt = TimestampConverter.isoToTimestamp(lastCalc)
                } catch (_: Exception) {
                    // Ignore unparseable date
                }
            }
            is Timestamp -> {
                builder.lastCalculatedAt = lastCalc
            }
        }

        return builder.build()
    }

    /**
     * Serializes a [CompanyMetrics] proto message into a map matching tracker.schema.json.
     */
    public fun toMap(metrics: CompanyMetrics): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        if (metrics.hasCounts()) {
            val counts = metrics.counts
            result["counts"] = mapOf(
                "draft" to counts.draft,
                "submitted" to counts.submitted,
                "interviewing" to counts.interviewing,
                "offer" to counts.offer,
                "rejected" to counts.rejected,
                "total" to counts.total
            )
        }

        if (metrics.hasPercentages()) {
            val percentages = metrics.percentages
            result["percentages"] = mapOf(
                "draft" to percentages.draft,
                "submitted" to percentages.submitted,
                "interviewing" to percentages.interviewing,
                "offer" to percentages.offer,
                "rejected" to percentages.rejected
            )
        }

        if (metrics.hasAverageFitScore()) {
            result["averageFitScore"] = metrics.averageFitScore
        }

        if (metrics.hasSalaryRange()) {
            val salary = metrics.salaryRange
            val salaryMap = mutableMapOf<String, Any>()
            if (salary.hasMin()) salaryMap["min"] = salary.min
            if (salary.hasMax()) salaryMap["max"] = salary.max
            if (salary.currency.isNotBlank()) salaryMap["currency"] = salary.currency
            salaryMap["display"] = salary.display
            result["salaryRange"] = salaryMap
        }

        if (metrics.primaryChannel != ApplicationChannel.APPLICATION_CHANNEL_UNSPECIFIED &&
            metrics.primaryChannel != ApplicationChannel.UNRECOGNIZED) {
            result["primaryChannel"] = metrics.primaryChannel.name
        }

        if (metrics.locationsCount > 0) {
            result["locations"] = metrics.locationsList
        }

        if (metrics.hasLastCalculatedAt()) {
            result["lastCalculatedAt"] = TimestampConverter.timestampToIso(metrics.lastCalculatedAt)
        }

        return result
    }
}
