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

        val category = data["category"]?.jsonObject
        val sector = category?.get("sector")?.jsonPrimitive?.contentOrNull
        val industry = category?.get("industry")?.jsonPrimitive?.contentOrNull
        val subIndustry = category?.get("subIndustry")?.jsonPrimitive?.contentOrNull

        val geo = data["geo"]?.jsonObject
        val city = geo?.get("city")?.jsonPrimitive?.contentOrNull
        val state = geo?.get("state")?.jsonPrimitive?.contentOrNull
        val country = geo?.get("country")?.jsonPrimitive?.contentOrNull

        val metrics = data["metrics"]?.jsonObject
        val employees = metrics?.get("employees")?.jsonPrimitive?.contentOrNull
        val employeesCount = metrics?.get("employeesCount")?.jsonPrimitive?.contentOrNull?.toInt()
        val raised = metrics?.get("raised")?.jsonPrimitive?.contentOrNull?.toLong()

        val linkedinHandle = data["linkedin"]?.jsonObject?.get("handle")?.jsonPrimitive?.contentOrNull
        val twitterHandle = data["twitter"]?.jsonObject?.get("handle")?.jsonPrimitive?.contentOrNull
        val crunchbaseHandle = data["crunchbase"]?.jsonObject?.get("handle")?.jsonPrimitive?.contentOrNull

        val tags = data["tags"]?.jsonArray?.map { it.jsonPrimitive.content }

        return HunterCompanyInfo(
            id = data["id"]?.jsonPrimitive?.contentOrNull,
            name = data["name"]?.jsonPrimitive?.contentOrNull,
            industry = industry,
            subIndustry = subIndustry,
            sector = sector,
            linkedinHandle = linkedinHandle,
            twitterHandle = twitterHandle,
            crunchbaseHandle = crunchbaseHandle,
            logoUrl = data["logo"]?.jsonPrimitive?.contentOrNull,
            description = data["description"]?.jsonPrimitive?.contentOrNull,
            foundedYear = data["foundedYear"]?.jsonPrimitive?.contentOrNull?.toInt(),
            companyType = data["companyType"]?.jsonPrimitive?.contentOrNull,
            employees = employees,
            employeesCount = employeesCount,
            location = data["location"]?.jsonPrimitive?.contentOrNull,
            city = city,
            state = state,
            country = country,
            tags = tags,
            raised = raised
        )
    }
}
