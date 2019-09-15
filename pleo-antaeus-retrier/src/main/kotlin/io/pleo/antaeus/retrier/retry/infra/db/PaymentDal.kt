package io.pleo.antaeus.retrier.retry.infra.db

import io.pleo.antaeus.retrier.retry.domain.*
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.retry.domain.event.PayEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import io.pleo.antaeus.retrier.retry.infra.db.exceptions.UnknownTypeException

class PaymentDal(private val db: Database) {

    fun fetchPaymentAggregation(invoiceId: Int): Payment = Payment(fetchEvents(invoiceId))

    fun fetchEvents(invoiceId: Int): List<PaymentEvent> = transaction(db) {
            EventTable
                    .select { EventTable.invoiceId eq invoiceId }
                    .map { it.toEvent() }
    }

    fun persistChanges(payment: Payment): Unit = payment.difference().forEach {addEvent(it)}

    private fun addEvent(event: PaymentEvent): Int {
        val eventJson = when(event.event) {
            is InvoicePayCommitSucceedEvent ->
                Json(JsonConfiguration.Stable).stringify(InvoicePayCommitSucceedEvent.serializer(), event.event)
            is InvoicePayCommitFailedEvent ->
                Json(JsonConfiguration.Stable).stringify(InvoicePayCommitFailedEvent.serializer(), event.event)
            else -> throw UnknownTypeException(event.event::class.simpleName!!)
        }

        return addEvent(event, eventJson) ?: 0
    }

    private fun addEvent(event: PaymentEvent, eventJson: String): Int? = transaction(db) {
        EventTable.insert {
            it[invoiceId] = event.invoiceId
            it[type] = event.event::class.simpleName!!
            it[EventTable.event] = eventJson
        } get EventTable.id
    }
}
