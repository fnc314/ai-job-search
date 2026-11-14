package dev.fnc314.tools.jobtracker.backend.webhook

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposioWebhookRouteTest {

    @Test
    fun `test generic trigger returns OK`() = testApplication {
        application {
            routing {
                composioWebhookRoute()
            }
        }
        val response = client.post("/webhook/composio") {
            contentType(ContentType.Application.Json)
            setBody("""{"trigger_name": "SOME_OTHER_TRIGGER", "payload": {}}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        val json = Json.parseToJsonElement(body).jsonObject
        assertEquals("OK", json["status"]?.toString()?.replace("\"", ""))
        assertTrue(json["message"]?.toString()?.contains("no specific handler implemented yet") == true)
    }

    @Test
    fun `test invalid json returns InternalServerError`() = testApplication {
        application {
            routing {
                composioWebhookRoute()
            }
        }
        val response = client.post("/webhook/composio") {
            contentType(ContentType.Application.Json)
            setBody("invalid-json")
        }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }
}
