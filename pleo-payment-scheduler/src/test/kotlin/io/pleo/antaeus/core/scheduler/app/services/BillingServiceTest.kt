package io.pleo.antaeus.core.scheduler.app.services

import io.mockk.*
import io.pleo.anateus.scheduler.app.exceptions.CustomerNotFoundException
import io.pleo.anateus.scheduler.app.external.Bus
import io.pleo.anateus.scheduler.app.external.PaymentProvider
import io.pleo.anateus.scheduler.app.services.BillingService
import io.pleo.anateus.scheduler.domain.Currency
import io.pleo.anateus.scheduler.domain.Invoice
import io.pleo.anateus.scheduler.domain.InvoiceStatus
import io.pleo.anateus.scheduler.domain.Money
import io.pleo.anateus.scheduler.infra.db.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BillingServiceTest {
    private val dal = mockk<AntaeusDal> {
        val invoice1 = Invoice( id = 1, customerId = 1, amount = Money(value= BigDecimal(1), currency = Currency.DKK), status = InvoiceStatus.PENDING )
        val invoice2 = Invoice( id = 2, customerId = 1, amount = Money(value= BigDecimal(1), currency = Currency.DKK), status = InvoiceStatus.PENDING )
        val invoice3 = Invoice( id = 3, customerId = 1, amount = Money(value= BigDecimal(1), currency = Currency.DKK), status = InvoiceStatus.PENDING )

        every { markInvoiceAsProcessing(invoice1)} returns 1
        every { markInvoiceAsProcessing(invoice2)} returns 0
        every { markInvoiceAsProcessing(invoice3)} returns 1

        every { fetchScheduledInvoices(any()) } returns listOf(invoice1, invoice2, invoice3)
    }

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(any()) } returns true
    }

    private val bus = mockk<Bus> {
        every { publish(any()) } returns Unit
    }

    private val billingServiceService = BillingService(dal = dal, paymentProvider = paymentProvider, bus = bus)

    @Test
    fun `should publish only the correct messages`() {
        billingServiceService.processPayments()

        verifySequence {
            bus.publish("1")
            bus.publish("3")
        }

        confirmVerified(bus)
    }
}