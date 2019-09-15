package io.pleo.antaeus.retrier.retry.domain

import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.retry.domain.event.PayEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaymentTest {
    @Test
    fun `the latest status should be failed even with events out of order`() {
        val payment = Payment(listOf<PaymentEvent>(
                PaymentEvent(2, "", InvoicePayCommitFailedEvent(invoiceID = 2, timestamp = 123, reason = "")),
                PaymentEvent(1, "", InvoicePayCommitSucceedEvent(invoiceID = 1, timestamp = 12)),
                PaymentEvent(4, "", InvoicePayCommitFailedEvent(invoiceID = 4, timestamp = 12345, reason = ""))
        ))

        assertEquals(Status.FAILED, payment.finalStatus())
        assertEquals(3, payment.totalChanges())
        assertEquals(mutableListOf<PayEvent>(), payment.difference())
    }

    @Test
    fun `the latest status should be succeed even with events out of order`() {
        val payment = Payment(listOf<PaymentEvent>(
                PaymentEvent(2, "",InvoicePayCommitFailedEvent(invoiceID = 2, timestamp = 123, reason = "")),
                PaymentEvent(1, "",InvoicePayCommitSucceedEvent(invoiceID = 1, timestamp = 12)),
                PaymentEvent(4, "",InvoicePayCommitFailedEvent(invoiceID = 4, timestamp = 12345, reason = ""))
        ))

        payment.add(PaymentEvent(3, "",InvoicePayCommitSucceedEvent(invoiceID = 3, timestamp = 22333)))
        payment.add(PaymentEvent(6, "",InvoicePayCommitSucceedEvent(invoiceID = 6, timestamp = 1)))

        assertEquals(Status.SUCCEED, payment.finalStatus())
        assertEquals(5, payment.totalChanges())
        assertEquals(mutableListOf<PaymentEvent>(
                PaymentEvent(3, "",InvoicePayCommitSucceedEvent(invoiceID = 3, timestamp = 22333)),
                PaymentEvent(6, "",InvoicePayCommitSucceedEvent(invoiceID = 6, timestamp = 1))
        ), payment.difference())
    }
}