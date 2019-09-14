package io.pleo.antaeus.retrier.payment.infra.db

import io.pleo.antaeus.retrier.payment.domain.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.payment.domain.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.payment.domain.PayEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class PaymentDal(private val db: Database) {

    fun fetchEvents(invoiceId: Int): List<PayEvent> = transaction(db) {
        EventTable
                .select { EventTable.invoiceId eq invoiceId }
                .map { it.toEvent() }
    }

    fun addEvent(event: InvoicePayCommitSucceedEvent): Int? = transaction(db) {
        EventTable.insert {
            it[invoiceId] = event.invoiceID
            it[type] = event::class.simpleName!!
            it[EventTable.event] = Json(JsonConfiguration.Stable).stringify(InvoicePayCommitSucceedEvent.serializer(), event)
        } get EventTable.id
    }

    fun addEvent(event: InvoicePayCommitFailedEvent): Int? = transaction(db) {
        EventTable.insert {
            it[invoiceId] = event.invoiceID
            it[type] = event::class.simpleName!!
            it[EventTable.event] = Json(JsonConfiguration.Stable).stringify(InvoicePayCommitFailedEvent.serializer(), event)
        } get EventTable.id
    }
}
