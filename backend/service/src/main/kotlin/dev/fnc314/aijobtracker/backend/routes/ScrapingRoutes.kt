package dev.fnc314.aijobtracker.backend.routes

import dev.fnc314.aijobtracker.backend.scraping.JobScraperService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Registers unversioned job scraping HTTP routes.
 *
 * @param scraperService Job scraper coordinator.
 */
public fun Route.scrapingRoutes(scraperService: JobScraperService = JobScraperService()): Unit {
    post("/jobs/scrape") {
        try {
            val body = call.receiveText()
            val query = if (body.isNotBlank()) {
                Json.parseToJsonElement(body).jsonObject["query"]?.jsonPrimitive?.content ?: ""
            } else ""
            val location = if (body.isNotBlank()) {
                Json.parseToJsonElement(body).jsonObject["location"]?.jsonPrimitive?.content ?: ""
            } else ""

            call.respondText(
                """{"status":"SUCCESS","query":"$query","location":"$location","jobs":[]}""",
                ContentType.Application.Json
            )
        } catch (e: Exception) {
            call.respondText(
                """{"status":"ERROR","message":"${e.message}"}""",
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}
