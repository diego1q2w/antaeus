package io.pleo.antaeus.retrier.payment.app.services

import io.pleo.antaeus.retrier.payment.domain.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.payment.domain.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.payment.domain.Payment
import io.pleo.antaeus.retrier.payment.infra.db.PaymentDal

class PaymentService(
        private val dal: PaymentDal
) {

    fun paymentEvent(cmd: InvoicePayCommitSucceedEvent) {
        val payment = Payment(dal.fetchEvents(cmd.invoiceID))
        payment.add(cmd)

        dal.addEvent(cmd)
    }

    fun paymentEvent(cmd: InvoicePayCommitFailedEvent) {
        val payment = Payment(dal.fetchEvents(cmd.invoiceID))
        payment.add(cmd)

        dal.addEvent(cmd)
    }
}