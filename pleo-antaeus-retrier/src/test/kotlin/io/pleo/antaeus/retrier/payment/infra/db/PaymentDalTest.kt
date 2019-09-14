package io.pleo.antaeus.retrier.payment.infra.db

import io.pleo.antaeus.retrier.payment.domain.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
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
    fun `should get the correct events`() {
        listOf<PayEvent>(
                InvoicePayCommitFailedEvent(invoiceID = 1, timestamp = 123, reason = "foo"),
                InvoicePayCommitSucceedEvent(invoiceID = 1, timestamp = 12),
                InvoicePayCommitFailedEvent(invoiceID = 4, timestamp = 12345, reason = "bar")
        ).forEach {
            when(it) {
                is InvoicePayCommitFailedEvent -> dal.addEvent(it)
                is InvoicePayCommitSucceedEvent -> dal.addEvent(it)
            }
        }

        val expectedPayments = listOf(
                InvoicePayCommitFailedEvent(invoiceID = 1, timestamp = 123, reason = "foo"),
                InvoicePayCommitSucceedEvent(invoiceID = 1, timestamp = 12))

        Assertions.assertEquals(expectedPayments, dal.fetchEvents(1))
    }
}