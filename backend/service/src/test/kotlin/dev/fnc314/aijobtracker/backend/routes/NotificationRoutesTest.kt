package dev.fnc314.aijobtracker.backend.routes

import dev.fnc314.aijobtracker.backend.security.ApiKeyAuthPlugin
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationRoutesTest {

    @Test
    fun testNotificationRoutes_unauthorizedWhenNoAuth() = testApplication {
        routing {
            notificationRoutes()
        }

        // Without auth/token, should respond Unauthorized
        val response = client.post("/notifications/device/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"test-1","token":"tok-123"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testNotificationRoutes_badRequestWhenMissingFields() = testApplication {
        routing {
            notificationRoutes()
        }

        // With X-Api-Key matching test/local expectations
        val response = client.post("/notifications/device/register") {
            header("X-Api-Key", "test-key")
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":""}""")
        }
        // Since JOB_TRACKER_API_KEY is not set in test environment, extractUid defaults to "default-user"
        // and returns 400 BadRequest due to missing deviceId/token
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("deviceId and token are required"))
    }
}
