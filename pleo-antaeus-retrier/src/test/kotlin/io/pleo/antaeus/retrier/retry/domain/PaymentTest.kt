package io.pleo.antaeus.retrier.retry.domain

import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.retry.domain.event.PayEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaymentTest {
    @Test
    fun `the latest status should be pay failed even with events out of order`() {
        val payment = Payment(listOf<PayEvent>(
                InvoicePayCommitFailedEvent(invoiceID = 2, timestamp = 123, reason = ""),
                InvoicePayCommitSucceedEvent(invoiceID = 1, timestamp = 12),
                InvoicePayCommitFailedEvent(invoiceID = 4, timestamp = 12345, reason = "")
        ))

        assertEquals(Status.FAILED, payment.finalStatus())
        assertEquals(3, payment.totalChanges())
        assertEquals(mutableListOf<PayEvent>(), payment.difference())
    }

    @Test
    fun `the latest status should be pay succeed even with events out of order`() {
        val payment = Payment(listOf<PayEvent>(
                InvoicePayCommitFailedEvent(invoiceID = 2, timestamp = 123, reason = ""),
                InvoicePayCommitSucceedEvent(invoiceID = 1, timestamp = 12),
                InvoicePayCommitFailedEvent(invoiceID = 4, timestamp = 12345, reason = "")
        ))

        payment.add(InvoicePayCommitSucceedEvent(invoiceID = 3, timestamp = 22333))
        payment.add(InvoicePayCommitSucceedEvent(invoiceID = 6, timestamp = 1))

        assertEquals(Status.SUCCEED, payment.finalStatus())
        assertEquals(5, payment.totalChanges())
        assertEquals(mutableListOf<PayEvent>(
                InvoicePayCommitSucceedEvent(invoiceID = 3, timestamp = 22333),
                InvoicePayCommitSucceedEvent(invoiceID = 6, timestamp = 1)
        ), payment.difference())
    }
}