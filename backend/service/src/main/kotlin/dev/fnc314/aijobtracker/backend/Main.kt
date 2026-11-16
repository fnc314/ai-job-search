package dev.fnc314.aijobtracker.backend

import dev.fnc314.aijobtracker.backend.routes.applicationRoutes
import dev.fnc314.aijobtracker.backend.routes.compilerRoutes
import dev.fnc314.aijobtracker.backend.routes.rankingRoutes
import dev.fnc314.aijobtracker.backend.routes.scrapingRoutes
import dev.fnc314.aijobtracker.backend.security.ApiKeyAuthPlugin
import dev.fnc314.aijobtracker.backend.webhook.emailWebhookRoute
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.system.exitProcess
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull

/**
 * Main application entry point for the backend service.
 *
 * Hosts HTTP routing for Cloud Functions/Cloud Run, webhook ingestors, and supports
 * direct CLI invocation flags for metrics recalculation, routine sweeps, and company enrichment.
 *
 * @param args Command-line execution arguments and flag directives.
 */
public fun main(args: Array<String>): Unit {
    if (args.contains("--recalculate")) {
        val targetIndex = args.indexOf("--recalculate") + 1
        val targetCompanyId = args.getOrNull(targetIndex)?.takeIf { !it.startsWith("--") && it.isNotBlank() }
        println("Executing company metrics recalculation locally (target: ${targetCompanyId ?: "ALL"})...")
        try {
            val result = BackendService.recalculateCompanyMetrics(targetCompanyId)
            println("Recalculation complete: $result")
        } catch (e: Exception) {
            System.err.println("Error during recalculation: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
        return
    }

    if (args.contains("--enrich-batch")) {
        println("Executing batch company enrichment locally...")
        try {
            val result = BackendService.batchEnrichCompanies()
            println("Batch enrichment complete: $result")
        } catch (e: Exception) {
            System.err.println("Error during batch enrichment: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
        return
    }

    if (args.contains("--emails-backfill")) {
        val dryRun = args.contains("--dry-run")
        println("Executing emails backfill locally (dryRun: $dryRun)...")
        try {
            val result = BackendService.backfillEmails(dryRun = dryRun)
            println("Emails backfill complete: $result")
        } catch (e: Exception) {
            System.err.println("Error during emails backfill: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
        return
    }

    if (args.contains("--deduplicate-companies")) {
        val dryRun = args.contains("--dry-run")
        println("Executing companies deduplication locally (dryRun: $dryRun)...")
        try {
            val result = BackendService.deduplicateCompanies(dryRun = dryRun)
            println("Companies deduplication complete: $result")
        } catch (e: Exception) {
            System.err.println("Error during companies deduplication: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
        return
    }

    if (args.contains("--audit-companies")) {
        val dryRun = args.contains("--dry-run")
        println("Executing company audit locally (dryRun: $dryRun)...")
        try {
            val result = BackendService.auditOrphanCompanies(dryRun = dryRun)
            println("Company audit complete: $result")
        } catch (e: Exception) {
            System.err.println("Error during company audit: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
        return
    }

    if (args.contains("--migrate-potentials")) {
        val dryRun = args.contains("--dry-run")
        println("Executing zero-app migration to potentials locally (dryRun: $dryRun)...")
        try {
            val result = BackendService.migrateZeroAppToPotentials(dryRun = dryRun)
            println("Migration complete: $result")
        } catch (e: Exception) {
            System.err.println("Error during migration: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
        return
    }

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) {
        routing {
            // Public endpoint for uptime and platform health checks
            get("/health") {
                call.respondText(
                    """{"status":"UP","service":"jobtracker-backend","port":$port}""",
                    ContentType.Application.Json
                )
            }

            // Webhook ingestion route (managed with webhook signature validation)
            emailWebhookRoute()

            // Protected API surface secured by ApiKeyAuthPlugin
            route("") {
                install(ApiKeyAuthPlugin)

                scrapingRoutes()
                rankingRoutes()
                applicationRoutes()
                compilerRoutes()

                post("/applications/{id}/events") {
                val applicationId = call.parameters["id"]
                if (applicationId.isNullOrBlank()) {
                    call.respondText(
                        """{"status":"ERROR","message":"Missing applicationId parameter"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.BadRequest
                    )
                    return@post
                }

                try {
                    val bodyText = call.receiveText()
                    val jsonElement = Json.parseToJsonElement(bodyText).jsonObject
                    val idempotencyKey = call.request.headers["Idempotency-Key"]
                        ?: jsonElement["idempotencyKey"]?.jsonPrimitive?.contentOrNull

                    val payload = mutableMapOf<String, Any?>()
                    jsonElement.forEach { (k, v) ->
                        payload[k] = v.jsonPrimitive.contentOrNull
                    }

                    val result = BackendService.appendEventTransactional(
                        applicationId = applicationId,
                        eventPayload = payload,
                        idempotencyKey = idempotencyKey
                    )

                    val responseJson = Json.encodeToString(JsonObject(result.mapValues { (_, v) -> JsonPrimitive(v?.toString() ?: "") }))
                    call.respondText(
                        responseJson,
                        ContentType.Application.Json,
                        status = HttpStatusCode.OK
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/applications/{id}/align") {
                val applicationId = call.parameters["id"]
                if (applicationId.isNullOrBlank()) {
                    call.respondText(
                        """{"status":"ERROR","message":"Missing applicationId parameter"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.BadRequest
                    )
                    return@post
                }

                try {
                    val dryRun = call.request.queryParameters["dryRun"]?.toBooleanStrictOrNull() ?: false
                    val result = BackendService.alignApplication(applicationId, dryRun = dryRun)
                    val responseJson = Json.encodeToString(JsonObject(result.mapValues { (_, v) -> JsonPrimitive(v?.toString() ?: "") }))
                    call.respondText(
                        responseJson,
                        ContentType.Application.Json,
                        status = HttpStatusCode.OK
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/tasks/sweep") {
                try {
                    val result = BackendService.performRoutineSweep()
                    val jsonMap = result.mapValues { (_, v) -> JsonPrimitive(v.toString()) }
                    call.respondText(
                        Json.encodeToString(JsonObject(jsonMap)),
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/companies/enrich") {
                try {
                    val bodyText = call.receiveText()
                    val jsonElement = Json.parseToJsonElement(bodyText).jsonObject
                    val companyId = jsonElement["companyId"]?.jsonPrimitive?.contentOrNull
                    val name = jsonElement["name"]?.jsonPrimitive?.contentOrNull ?: companyId ?: "unknown"
                    val linkedinUrl = jsonElement["linkedinUrl"]?.jsonPrimitive?.contentOrNull
                    val industry = jsonElement["industry"]?.jsonPrimitive?.contentOrNull
                    val logoUrl = jsonElement["logoUrl"]?.jsonPrimitive?.contentOrNull

                    if (companyId.isNullOrBlank()) {
                        call.respondText(
                            """{"status":"ERROR","message":"Missing companyId parameter"}""",
                            ContentType.Application.Json,
                            status = HttpStatusCode.BadRequest
                        )
                        return@post
                    }

                    val result = BackendService.enrichCompany(
                        companyId = companyId,
                        companyName = name,
                        linkedinUrl = linkedinUrl,
                        industry = industry,
                        logoUrl = logoUrl
                    )

                    val responseJson = Json.encodeToString(JsonObject(result.mapValues { (_, v) -> JsonPrimitive(v?.toString() ?: "") }))
                    call.respondText(
                        responseJson,
                        ContentType.Application.Json,
                        status = HttpStatusCode.OK
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/companies/tasks/enrich/batch") {
                try {
                    val result = BackendService.batchEnrichCompanies()
                    val jsonMap = result.mapValues { (_, v) ->
                        when (v) {
                            is List<*> -> {
                                JsonArray(v.map { JsonPrimitive(it?.toString() ?: "") })
                            }
                            else -> JsonPrimitive(v?.toString() ?: "")
                        }
                    }
                    call.respondText(
                        Json.encodeToString(JsonObject(jsonMap)),
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/companies/tasks/deduplicate") {
                try {
                    val dryRun = call.request.queryParameters["dryRun"]?.toBoolean() ?: false
                    val result = BackendService.deduplicateCompanies(dryRun = dryRun)
                    val jsonMap = result.mapValues { (_, v) ->
                        when (v) {
                            is List<*> -> {
                                JsonArray(v.map { JsonPrimitive(it?.toString() ?: "") })
                            }
                            is Number -> JsonPrimitive(v)
                            is Boolean -> JsonPrimitive(v)
                            else -> JsonPrimitive(v.toString())
                        }
                    }
                    call.respondText(
                        Json.encodeToString(JsonObject(jsonMap)),
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/companies/add") {
                try {
                    val bodyText = call.receiveText()
                    val jsonElement = Json.parseToJsonElement(bodyText).jsonObject
                    val name = jsonElement["name"]?.jsonPrimitive?.contentOrNull
                    val website = jsonElement["website"]?.jsonPrimitive?.contentOrNull

                    if (name.isNullOrBlank()) {
                        call.respondText(
                            """{"status":"ERROR","message":"Missing name parameter"}""",
                            ContentType.Application.Json,
                            status = HttpStatusCode.BadRequest
                        )
                        return@post
                    }

                    val result = BackendService.addCompany(name = name, website = website)

                    val responseJson = Json.encodeToString(JsonObject(result.mapValues { (_, v) -> JsonPrimitive(v?.toString() ?: "") }))
                    call.respondText(
                        responseJson,
                        ContentType.Application.Json,
                        status = HttpStatusCode.OK
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/tasks/companies-recalculate-metrics") {
                try {
                    val targetCompanyId = call.request.queryParameters["companyId"]
                    val result = BackendService.recalculateCompanyMetrics(targetCompanyId)
                    val jsonMap = result.mapValues { (_, v) -> JsonPrimitive(v.toString()) }
                    call.respondText(
                        Json.encodeToString(JsonObject(jsonMap)),
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/tasks/email-read-inbox") {
                call.respondText(
                    """{"status":"SUCCESS","message":"Inbox scan executed"}""",
                    ContentType.Application.Json
                )
            }
            post("/tasks/email-collect") {
                call.respondText(
                    """{"status":"SUCCESS","message":"Proposals collected"}""",
                    ContentType.Application.Json
                )
            }
            post("/tasks/proposals-expire") {
                try {
                    val expired = BackendService.expireStaleProposals()
                    call.respondText(
                        """{"status":"SUCCESS","expired":$expired}""",
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
            post("/tasks/tracker-sync-due") {
                try {
                    val processed = BackendService.processDueSyncs()
                    call.respondText(
                        """{"status":"SUCCESS","processed":$processed}""",
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
            post("/emails/tasks/backfill") {
                try {
                    val dryRun = call.request.queryParameters["dryRun"]?.toBoolean() ?: false
                    val res = BackendService.backfillEmails(dryRun = dryRun)
                    call.respondText(
                        Json.encodeToString(JsonObject(res.mapValues {
                            when (val v = it.value) {
                                is Number -> JsonPrimitive(v)
                                is Boolean -> JsonPrimitive(v)
                                else -> JsonPrimitive(v.toString())
                            }
                        })),
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
            post("/emails/rectify") {
                try {
                    val body = call.receiveText()
                    val payload = Json.parseToJsonElement(body).jsonObject
                    val messageId = payload["messageId"]?.jsonPrimitive?.contentOrNull
                        ?: return@post call.respondText(
                            """{"status":"ERROR","message":"messageId is required"}""",
                            ContentType.Application.Json,
                            status = HttpStatusCode.BadRequest
                        )
                    val targetAppId = payload["applicationId"]?.jsonPrimitive?.contentOrNull
                    val status = payload["status"]?.jsonPrimitive?.contentOrNull ?: "UNCLASSIFIED"
                    val isNoise = payload["isNoise"]?.jsonPrimitive?.booleanOrNull ?: false
                    val dryRun = call.request.queryParameters["dryRun"]?.toBoolean() ?: false

                    val res = BackendService.rectifyEmail(
                        messageId = messageId,
                        targetApplicationId = targetAppId,
                        status = status,
                        isNoise = isNoise,
                        dryRun = dryRun
                    )
                    call.respondText(
                        Json.encodeToString(JsonObject(res.mapValues {
                            when (val v = it.value) {
                                null -> JsonNull
                                is Number -> JsonPrimitive(v)
                                is Boolean -> JsonPrimitive(v)
                                else -> JsonPrimitive(v.toString())
                            }
                        })),
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
        }
    }.start(wait = true)
}
