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
        }
    }.start(wait = true)
}
