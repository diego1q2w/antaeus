package io.pleo.antaeus.retrier.notification.deliver.bus

import io.pleo.antaeus.rabbitmq.Message
import io.pleo.antaeus.rabbitmq.exceptions.RejectedMessageException
import io.pleo.antaeus.retrier.notification.app.NotificationService
import io.pleo.antaeus.retrier.notification.domain.event.InvoicePayRetryExceededEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import java.lang.Exception

fun invoicePayRetryDisApprovedHandler(service: NotificationService): (Message) -> Unit {
    val logger = KotlinLogging.logger {}

    return { message ->
        try {
            val event = Json(JsonConfiguration.Stable).parse(InvoicePayRetryExceededEvent.serializer(), message.msg)
            service.paymentTriesExceed(event.invoiceID, event.maxRetries)
        } catch (e: Exception) {
            logger.error { "Unable to handle ${message.topic}: ${e.message}" }
            throw RejectedMessageException()
        }
    }
}