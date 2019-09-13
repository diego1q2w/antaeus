package io.pleo.antaeus.core.scheduler.app.services

import io.mockk.*
import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.scheduler.app.external.PaymentProvider
import io.pleo.antaeus.scheduler.app.services.BillingService
import io.pleo.antaeus.scheduler.domain.Currency
import io.pleo.antaeus.scheduler.domain.Invoice
import io.pleo.antaeus.scheduler.domain.InvoiceStatus
import io.pleo.antaeus.scheduler.domain.Money
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal
import org.junit.jupiter.api.Test

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
        every { publishMessage(any(), any()) } returns Unit
    }

    private val now = fun (): LocalDateTime {
        val str = "2000-04-08 12:30"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return LocalDateTime.parse(str, formatter)
    }

    private val billingServiceService = BillingService(dal = dal, paymentProvider = paymentProvider, bus = bus, now=now)

    @Test
    fun `should publish only the correct messages`() {
        billingServiceService.processPayments()

        verifySequence {
            bus.publishMessage("PaymentScheduledEvent", """{"invoiceID":1,"timestamp":955197000000}""")
            bus.publishMessage("PaymentScheduledEvent", """{"invoiceID":3,"timestamp":955197000000}""")
        }

        confirmVerified(bus)
    }
}