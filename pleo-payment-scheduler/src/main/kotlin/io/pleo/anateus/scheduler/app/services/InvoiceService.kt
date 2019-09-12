/*
    Implements endpoints related to invoices.
 */

package io.pleo.anateus.scheduler.app.services

import io.pleo.anateus.scheduler.app.exceptions.InvoiceNotFoundException
import io.pleo.anateus.scheduler.domain.Invoice
import io.pleo.anateus.scheduler.infra.db.AntaeusDal

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }
}
