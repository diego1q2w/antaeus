package io.pleo.antaeus.scheduler.delivery.bus

import io.pleo.antaeus.rabbitmq.Message
import io.pleo.antaeus.rabbitmq.exceptions.RejectedMessageException
import io.pleo.antaeus.scheduler.app.services.BillingService
import io.pleo.antaeus.scheduler.domain.event.InvoicePayRetryApprovedEvent
import io.pleo.antaeus.scheduler.domain.event.InvoicePayRetryExceededEvent
import io.pleo.antaeus.scheduler.domain.event.InvoiceScheduledEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import java.lang.Exception

fun  invoiceScheduledHandler(service: BillingService): (Message) -> Unit {
    val logger = KotlinLogging.logger {}

    return { message ->
        try {
            val event = Json(JsonConfiguration.Stable).parse(InvoiceScheduledEvent.serializer(), message.msg)
            service.commitPayment(event.invoiceID)
        } catch (e: Exception) {
            logger.error { "Unable to handle ${message.topic}: ${e.message}" }
            throw RejectedMessageException()
        }
    }
}

/*
* This emulates a monthly event that happens every 1st of each month, this is only a PoC.
* A kubernetes cronjob would be perfect for this use case
* */
fun  monthlyHandler(service: BillingService): (Message) -> Unit {
    val logger = KotlinLogging.logger {}
    return { message ->
        try {
            service.schedulePayments()
        } catch (e: Exception) {
            logger.error { "Unable to handle ${message.topic}: ${e.message}" }
        }
    }
}

fun  invoicePayRetryApprovedHandler(service: BillingService): (Message) -> Unit {
    val logger = KotlinLogging.logger {}

    return { message ->
        try {
            val event = Json(JsonConfiguration.Stable).parse(InvoicePayRetryApprovedEvent.serializer(), message.msg)
            service.commitPayment(event.invoiceID)
        } catch (e: Exception) {
            logger.error { "Unable to handle ${message.topic}: ${e.message}" }
            throw RejectedMessageException()
        }
    }
}

fun  invoicePayRetryDisApprovedHandler(service: BillingService): (Message) -> Unit {
    val logger = KotlinLogging.logger {}

    return { message ->
        try {
            val event = Json(JsonConfiguration.Stable).parse(InvoicePayRetryExceededEvent.serializer(), message.msg)
            service.failedPayment(event.invoiceID)
        } catch (e: Exception) {
            logger.error { "Unable to handle ${message.topic}: ${e.message}" }
            throw RejectedMessageException()
        }
    }
}