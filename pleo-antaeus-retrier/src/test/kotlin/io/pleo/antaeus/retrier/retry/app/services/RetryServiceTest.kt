package io.pleo.antaeus.retrier.retry.app.services

import io.mockk.*
import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.retrier.retry.domain.*
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayRetryApprovedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayRetryExceededEvent
import io.pleo.antaeus.retrier.retry.infra.db.PaymentDal
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RetryServiceTest {
    private val maxRetries = 2

    private val bus = mockk<Bus> {
        every { publishMessage(any()) } returns Unit
    }

    private val now = fun (): LocalDateTime {
        val str = "2000-04-08 12:30"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return LocalDateTime.parse(str, formatter)
    }

    @Test
    fun `if the payment is successful it should not do anything`() {
        val payment = mockk<Payment>(relaxed = true) {
            every { finalStatus() } returns Status.SUCCEED
        }

        val dbal = mockk<PaymentDal>(relaxed = true) {
            every { fetchPaymentAggregation(1) } returns payment
        }

        val retryService = RetryService(dbal, bus, now, maxRetries)

        val event = InvoicePayCommitFailedEvent(invoiceID = 1, timestamp = 1, reason = "foo")
        retryService.paymentEvent(PaymentEvent(invoiceId = event.invoiceID, type = event.topic(), event = event))

        verify {
            bus wasNot Called
        }

        confirmVerified(bus)
    }

    @Test
    fun `payment failed should send a message if still can retry`() {
        val payment = mockk<Payment>(relaxed = true) {
            every { finalStatus() } returns Status.FAILED
            every { totalChanges() } returns 1
        }

        val dbal = mockk<PaymentDal>(relaxed = true) {
            every { fetchPaymentAggregation(2) } returns payment
        }

        val retryService = RetryService(dbal, bus, now, maxRetries)

        val event = InvoicePayCommitFailedEvent(invoiceID = 2, timestamp = 1, reason = "foo")
        retryService.paymentEvent(PaymentEvent(invoiceId = event.invoiceID, type = event.topic(), event = event))

        verify {
            bus.publishMessage(InvoicePayRetryApprovedEvent(invoiceID = 2, timestamp = 955197000000))
        }

        confirmVerified(bus)
    }

    @Test
    fun `payment failed should send a message if can no longer retry`() {
        val payment = mockk<Payment>(relaxed = true) {
            every { finalStatus() } returns Status.FAILED
            every { totalChanges() } returns 4
        }

        val dbal = mockk<PaymentDal>(relaxed = true) {
            every { fetchPaymentAggregation(3) } returns payment
        }

        val retryService = RetryService(dbal, bus, now, maxRetries)

        val event = InvoicePayCommitFailedEvent(invoiceID = 3, timestamp = 1, reason = "foo")
        retryService.paymentEvent(PaymentEvent(invoiceId = event.invoiceID, type = event.topic(), event = event))

        verify {
            bus.publishMessage(InvoicePayRetryExceededEvent(invoiceID = 3, timestamp = 955197000000, maxRetries = maxRetries))
        }

        confirmVerified(bus)
    }



}