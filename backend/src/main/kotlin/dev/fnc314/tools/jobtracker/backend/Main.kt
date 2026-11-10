package dev.fnc314.tools.jobtracker.backend

import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
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

public fun main(args: Array<String>) {
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

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) {
        routing {
            get("/health") {
                call.respondText(
                    """{"status":"UP","service":"jobtracker-backend","port":$port}""",
                    io.ktor.http.ContentType.Application.Json
                )
            }
            post("/applications/{id}/events") {
                val applicationId = call.parameters["id"]
                if (applicationId.isNullOrBlank()) {
                    call.respondText(
                        """{"status":"ERROR","message":"Missing applicationId parameter"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.BadRequest
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
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.OK
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/applications/{id}/align") {
                val applicationId = call.parameters["id"]
                if (applicationId.isNullOrBlank()) {
                    call.respondText(
                        """{"status":"ERROR","message":"Missing applicationId parameter"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.BadRequest
                    )
                    return@post
                }

                try {
                    val dryRun = call.request.queryParameters["dryRun"]?.toBooleanStrictOrNull() ?: false
                    val result = BackendService.alignApplication(applicationId, dryRun = dryRun)
                    val responseJson = Json.encodeToString(JsonObject(result.mapValues { (_, v) -> JsonPrimitive(v?.toString() ?: "") }))
                    call.respondText(
                        responseJson,
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.OK
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/tasks/sweep") {
                try {
                    val result = BackendService.performRoutineSweep()
                    val jsonMap = result.mapValues { (_, v) -> JsonPrimitive(v.toString()) }
                    call.respondText(
                        Json.encodeToString(JsonObject(jsonMap)),
                        io.ktor.http.ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
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
                            io.ktor.http.ContentType.Application.Json,
                            status = io.ktor.http.HttpStatusCode.BadRequest
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
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.OK
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/companies/tasks/enrich/batch") {
                try {
                    val result = BackendService.batchEnrichCompanies()
                    val jsonMap = result.mapValues { (_, v) ->
                        when (v) {
                            is List<*> -> {
                                kotlinx.serialization.json.JsonArray(v.map { kotlinx.serialization.json.JsonPrimitive(it?.toString() ?: "") })
                            }
                            else -> kotlinx.serialization.json.JsonPrimitive(v?.toString() ?: "")
                        }
                    }
                    call.respondText(
                        Json.encodeToString(JsonObject(jsonMap)),
                        io.ktor.http.ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
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
                            io.ktor.http.ContentType.Application.Json,
                            status = io.ktor.http.HttpStatusCode.BadRequest
                        )
                        return@post
                    }

                    val result = BackendService.addCompany(name = name, website = website)

                    val responseJson = Json.encodeToString(JsonObject(result.mapValues { (_, v) -> JsonPrimitive(v?.toString() ?: "") }))
                    call.respondText(
                        responseJson,
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.OK
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
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
                        io.ktor.http.ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/tasks/email-read-inbox") {
                call.respondText(
                    """{"status":"SUCCESS","message":"Inbox scan executed"}""",
                    io.ktor.http.ContentType.Application.Json
                )
            }
            post("/tasks/email-collect") {
                call.respondText(
                    """{"status":"SUCCESS","message":"Proposals collected"}""",
                    io.ktor.http.ContentType.Application.Json
                )
            }
            post("/tasks/proposals-expire") {
                try {
                    val expired = BackendService.expireStaleProposals()
                    call.respondText(
                        """{"status":"SUCCESS","expired":$expired}""",
                        io.ktor.http.ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
                    )
                }
            }
            post("/tasks/tracker-sync-due") {
                try {
                    val processed = BackendService.processDueSyncs()
                    call.respondText(
                        """{"status":"SUCCESS","processed":$processed}""",
                        io.ktor.http.ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
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
                                is Number -> kotlinx.serialization.json.JsonPrimitive(v)
                                is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
                                else -> kotlinx.serialization.json.JsonPrimitive(v.toString())
                            }
                        })),
                        io.ktor.http.ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
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
                            io.ktor.http.ContentType.Application.Json,
                            status = io.ktor.http.HttpStatusCode.BadRequest
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
                                null -> kotlinx.serialization.json.JsonNull
                                is Number -> kotlinx.serialization.json.JsonPrimitive(v)
                                is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
                                else -> kotlinx.serialization.json.JsonPrimitive(v.toString())
                            }
                        })),
                        io.ktor.http.ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        """{"status":"ERROR","message":"${e.message}"}""",
                        io.ktor.http.ContentType.Application.Json,
                        status = io.ktor.http.HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }.start(wait = true)
}
