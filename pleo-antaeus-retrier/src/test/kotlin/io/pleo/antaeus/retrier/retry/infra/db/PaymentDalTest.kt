package io.pleo.antaeus.retrier.retry.infra.db

import io.pleo.antaeus.retrier.retry.domain.*
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.retry.domain.event.PayEvent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.Connection

class PaymentDalTest {
    private val tables = arrayOf(EventTable)
    private val db = Database
            .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
            .also {
                TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                transaction(it) {
                    addLogger(StdOutSqlLogger)
                    SchemaUtils.drop(*tables)
                    SchemaUtils.create(*tables)
                }
            }

    private val dal = PaymentDal(db)

    @Test
    fun `should only persist the payments difference`() {
        val event1 = InvoicePayCommitFailedEvent(invoiceID = 1, timestamp = 123, reason = "foo")
        val pay = Payment(listOf<PaymentEvent>(PaymentEvent(event1.invoiceID, event1.topic(), event1)))

        val event2 = InvoicePayCommitSucceedEvent(invoiceID = 1, timestamp = 12)
        val event3 = InvoicePayCommitFailedEvent(invoiceID = 1, timestamp = 12345, reason = "bar")
        pay.add(PaymentEvent(event2.invoiceID, event2.topic(), event2))
        pay.add(PaymentEvent(event3.invoiceID, event3.topic(), event3))

        dal.persistChanges(pay)

        val expectedPayments = listOf(
                PaymentEvent(event2.invoiceID, event2.topic(), event2),
                PaymentEvent(event3.invoiceID, event3.topic(), event3)
        )

        assertEquals(expectedPayments, dal.fetchPaymentAggregation(1).initialSet())
    }
}