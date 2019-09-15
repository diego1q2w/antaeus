package io.pleo.antaeus.scheduler.app.services

import io.mockk.*
import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.scheduler.app.exceptions.CurrencyMismatchException
import io.pleo.antaeus.scheduler.app.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.scheduler.app.exceptions.NetworkException
import io.pleo.antaeus.scheduler.app.external.PaymentProvider
import io.pleo.antaeus.scheduler.domain.*
import io.pleo.antaeus.scheduler.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.scheduler.domain.event.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.scheduler.domain.event.InvoiceScheduledEvent
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BillingServiceTest {
    private val invoice1 = Invoice( id = 1, customerId = 1, amount = Money(value= BigDecimal(1), currency = Currency.DKK), status = InvoiceStatus.PENDING )
    private val invoice2 = Invoice( id = 2, customerId = 1, amount = Money(value= BigDecimal(1), currency = Currency.DKK), status = InvoiceStatus.PENDING )
    private val invoice3 = Invoice( id = 3, customerId = 1, amount = Money(value= BigDecimal(1), currency = Currency.DKK), status = InvoiceStatus.PENDING )
    private val invoice4 = Invoice( id = 4, customerId = 1, amount = Money(value= BigDecimal(1), currency = Currency.DKK), status = InvoiceStatus.PENDING )


    private val dal = mockk<AntaeusDal> {
        every { markInvoiceAsProcessing(invoice1)} returns 1
        every { markInvoiceAsProcessing(invoice2)} returns 0
        every { markInvoiceAsProcessing(invoice3)} returns 1

        every { markInvoiceAsPaid(any())} returns 1
        every { markInvoiceAsRetry(any())} returns 1

        every { fetchScheduledInvoices(any()) } returns listOf(invoice1, invoice2, invoice3)

        every { fetchInvoice(1) } returns invoice1
        every { fetchInvoice(2) } returns invoice2
        every { fetchInvoice(3) } returns invoice3
        every { fetchInvoice(4) } returns invoice4
        every { fetchInvoice(5) } returns null
    }

    private val paymentProvider = mockk<PaymentProvider> {
        every { charge(invoice1) } returns true
        every { charge(invoice2) } throws  NetworkException()
        every { charge(invoice3) } returns false
        every { charge(invoice4) } throws  CurrencyMismatchException(1,1)
    }

    private val bus = mockk<Bus> {
        every { publishMessage(any()) } returns Unit
    }

    private val now = fun (): LocalDateTime {
        val str = "2000-04-08 12:30"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return LocalDateTime.parse(str, formatter)
    }

    private val billingServiceService = BillingService(dal = dal, paymentProvider = paymentProvider, bus = bus, now=now, processBatches = 10)

    @Test
    fun `should publish only deduplicated scheduled payments`() {
        billingServiceService.processPayments()

        verifySequence {
            bus.publishMessage(InvoiceScheduledEvent(1, 955197000000))
            bus.publishMessage(InvoiceScheduledEvent(3, 955197000000))
        }

        confirmVerified(bus)
    }

    @Test
    fun `if the payment is successful should publish an event`() {
        billingServiceService.commitPayment(1)

        verifySequence {
            bus.publishMessage(InvoicePayCommitSucceedEvent(1, 955197000000))
        }

        confirmVerified(bus)
    }

    @Test
    fun `if the payment is not successful should publish an event`() {
        billingServiceService.commitPayment(3)

        verifySequence {
            bus.publishMessage(InvoicePayCommitFailedEvent(3, 955197000000, "account balance did not allow the charge"))
        }

        confirmVerified(bus)
    }

    @Test
    fun `if there is an network error while processing exception expected`() {
        assertThrows<NetworkException> {
            billingServiceService.commitPayment(2)
        }

        verify {
            bus wasNot Called
        }

        confirmVerified(bus)
    }

    @Test
    fun `if there is a recognized issue while processing nothing should happen`() {
        assert(billingServiceService.commitPayment(4) == Unit)

        verify {
            bus wasNot Called
        }

        confirmVerified(bus)
    }

    @Test
    fun `failed payment will throw an error in case of invoice not found`() {
        assertThrows<InvoiceNotFoundException> { billingServiceService.failedPayment(5) }
    }
}