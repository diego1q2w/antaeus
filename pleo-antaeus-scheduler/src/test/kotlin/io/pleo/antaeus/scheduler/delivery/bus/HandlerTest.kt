package io.pleo.antaeus.scheduler.delivery.bus

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.rabbitmq.Message
import io.pleo.antaeus.rabbitmq.exceptions.RejectedMessageException
import io.pleo.antaeus.scheduler.app.exceptions.NetworkException
import io.pleo.antaeus.scheduler.app.services.BillingService
import io.pleo.antaeus.scheduler.delivery.bus.invoiceScheduledHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HandlerTest {
    private val billingService = mockk<BillingService> {
        every { commitPayment(1) } returns Unit
        every { commitPayment(2) } throws NetworkException()
    }

    private val invoiceScheduledHandler = invoiceScheduledHandler(billingService)

    @Test
    fun `if no issues handling InvoiceScheduledEvent nothing should happen`() {
        val message = Message("{\"invoiceID\":1,\"timestamp\":1568438460263}", "InvoiceScheduledEvent")
        assert(
                invoiceScheduledHandler(message) == Unit
        )
    }

    @Test
    fun `if any issues handling the message should be rejected`() {
        val message = Message("{\"invoiceID\":2,\"timestamp\":1568438460263}", "InvoiceScheduledEvent")
        assertThrows<RejectedMessageException> { invoiceScheduledHandler(message) }
    }
}