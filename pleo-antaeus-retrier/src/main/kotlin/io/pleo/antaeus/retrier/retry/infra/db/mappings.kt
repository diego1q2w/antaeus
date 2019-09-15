/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.retrier.retry.infra.db

import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.retry.domain.event.PayEvent
import io.pleo.antaeus.retrier.retry.infra.db.exceptions.UnknownTypeException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toEvent(): PayEvent {
    val event = this[EventTable.event]

    return when(val type = this[EventTable.type]) {
        InvoicePayCommitSucceedEvent::class.simpleName!! ->
            Json(JsonConfiguration.Stable).parse(InvoicePayCommitSucceedEvent.serializer(), event)
        InvoicePayCommitFailedEvent::class.simpleName!! ->
            Json(JsonConfiguration.Stable).parse(InvoicePayCommitFailedEvent.serializer(), event)
        else -> throw UnknownTypeException(type)
    }
}