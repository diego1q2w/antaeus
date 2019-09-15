package io.pleo.antaeus.scheduler.app.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.scheduler.app.exceptions.CustomerNotFoundException
import io.pleo.antaeus.scheduler.app.services.CustomerService
import io.pleo.antaeus.scheduler.app.services.HealthCheckService
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HealthCheckServiceTest {
    private val service = HealthCheckService()

    @Test
    fun `if all services are fine it should be true`() {
        service.addHealthCheck("foo") { true }
        service.addHealthCheck("bar") { true }
        service.addHealthCheck("foobar") { true }

        val (isHealthy, services) = service.isHealthy()
        assert(isHealthy)
        assertEquals(listOf(
                mapOf("foo" to true),
                mapOf("bar" to true),
                mapOf("foobar" to true)
        ), services)
    }

    @Test
    fun `if one service fail it should be false`() {
        service.addHealthCheck("foo") { true }
        service.addHealthCheck("bar") { true }
        service.addHealthCheck("foobar") { false }

        val (isHealthy, body) = service.isHealthy()
        assert(!isHealthy)
        assertEquals(listOf(
                mapOf("foo" to true),
                mapOf("bar" to true),
                mapOf("foobar" to false)
        ), body)
    }
}