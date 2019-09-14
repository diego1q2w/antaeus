package io.pleo.antaeus.retrier.payment.app.services

import io.mockk.*
import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.retrier.payment.domain.*
import io.pleo.antaeus.retrier.payment.infra.db.PaymentDal
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PaymentServiceTest {
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
            every { fetchEvents(1) } returns payment
        }

        val paymentService = PaymentService(dbal, bus, now, maxRetries)

        paymentService.paymentEvent(InvoicePayCommitFailedEvent(invoiceID = 1, timestamp = 1, reason = "foo"))

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
            every { fetchEvents(2) } returns payment
        }

        val paymentService = PaymentService(dbal, bus, now, maxRetries)

        paymentService.paymentEvent(InvoicePayCommitFailedEvent(invoiceID = 2, timestamp = 1, reason = "foo"))

        verify {
            bus.publishMessage(InvoicePayRetryApproved(invoiceID = 2, timestamp = 955197000000))
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
            every { fetchEvents(3) } returns payment
        }

        val paymentService = PaymentService(dbal, bus, now, maxRetries)

        paymentService.paymentEvent(InvoicePayCommitFailedEvent(invoiceID = 3, timestamp = 1, reason = "foo"))

        verify {
            bus.publishMessage(InvoicePayRetryDisApproved(invoiceID = 3, timestamp = 955197000000, maxRetries = maxRetries))
        }

        confirmVerified(bus)
    }



}