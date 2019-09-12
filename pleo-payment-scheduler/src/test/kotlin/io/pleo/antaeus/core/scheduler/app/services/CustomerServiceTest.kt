package io.pleo.antaeus.core.scheduler.app.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.anateus.scheduler.app.exceptions.CustomerNotFoundException
import io.pleo.anateus.scheduler.app.services.CustomerService
import io.pleo.anateus.scheduler.infra.db.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CustomerServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(404) } returns null
    }

    private val customerService = CustomerService(dal = dal)

    @Test
    fun `will throw if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(404)
        }
    }
}