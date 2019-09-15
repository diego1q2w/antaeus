package io.pleo.antaeus.retrier.retry.delivery.bus

import io.pleo.antaeus.rabbitmq.Message
import io.pleo.antaeus.rabbitmq.exceptions.RejectedMessageException
import io.pleo.antaeus.retrier.retry.app.services.RetryService
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitSucceedEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import java.lang.Exception

fun invoicePayCommitFailedHandler(service: RetryService): (Message) -> Unit {
    val logger = KotlinLogging.logger {}

    return { message ->
        try {
            val event = Json(JsonConfiguration.Stable).parse(InvoicePayCommitFailedEvent.serializer(), message.msg)
            service.paymentEvent(event)
        } catch (e: Exception) {
            logger.error { "Unable to handle ${message.topic}: ${e.message}" }
            throw RejectedMessageException()
        }
    }
}


fun invoicePayCommitSucceedHandler(service: RetryService): (Message) -> Unit {
    val logger = KotlinLogging.logger {}

    return { message ->
        try {
            val event = Json(JsonConfiguration.Stable).parse(InvoicePayCommitSucceedEvent.serializer(), message.msg)
            service.paymentEvent(event)
        } catch (e: Exception) {
            logger.error { "Unable to handle ${message.topic}: ${e.message}" }
            throw RejectedMessageException()
        }
    }
}