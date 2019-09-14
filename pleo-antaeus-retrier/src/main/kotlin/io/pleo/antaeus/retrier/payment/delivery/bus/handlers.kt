package io.pleo.antaeus.retrier.payment.delivery.bus

import io.pleo.antaeus.rabbitmq.Message
import io.pleo.antaeus.rabbitmq.exceptions.RejectedMessageException
import io.pleo.antaeus.retrier.payment.app.services.PaymentService
import io.pleo.antaeus.retrier.payment.domain.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.payment.domain.InvoicePayCommitSucceedEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import java.lang.Exception

fun invoicePayCommitFailedHandler(service: PaymentService): (Message) -> Unit {
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


fun invoicePayCommitSucceedHandler(service: PaymentService): (Message) -> Unit {
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