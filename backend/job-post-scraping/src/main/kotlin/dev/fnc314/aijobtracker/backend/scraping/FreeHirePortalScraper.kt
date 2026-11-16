package dev.fnc314.aijobtracker.backend.scraping

import dev.fnc314.aijobtracker.backend.scraping.model.ScrapedJobPosting
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Native Kotlin HTTP scraper for the public FreeHire aggregator API.
 */
public class FreeHirePortalScraper(
    private val client: HttpClient = HttpClient(CIO)
) : PortalScraper {

    override fun invoke(query: String, location: String): Flow<ScrapedJobPosting> = flow {
        try {
            val response = client.get("https://freehire.me/api/jobs") {
                parameter("q", query)
                if (location.isNotBlank()) {
                    parameter("location", location)
                }
            }

            val responseBody = response.bodyAsText()
            val jsonArray = Json.parseToJsonElement(responseBody).jsonArray

            for (element in jsonArray) {
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: ""
                val title = obj["title"]?.jsonPrimitive?.content ?: ""
                val company = obj["company"]?.jsonPrimitive?.content ?: ""
                val loc = obj["location"]?.jsonPrimitive?.content ?: location
                val url = obj["url"]?.jsonPrimitive?.content ?: ""
                val description = obj["description"]?.jsonPrimitive?.content ?: ""

                if (title.isNotBlank()) {
                    emit(
                        ScrapedJobPosting(
                            id = id.ifBlank { url.hashCode().toString() },
                            portal = "FreeHire",
                            title = title,
                            company = company,
                            location = loc,
                            url = url,
                            description = description
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("FreeHire scraper error: ${e.message}")
        }
    }
}
