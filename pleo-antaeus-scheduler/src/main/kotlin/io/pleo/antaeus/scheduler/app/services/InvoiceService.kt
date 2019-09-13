/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.scheduler.app.services

import io.pleo.antaeus.scheduler.app.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.scheduler.domain.Invoice
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }
}
