package io.pleo.antaeus.scheduler.delivery.bus

import io.pleo.antaeus.rabbitmq.Message
import io.pleo.antaeus.rabbitmq.exceptions.RejectedMessageException
import io.pleo.antaeus.scheduler.app.services.BillingService
import io.pleo.antaeus.scheduler.domain.InvoiceScheduledEvent
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.lang.Exception

fun  invoiceScheduledHandler(service: BillingService): (Message) -> Unit {
    val logger = KotlinLogging.logger {}

    return { message ->
        try {
            val event = Json.parse(InvoiceScheduledEvent.serializer(), message.msg)
            service.commitPayment(event.invoiceID)
        } catch (e: Exception) {
            logger.error { "Unable to handle ${message.topic}: ${e.message}" }
            throw RejectedMessageException()
        }
    }
}