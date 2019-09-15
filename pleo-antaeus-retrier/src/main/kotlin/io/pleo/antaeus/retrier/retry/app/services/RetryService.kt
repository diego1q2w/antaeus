package io.pleo.antaeus.retrier.retry.app.services

import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.retrier.retry.domain.*
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayRetryApprovedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayRetryDisApprovedEvent
import io.pleo.antaeus.retrier.retry.domain.event.PayEvent
import io.pleo.antaeus.retrier.retry.infra.db.PaymentDal
import java.time.LocalDateTime
import java.time.ZoneId

typealias now = () -> LocalDateTime
class RetryService(
        private val dal: PaymentDal,
        private val bus: Bus,
        private val now: now,
        private val maxRetries: Int
) {

    //It aggregates all the payments for that invoice in order to decide whether to retry or not
    fun paymentEvent(cmd: PayEvent) {
        val payment = dal.fetchEvents(cmd.invoiceID)
        payment.add(cmd)

        dal.persistChanges(payment)
        retry(payment, cmd.invoiceID)
    }

    private fun getTimestamp(): Long = now().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()

    private fun retry(payment: Payment, invoiceId: Int) {
        if(payment.finalStatus() == Status.FAILED) {
            when(val total = payment.totalChanges()) {
                in maxRetries downTo 0 ->
                    InvoicePayRetryApprovedEvent(invoiceID = invoiceId, timestamp = getTimestamp())
                            .let(bus::publishMessage)
                else ->
                    InvoicePayRetryDisApprovedEvent(invoiceID = invoiceId, timestamp = getTimestamp(), maxRetries = maxRetries)
                            .let(bus::publishMessage)
            }
        }
    }
}