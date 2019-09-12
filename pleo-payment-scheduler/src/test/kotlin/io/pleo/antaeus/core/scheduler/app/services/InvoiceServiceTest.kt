package io.pleo.antaeus.core.scheduler.app.services


import io.mockk.every
import io.mockk.mockk
import io.pleo.anateus.scheduler.app.exceptions.InvoiceNotFoundException
import io.pleo.anateus.scheduler.app.services.InvoiceService
import io.pleo.anateus.scheduler.infra.db.AntaeusDal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(404) } returns null
    }

    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `will throw if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }
}