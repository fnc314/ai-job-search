package dev.fnc314.aijobtracker.frontend.features.signin.data

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CompletableDeferred

internal class DesktopOAuthServer {
    private var server: HttpServer? = null

    val callbackDeferred = CompletableDeferred<OAuthCallbackResult>()

    data class OAuthCallbackResult(
        val code: String?,
        val state: String?,
        val error: String?,
    )

    fun start(): Int {
        val httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        httpServer.createContext("/callback", CallbackHandler())
        httpServer.executor = null
        httpServer.start()
        this.server = httpServer
        return httpServer.address.port
    }

    fun stop() {
        server?.stop(1)
        server = null
    }

    private inner class CallbackHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val query = exchange.requestURI.query ?: ""
            val params = query.split("&").associate { pair ->
                val parts = pair.split("=", limit = 2)
                val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
                val value = if (parts.size > 1) URLDecoder.decode(parts[1], StandardCharsets.UTF_8) else ""
                key to value
            }

            val code = params["code"]
            val state = params["state"]
            val error = params["error"]

            val htmlResponse = if (code != null) {
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Authentication Successful</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #0f172a; color: #f8fafc; }
                        .card { background: #1e293b; padding: 2.5rem; border-radius: 1rem; box-shadow: 0 10px 25px rgba(0,0,0,0.5); text-align: center; max-width: 400px; }
                        h1 { color: #38bdf8; margin-top: 0; }
                        p { color: #94a3b8; line-height: 1.5; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Authentication Successful</h1>
                        <p>You have signed in to AI Job Tracker. You may safely close this browser window and return to the application.</p>
                    </div>
                </body>
                </html>
                """.trimIndent()
            } else {
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Authentication Failed</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #0f172a; color: #f8fafc; }
                        .card { background: #1e293b; padding: 2.5rem; border-radius: 1rem; box-shadow: 0 10px 25px rgba(0,0,0,0.5); text-align: center; max-width: 400px; }
                        h1 { color: #f87171; margin-top: 0; }
                        p { color: #94a3b8; line-height: 1.5; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Sign-In Failed</h1>
                        <p>${error ?: "Unknown authentication error occurred."}</p>
                    </div>
                </body>
                </html>
                """.trimIndent()
            }

            val responseBytes = htmlResponse.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "text/html; charset=UTF-8")
            exchange.sendResponseHeaders(200, responseBytes.size.toLong())
            val os: OutputStream = exchange.responseBody
            os.write(responseBytes)
            os.close()

            callbackDeferred.complete(OAuthCallbackResult(code = code, state = state, error = error))
        }
    }
}
