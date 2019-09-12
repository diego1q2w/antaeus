package io.pleo.anateus.scheduler.app.services

import io.pleo.anateus.scheduler.app.external.Bus
import io.pleo.anateus.scheduler.app.external.PaymentProvider
import io.pleo.anateus.scheduler.domain.Invoice
import io.pleo.anateus.scheduler.infra.db.AntaeusDal
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal,
    private val bus: Bus
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun schedulePayments() {
        val num = dal.schedulePendingInvoices()
        logger.info { "$num payments scheduled" }
    }

    fun processPayments() {
        dal.fetchScheduledInvoices(10).forEach {
            if (dal.markInvoiceAsProcessing(it) == 1) {
                publishProcessEvent(invoice = it)
            }
        }
    }

    private fun publishProcessEvent(invoice: Invoice) {
        println(invoice)
        bus.publish("${invoice.id}")
    }
}