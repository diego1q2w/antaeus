package io.pleo.antaeus.retrier.notification.deliver.bus

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.rabbitmq.Message
import io.pleo.antaeus.retrier.notification.app.NotificationService
import io.pleo.antaeus.retrier.notification.domain.event.topic.EventTopic
import org.junit.jupiter.api.Test

class HandlerTest {
    private val notificationService = mockk<NotificationService> {
        every { paymentTriesExceed(any(), any()) } returns Unit
    }

    @Test
    fun `it should pass the correct params in invoicePayRetryDisApprovedHandler`() {
        val invoicePayRetryDisApprovedHandler = invoicePayRetryDisApprovedHandler(notificationService)
        val message = Message(
                "{\"invoiceID\":1,\"timestamp\":1568438460263,\"maxRetries\":4}", EventTopic.InvoicePayRetryExceededEvent.name)
        invoicePayRetryDisApprovedHandler(message)

        verify {
            notificationService.paymentTriesExceed(1, 4)
        }
    }
}