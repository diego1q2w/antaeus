package io.pleo.antaeus.retrier.payment.domain

import io.pleo.antaeus.rabbitmq.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

interface PayEvent {
    val timestamp: Long
    val invoiceID: Int
}

@Serializable
data class InvoicePayCommitSucceedEvent(override val invoiceID: Int, override val timestamp: Long): Event(), PayEvent {

    override fun toJSON(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }
}

@Serializable
data class InvoicePayCommitFailedEvent(override val invoiceID: Int, override val timestamp: Long, val reason: String): Event(), PayEvent {

    override fun toJSON(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }
}

@Serializable
data class InvoicePayRetryApproved(val invoiceID: Int, val timestamp: Long): Event() {

    override fun toJSON(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }
}

@Serializable
data class InvoicePayRetryDisApproved(val invoiceID: Int, val timestamp: Long, val maxRetries: Int): Event() {

    override fun toJSON(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }
}