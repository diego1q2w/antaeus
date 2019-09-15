package io.pleo.antaeus.retrier.retry.delivery.http

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.pleo.antaeus.retrier.retry.app.services.HealthCheckService
import mu.KotlinLogging
import org.eclipse.jetty.http.HttpStatus

private val logger = KotlinLogging.logger {}

class AntaeusRest (
        private val healthCheckService: HealthCheckService
) : Runnable {
    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
           path("rest") {
               // Route to check whether the app is running
               // URL: /rest/health
               get("health") {
                   val (isHealthy, body) = healthCheckService.isHealthy()
                   it.status(if (isHealthy) HttpStatus.OK_200 else HttpStatus.INTERNAL_SERVER_ERROR_500)
                   it.json(body)
               }
           }
        }
    }
}
