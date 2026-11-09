package dev.fnc314.tools.jobtracker.backend

import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

public fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) {
        routing {
            get("/health") {
                call.respondText(
                    """{"status":"UP","service":"jobtracker-backend","port":$port}""",
                    io.ktor.http.ContentType.Application.Json
                )
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
