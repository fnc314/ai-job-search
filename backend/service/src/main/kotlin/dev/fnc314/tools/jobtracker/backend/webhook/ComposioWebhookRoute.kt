package dev.fnc314.tools.jobtracker.backend.webhook

import dev.fnc314.tools.jobtracker.backend.BackendService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Registers the Composio trigger webhook endpoint.
 *
 * This endpoint receives real-time email triggers (like GMAIL_EMAIL_SENT_TRIGGER
 * or inbound push notifications) from Composio/PubSub and triggers
 * the corresponding logic in BackendService.
 */
public fun Route.composioWebhookRoute() {
    post("/webhook/composio") {
        try {
            val bodyText = call.receiveText()
            val jsonElement = Json.parseToJsonElement(bodyText).jsonObject

            // Check if it's a Pub/Sub message wrapping the payload
            val payloadToProcess = if (jsonElement.containsKey("message")) {
                val messageObj = jsonElement["message"]?.jsonObject
                val dataBase64 = messageObj?.get("data")?.jsonPrimitive?.contentOrNull
                
                if (dataBase64 != null) {
                    // Decode base64 Pub/Sub data payload
                    val decodedData = java.util.Base64.getDecoder().decode(dataBase64).toString(Charsets.UTF_8)
                    Json.parseToJsonElement(decodedData).jsonObject
                } else {
                    jsonElement
                }
            } else {
                jsonElement
            }

            // Route based on trigger type
            val triggerName = payloadToProcess["trigger_name"]?.jsonPrimitive?.contentOrNull
            
            when (triggerName) {
                "GMAIL_EMAIL_SENT_TRIGGER" -> {
                    handleEmailSentTrigger(call, payloadToProcess)
                }
                else -> {
                    // Log unknown or general webhook payload
                    println("Received generic Composio webhook. Trigger: ${triggerName ?: "UNKNOWN"}")
                    
                    // For inbound emails or other triggers, we can invoke discover/collect directly
                    // This integrates with our email pipeline goal.
                    call.respondText(
                        """{"status":"OK","message":"Trigger received but no specific handler implemented yet."}""",
                        ContentType.Application.Json,
                        status = HttpStatusCode.OK
                    )
                }
            }
        } catch (e: Exception) {
            System.err.println("Error processing Composio webhook: ${e.message}")
            e.printStackTrace()
            call.respondText(
                """{"status":"ERROR","message":"${e.message}"}""",
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}

private suspend fun handleEmailSentTrigger(call: ApplicationCall, payload: kotlinx.serialization.json.JsonObject) {
    // Extract metadata from the GMAIL_EMAIL_SENT_TRIGGER payload
    val payloadData = payload["payload"]?.jsonObject ?: payload
    
    val threadId = payloadData["thread_id"]?.jsonPrimitive?.contentOrNull
    val messageId = payloadData["message_id"]?.jsonPrimitive?.contentOrNull
    val recipients = payloadData["recipients"]?.jsonPrimitive?.contentOrNull ?: ""
    val subject = payloadData["subject"]?.jsonPrimitive?.contentOrNull ?: ""
    val messageText = payloadData["message_text"]?.jsonPrimitive?.contentOrNull ?: ""
    val timestamp = payloadData["message_timestamp"]?.jsonPrimitive?.contentOrNull
    
    println("Processing GMAIL_EMAIL_SENT_TRIGGER for messageId=$messageId, threadId=$threadId")
    
    // Construct a standard DiscoveryCandidate that BackendService understands
    val candidate = BackendService.DiscoveryCandidate(
        company = "Unknown (Inbound Hook)", // We might need to run extractCompanyFromEmail
        companyId = "unknown-hook",
        role = "Unknown Role",
        status = "APPLIED", 
        messageId = messageId ?: "unknown",
        subject = subject,
        sender = recipients, // for outbound, we are sending TO them.
        receivedAt = timestamp,
        confidence = "LOW",
        relatedMessageIds = listOfNotNull(messageId),
        sourceLabel = "Webhook Outbound"
    )
    
    try {
        // Evaluate the outbound email to see if it matches an existing tracked company
        val result = BackendService.stageDiscoveryProposals(listOf(candidate))
        val count = result["staged"] ?: 0
        println("Staged $count proposals from GMAIL_EMAIL_SENT_TRIGGER.")
        
        call.respondText(
            """{"status":"OK","message":"Processed GMAIL_EMAIL_SENT_TRIGGER successfully. Staged $count proposals."}""",
            ContentType.Application.Json,
            status = HttpStatusCode.OK
        )
    } catch (e: Exception) {
        println("Error staging proposals for outbound email: ${e.message}")
        e.printStackTrace()
        call.respondText(
            """{"status":"ERROR","message":"Error staging proposals: ${e.message}"}""",
            ContentType.Application.Json,
            status = HttpStatusCode.InternalServerError
        )
    }
}
