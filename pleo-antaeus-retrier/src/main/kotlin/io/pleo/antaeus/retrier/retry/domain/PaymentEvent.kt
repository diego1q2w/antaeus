package io.pleo.antaeus.retrier.retry.domain

import io.pleo.antaeus.retrier.retry.domain.event.PayEvent

data class PaymentEvent(val invoiceId: Int, val type: String, val event: PayEvent)