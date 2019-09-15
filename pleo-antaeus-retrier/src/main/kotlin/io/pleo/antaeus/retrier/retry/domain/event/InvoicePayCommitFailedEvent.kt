package io.pleo.antaeus.retrier.retry.domain.event

import io.pleo.antaeus.rabbitmq.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
data class InvoicePayCommitFailedEvent(override val invoiceID: Int, override val timestamp: Long, val reason: String): Event(), PayEvent {

    override fun toJSON(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }
}
