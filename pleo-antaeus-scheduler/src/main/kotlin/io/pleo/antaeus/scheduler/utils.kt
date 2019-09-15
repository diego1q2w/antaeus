package io.pleo.antaeus.scheduler

import io.pleo.antaeus.scheduler.app.exceptions.CurrencyMismatchException
import io.pleo.antaeus.scheduler.app.exceptions.CustomerNotFoundException
import io.pleo.antaeus.scheduler.app.exceptions.NetworkException
import io.pleo.antaeus.scheduler.app.external.PaymentProvider
import io.pleo.antaeus.scheduler.domain.Currency
import io.pleo.antaeus.scheduler.domain.Invoice
import io.pleo.antaeus.scheduler.domain.InvoiceStatus
import io.pleo.antaeus.scheduler.domain.Money
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal
import java.math.BigDecimal
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..15).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            if(Random.nextInt(100) <= 3) {
                when(Random.nextInt(5)) {
                    0 -> throw CustomerNotFoundException(invoice.customerId)
                    1 -> throw CurrencyMismatchException(invoice.id, invoice.customerId)
                    else -> throw NetworkException() // This is the most likely to happen
                }
            }

            return Random.nextBoolean()
        }
    }
}
