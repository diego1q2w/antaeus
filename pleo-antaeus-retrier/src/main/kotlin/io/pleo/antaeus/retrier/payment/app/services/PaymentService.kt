package io.pleo.antaeus.retrier.payment.app.services

import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.retrier.payment.domain.*
import io.pleo.antaeus.retrier.payment.infra.db.PaymentDal
import java.time.LocalDateTime
import java.time.ZoneId

typealias now = () -> LocalDateTime
class PaymentService(
        private val dal: PaymentDal,
        private val bus: Bus,
        private val now: now,
        private val maxRetries: Int
) {

    fun paymentEvent(cmd: PayEvent) {
        val payment = dal.fetchEvents(cmd.invoiceID)
        payment.add(cmd)

        dal.persistChanges(payment)
        retry(payment, cmd.invoiceID)
    }

    private fun getTimestamp(): Long = now().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()

    private fun retry(payment: Payment, invoiceId: Int) {
        if(payment.finalStatus() == Status.FAILED) {
            when(payment.totalChanges()) {
                in maxRetries..0 ->
                    InvoicePayRetryApproved(invoiceID = invoiceId, timestamp = getTimestamp())
                            .let(bus::publishMessage)
                else ->
                    InvoicePayRetryDisApproved(invoiceID = invoiceId, timestamp = getTimestamp(), maxRetries = maxRetries)
                            .let(bus::publishMessage)
            }
        }
    }
}