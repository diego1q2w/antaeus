package io.pleo.antaeus.scheduler.domain

import kotlinx.serialization.Serializable

@Serializable
data class PaymentScheduledEvent(val invoiceID: Int, val timestamp: Long)