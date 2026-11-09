package dev.fnc314.tools.jobtracker.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/health") {
                call.respondText("""{"status":"UP","service":"jobtracker-backend"}""")
            }
            post("/tasks/sweep") {
                call.respondText("""{"status":"SUCCESS","message":"Routine sweep completed"}""")
            }
            post("/tasks/email-read-inbox") {
                call.respondText("""{"status":"SUCCESS","message":"Inbox read"}""")
            }
            post("/tasks/email-collect") {
                call.respondText("""{"status":"SUCCESS","message":"Proposals collected"}""")
            }
            post("/tasks/proposals-expire") {
                call.respondText("""{"status":"SUCCESS","message":"Stale proposals expired"}""")
            }
            post("/tasks/tracker-sync-due") {
                call.respondText("""{"status":"SUCCESS","message":"Due syncs processed"}""")
            }
        }
    }.start(wait = true)
}
