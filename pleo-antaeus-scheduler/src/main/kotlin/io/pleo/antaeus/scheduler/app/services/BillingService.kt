package io.pleo.antaeus.scheduler.app.services

import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.scheduler.app.external.PaymentProvider
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal
import io.pleo.antaeus.scheduler.domain.Invoice
import io.pleo.antaeus.scheduler.domain.InvoiceScheduledEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import java.time.LocalDateTime
import java.time.ZoneId

typealias now = () -> LocalDateTime
class BillingService(
        private val paymentProvider: PaymentProvider,
        private val dal: AntaeusDal,
        private val bus: Bus,
        private val now: now
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    // This will be executed every first day of the month, It will look for every PENDING payment
    fun schedulePayments() {
        val num = dal.schedulePendingInvoices()
        logger.info { "$num payments scheduled" }
    }

    // This will be executed repeatedly every few seconds or minutes, It will look for every SCHEDULED payment.
    fun processPayments() {
        dal.fetchScheduledInvoices(10).forEach {
            if (dal.markInvoiceAsProcessing(it) == 1) {
                publishProcessEvent(invoice = it)
            }
        }
    }

    private fun publishProcessEvent(invoice: Invoice) {
        val timestamp = now().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val event = InvoiceScheduledEvent(invoiceID = invoice.id, timestamp = timestamp)
        val json = Json(JsonConfiguration.Stable)
        val jsonData = json.stringify(InvoiceScheduledEvent.serializer(), event)

        bus.publishMessage(topic = InvoiceScheduledEvent::class.simpleName!!, msg = jsonData)
    }
}