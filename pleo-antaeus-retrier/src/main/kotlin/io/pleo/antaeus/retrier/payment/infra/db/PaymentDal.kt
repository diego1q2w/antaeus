package io.pleo.antaeus.retrier.payment.infra.db

import io.pleo.antaeus.retrier.payment.domain.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.payment.domain.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.payment.domain.PayEvent
import io.pleo.antaeus.retrier.payment.domain.Payment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import io.pleo.antaeus.retrier.payment.infra.db.exceptions.UnknownTypeException

class PaymentDal(private val db: Database) {

    fun fetchEvents(invoiceId: Int): Payment {
        transaction(db) {
            EventTable
                    .select { EventTable.invoiceId eq invoiceId }
                    .map { it.toEvent() }
        }.let {
            return Payment(it)
        }
    }

    fun persistChanges(payment: Payment): Unit = payment.difference().forEach {addEvent(it)}

    private fun addEvent(event: PayEvent): Int {
        val eventJson = when(event) {
            is InvoicePayCommitSucceedEvent ->
                Json(JsonConfiguration.Stable).stringify(InvoicePayCommitSucceedEvent.serializer(), event)
            is InvoicePayCommitFailedEvent ->
                Json(JsonConfiguration.Stable).stringify(InvoicePayCommitFailedEvent.serializer(), event)
            else -> throw UnknownTypeException(event::class.simpleName!!)
        }

        return addEvent(event, eventJson) ?: 0
    }

    private fun addEvent(event: PayEvent, eventJson: String): Int? = transaction(db) {
        EventTable.insert {
            it[invoiceId] = event.invoiceID
            it[type] = event::class.simpleName!!
            it[EventTable.event] = eventJson
        } get EventTable.id
    }
}
