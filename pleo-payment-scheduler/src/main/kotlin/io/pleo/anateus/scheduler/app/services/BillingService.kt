package io.pleo.anateus.scheduler.app.services

import io.pleo.anateus.scheduler.app.external.PaymentProvider
import io.pleo.anateus.scheduler.infra.db.AntaeusDal
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {
    private val logger = KotlinLogging.logger {}

    fun schedulePayments() {
        val num = dal.schedulePendingInvoices()
        logger.info { "$num payments scheduled" }
    }
}