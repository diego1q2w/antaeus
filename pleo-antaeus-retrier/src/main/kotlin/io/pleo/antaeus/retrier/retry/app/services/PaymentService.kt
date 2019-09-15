package io.pleo.antaeus.retrier.retry.app.services

import io.pleo.antaeus.retrier.retry.domain.PaymentEvent
import io.pleo.antaeus.retrier.retry.infra.db.PaymentDal

class PaymentService(private val dal: PaymentDal) {

    fun fetch(invoiceId: Int): List<PaymentEvent> = dal.fetchEvents(invoiceId)
}