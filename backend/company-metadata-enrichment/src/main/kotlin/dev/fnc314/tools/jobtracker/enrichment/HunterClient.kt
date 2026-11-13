package dev.fnc314.tools.jobtracker.enrichment

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Interface for Hunter.io API access.
 */
public interface HunterClient {
    /**
     * Lookup company metadata by domain.
     *
     * @param domain The company domain (e.g., "pnc.com").
     * @return Company metadata, or `null` if the API returns no data.
     */
    public fun lookup(domain: String): HunterCompanyInfo?
}

/**
 * HTTP implementation of [HunterClient] using the Hunter.io Company Enrichment API.
 *
 * @param apiKey The Hunter.io API key. If blank, lookups return `null`.
 */
public class HunterHttpClient(private val apiKey: String) : HunterClient {

    private val json = Json { ignoreUnknownKeys = true }

    override fun lookup(domain: String): HunterCompanyInfo? {
        if (apiKey.isBlank()) return null

        val cleanDomain = domain.removePrefix("https://").removePrefix("http://")
            .removePrefix("www.").split("/").first()

        val apiUrl = "https://api.hunter.io/v2/companies/find?domain=$cleanDomain&api_key=$apiKey"
        val connection = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 10000

        return try {
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                parseHunterResponse(response)
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseHunterResponse(response: String): HunterCompanyInfo? {
        val jsonElement = json.parseToJsonElement(response)
        val data = jsonElement.jsonObject["data"]?.jsonObject ?: return null

        fun String.orNull(): String? = if (isNullOrEmpty()) null else this

        val category = data["category"]?.jsonObject
        val sector = category?.get("sector")?.jsonPrimitive?.content?.orNull()
        val industry = category?.get("industry")?.jsonPrimitive?.content?.orNull()
        val subIndustry = category?.get("subIndustry")?.jsonPrimitive?.content?.orNull()

        val geo = data["geo"]?.jsonObject
        val city = geo?.get("city")?.jsonPrimitive?.content?.orNull()
        val state = geo?.get("state")?.jsonPrimitive?.content?.orNull()
        val country = geo?.get("country")?.jsonPrimitive?.content?.orNull()

        val metrics = data["metrics"]?.jsonObject
        val employees = metrics?.get("employees")?.jsonPrimitive?.content?.orNull()
        val employeesCount = metrics?.get("employeesCount")?.jsonPrimitive?.content?.toInt()
        val raised = metrics?.get("raised")?.jsonPrimitive?.content?.toLong()

        val linkedinHandle = data["linkedin"]?.jsonObject?.get("handle")?.jsonPrimitive?.content?.orNull()
        val twitterHandle = data["twitter"]?.jsonObject?.get("handle")?.jsonPrimitive?.content?.orNull()
        val crunchbaseHandle = data["crunchbase"]?.jsonObject?.get("handle")?.jsonPrimitive?.content?.orNull()

        val tags = data["tags"]?.jsonArray?.map { it.jsonPrimitive.content }

        return HunterCompanyInfo(
            id = data["id"]?.jsonPrimitive?.content?.orNull(),
            name = data["name"]?.jsonPrimitive?.content?.orNull(),
            industry = industry,
            subIndustry = subIndustry,
            sector = sector,
            linkedinHandle = linkedinHandle,
            twitterHandle = twitterHandle,
            crunchbaseHandle = crunchbaseHandle,
            logoUrl = data["logo"]?.jsonPrimitive?.content?.orNull(),
            description = data["description"]?.jsonPrimitive?.content?.orNull(),
            foundedYear = data["foundedYear"]?.jsonPrimitive?.content?.toInt(),
            companyType = data["companyType"]?.jsonPrimitive?.content?.orNull(),
            employees = employees,
            employeesCount = employeesCount,
            location = data["location"]?.jsonPrimitive?.content?.orNull(),
            city = city,
            state = state,
            country = country,
            tags = tags,
            raised = raised
        )
    }
}
