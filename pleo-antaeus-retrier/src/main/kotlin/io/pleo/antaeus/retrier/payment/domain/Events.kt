package io.pleo.antaeus.retrier.payment.domain

import io.pleo.antaeus.rabbitmq.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

interface PayEvent

@Serializable
data class InvoicePayCommitSucceedEvent(val invoiceID: Int, val timestamp: Long): Event(), PayEvent {

    override fun toJSON(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }
}

@Serializable
data class InvoicePayCommitFailedEvent(val invoiceID: Int, val timestamp: Long, val reason: String): Event(), PayEvent {

    override fun toJSON(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }
}