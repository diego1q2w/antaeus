/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.retrier.retry.infra.db

import org.jetbrains.exposed.sql.Table

object EventTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val type = varchar("type", 80)
    val invoiceId = integer("invoice_id")
    val event = text("event")
}