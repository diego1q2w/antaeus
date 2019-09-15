package io.pleo.antaeus.scheduler.delivery.bus

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.rabbitmq.Message
import io.pleo.antaeus.rabbitmq.exceptions.RejectedMessageException
import io.pleo.antaeus.scheduler.app.exceptions.NetworkException
import io.pleo.antaeus.scheduler.app.services.BillingService
import io.pleo.antaeus.scheduler.domain.event.topic.EventTopic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HandlerTest {
    private val billingService = mockk<BillingService>(relaxed = true) {
        every { commitPayment(1) } returns Unit
        every { commitPayment(2) } throws NetworkException()

        every { failedPayment(any()) } returns Unit
    }

    @Test
    fun `if no issues handling InvoiceScheduledEvent nothing should happen`() {
        val invoiceScheduledHandler = invoiceScheduledHandler(billingService)
        val message = Message(
                "{\"invoiceID\":1,\"timestamp\":1568438460263}", EventTopic.InvoiceScheduledEvent.name)
        assert(
                invoiceScheduledHandler(message) == Unit
        )
    }

    @Test
    fun `if any issues handling InvoiceScheduledEvent the message should be rejected`() {
        val invoiceScheduledHandler = invoiceScheduledHandler(billingService)
        val message = Message(
                "{\"invoiceID\":2,\"timestamp\":1568438460263}", EventTopic.InvoiceScheduledEvent.name)

        assertThrows<RejectedMessageException> { invoiceScheduledHandler(message) }
    }

    @Test
    fun `it should pass the correct params in invoicePayRetryApproved`() {
        val invoicePayRetryApprovedHandler = invoicePayRetryApprovedHandler(billingService)
        val message = Message(
                "{\"invoiceID\":1,\"timestamp\":1568438460263}", EventTopic.InvoicePayRetryDisApprovedEvent.name)
        invoicePayRetryApprovedHandler(message)

        verify {
            billingService.commitPayment(1)
        }
    }

    @Test
    fun `it should pass the correct params in invoicePayRetryDisApprovedHandler`() {
        val invoicePayRetryDisApprovedHandler = invoicePayRetryDisApprovedHandler(billingService)
        val message = Message(
                "{\"invoiceID\":1,\"timestamp\":1568438460263,\"maxRetries\":2}", EventTopic.InvoicePayRetryDisApprovedEvent.name)
        invoicePayRetryDisApprovedHandler(message)

        verify {
            billingService.failedPayment(1)
        }
    }
}