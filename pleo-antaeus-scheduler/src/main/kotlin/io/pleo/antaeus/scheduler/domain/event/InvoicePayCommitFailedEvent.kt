package io.pleo.antaeus.scheduler.domain.event

import io.pleo.antaeus.rabbitmq.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
data class InvoicePayCommitFailedEvent(val invoiceID: Int, val timestamp: Long, val reason: String): Event() {

    override fun toJSON(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }
}