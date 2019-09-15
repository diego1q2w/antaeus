package io.pleo.antaeus.retrier.retry.delivery.bus

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.rabbitmq.Message
import io.pleo.antaeus.retrier.retry.app.services.RetryService
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.retry.domain.event.topic.EventTopic
import org.junit.jupiter.api.Test

class HandlerTest {
    private val retryService = mockk<RetryService>(relaxed = true) {
        every { paymentEvent(any()) } returns Unit
    }

    @Test
    fun `it should pass the correct params in invoicePayCommitFailedHandler`() {
        val invoicePayCommitFailedHandler = invoicePayCommitFailedHandler(retryService)
        val message = Message(
                "{\"invoiceID\":1,\"timestamp\":1568438460263,\"reason\":\"foo\"}", EventTopic.InvoicePayCommitFailedEvent.name)
        invoicePayCommitFailedHandler(message)

        verify {
            retryService.paymentEvent(InvoicePayCommitFailedEvent(invoiceID = 1, timestamp = 1568438460263, reason = "foo"))
        }
    }

    @Test
    fun `it should pass the correct params in invoicePayCommitSucceedHandler`() {
        val invoicePayCommitSucceedHandler = invoicePayCommitSucceedHandler(retryService)
        val message = Message(
                "{\"invoiceID\":1,\"timestamp\":1568438460263}", EventTopic.InvoicePayCommitSucceedEvent.name)
        invoicePayCommitSucceedHandler(message)

        verify {
            retryService.paymentEvent(InvoicePayCommitSucceedEvent(invoiceID = 1, timestamp = 1568438460263))
        }
    }
}