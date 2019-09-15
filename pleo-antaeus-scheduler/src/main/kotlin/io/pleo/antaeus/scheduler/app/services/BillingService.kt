package io.pleo.antaeus.scheduler.app.services

import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.scheduler.app.exceptions.CurrencyMismatchException
import io.pleo.antaeus.scheduler.app.exceptions.CustomerNotFoundException
import io.pleo.antaeus.scheduler.app.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.scheduler.app.external.PaymentProvider
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal
import io.pleo.antaeus.scheduler.domain.Invoice
import io.pleo.antaeus.scheduler.domain.InvoiceStatus
import io.pleo.antaeus.scheduler.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.scheduler.domain.event.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.scheduler.domain.event.InvoiceScheduledEvent
import mu.KotlinLogging
import java.lang.Exception
import java.time.LocalDateTime
import java.time.ZoneId

typealias now = () -> LocalDateTime

class BillingService(
        private val paymentProvider: PaymentProvider,
        private val dal: AntaeusDal,
        private val bus: Bus,
        private val now: now,
        private val processBatches: Int
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /*
    * "schedulePayments" method is meant to be fast. It will only mark all "PENDING" invoices as "SCHEDULED"
    * After this, any other invoice that comes during or after it, will have to wait until the next "month".
    * */
    fun schedulePayments() {
        dal.schedulePendingInvoices().let {
            logger.info { "$it payments scheduled" }
        }
    }

    /*
    * This will be executed repeatedly every few seconds or minutes, It will look for every SCHEDULED payment.
    * And its only task is to publish an event per invoice and then mark it as PROCESSING.
    * It process payments per baches.
    *
    * It's time to talk about the elephant in the room. Why the nerve of publishing an event per invoice?
    * This achieve few things:
    *   1- The schedule payments task is fast and thus, less error-prone.
    *   2- Having events, enable transactionality, if there is a network error in either the DB or the payment provider, the event
    *       gets send to the DLX for a potential retry. Since it wasn't users fault (Perhaps, didn't even hit the endpoint) we can try to re-execute it.
    *   3- If the server gets shutdown or the DB becomes unreachable, we won't loose those payments and as soon everything gets back to normality.
    *       We will process every payment from the last successful one.
    */
    fun processPayments() {
        //TODO: Create an app configuration
        dal.fetchScheduledInvoices(processBatches).forEach {
            if (dal.markInvoiceAsProcessing(it) == 1) {
                publishProcessEvent(invoice = it)
            }
        }
    }

    /*
    * This will commit the payment and if succeed will publish either an `InvoicePayCommitSucceedEvent`
    * or `InvoicePayCommitFailedEvent` or nothing in case of an exception happen, that will potentially send the event to DLX for retrial
    * */
    fun commitPayment(invoiceId: Int) {
        try {
            val invoice = dal.fetchInvoice(invoiceId)?: throw InvoiceNotFoundException(invoiceId)

            if (invoice.status == InvoiceStatus.PAID) return

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

    fun failedPayment(invoiceId: Int) {
        val invoice = dal.fetchInvoice(invoiceId)?: throw InvoiceNotFoundException(invoiceId)

        dal.markInvoiceAsFailed(invoice)
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