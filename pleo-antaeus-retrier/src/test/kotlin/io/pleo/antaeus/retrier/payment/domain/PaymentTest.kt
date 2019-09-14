package io.pleo.antaeus.retrier.payment.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaymentTest {
    private val payment = Payment(listOf<PayEvent>(
            InvoicePayCommitFailedEvent(invoiceID = 1, timestamp = 1234, reason = ""),
            InvoicePayCommitSucceedEvent(invoiceID = 2, timestamp = 123),
            InvoicePayCommitFailedEvent(invoiceID = 4, timestamp = 12345, reason = "")
    ))

    @Test
    fun `the latest status should be failed`() {
        assertEquals(PaymentStatus(invoiceId = 4, timestamp = 12345, status = Status.FAILED), payment.finalStatus())
        assertEquals(3, payment.totalChanges())
    }

    @Test
    fun `the latest status should be succeed`() {
        payment.apply(InvoicePayCommitSucceedEvent(invoiceID = 5, timestamp = 12333))

        assertEquals(PaymentStatus(invoiceId = 5, timestamp = 12333, status = Status.SUCCEED), payment.finalStatus())
        assertEquals(4, payment.totalChanges())
    }
}