package io.pleo.antaeus.scheduler.domain

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceScheduledEvent(val invoiceID: Int, val timestamp: Long)