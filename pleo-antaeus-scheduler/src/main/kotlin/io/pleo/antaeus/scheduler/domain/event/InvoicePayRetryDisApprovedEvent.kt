package io.pleo.antaeus.scheduler.domain.event

import io.pleo.antaeus.rabbitmq.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
data class InvoicePayRetryDisApprovedEvent(val invoiceID: Int, val timestamp: Long, val maxRetries: Int): Event() {

    override fun toJSON(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }
}