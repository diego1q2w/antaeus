package io.pleo.antaeus.scheduler.app.services

import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.scheduler.app.exceptions.CurrencyMismatchException
import io.pleo.antaeus.scheduler.app.exceptions.CustomerNotFoundException
import io.pleo.antaeus.scheduler.app.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.scheduler.app.external.PaymentProvider
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal
import io.pleo.antaeus.scheduler.domain.Invoice
import io.pleo.antaeus.scheduler.domain.InvoicePayCommitFailedEvent
import io.pleo.antaeus.scheduler.domain.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.scheduler.domain.InvoiceScheduledEvent
import mu.KotlinLogging
import java.lang.Exception
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
        dal.schedulePendingInvoices().let {
            logger.info { "$it payments scheduled" }
        }
    }

    // This will be executed repeatedly every few seconds or minutes, It will look for every SCHEDULED payment.
    fun processPayments() {
        dal.fetchScheduledInvoices(10).forEach {
            if (dal.markInvoiceAsProcessing(it) == 1) {
                publishProcessEvent(invoice = it)
            }
        }
    }

    fun commitPayment(invoiceId: Int) {
        try {
            val invoice = dal.fetchInvoice(invoiceId)?: throw InvoiceNotFoundException(invoiceId)

            paymentProvider.charge(invoice).let {
                if(it) successPayment(invoice) else failedPayment(invoice, "account balance did not allow the charge")
            }
        } catch (e: Exception) {
            when(e) {
                // this should ideally trigger some sort of notification in slack or somewhere else
                is CustomerNotFoundException -> logger.warn { "unable to commit payment: ${e.message}" }
                is CurrencyMismatchException -> logger.warn { "unable to commit payment: ${e.message}" }
                is InvoiceNotFoundException -> logger.warn { "unable to commit payment: ${e.message}" }
                // Any other exception including the NetworkException will cause message rejection
                // which ultimately will send the message to the Dead-Letter queue
                else -> throw e
            }
        }
    }

    private fun getTimestamp(): Long = now().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()

    private fun successPayment(invoice: Invoice) {
        dal.markInvoiceAsPaid(invoice)
        InvoicePayCommitSucceedEvent(invoiceID = invoice.id, timestamp = getTimestamp()).let(bus::publishMessage)
    }

    private fun failedPayment(invoice: Invoice, reason: String) {
        dal.markInvoiceAsRetry(invoice)
        InvoicePayCommitFailedEvent(
                invoiceID = invoice.id,
                timestamp = getTimestamp(),
                reason = reason).let(bus::publishMessage)
    }

    private fun publishProcessEvent(invoice: Invoice) {
        InvoiceScheduledEvent(invoiceID = invoice.id, timestamp = getTimestamp()).let(bus::publishMessage)
    }
}