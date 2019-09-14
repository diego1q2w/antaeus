@file:JvmName("PleoRetrier")

package io.pleo.antaeus.retrier

import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.retrier.payment.app.services.HealthCheckService
import io.pleo.antaeus.retrier.payment.app.services.PaymentService
import io.pleo.antaeus.retrier.payment.delivery.bus.invoicePayCommitFailedHandler
import io.pleo.antaeus.retrier.payment.delivery.bus.invoicePayCommitSucceedHandler
import io.pleo.antaeus.retrier.payment.delivery.http.AntaeusRest
import io.pleo.antaeus.retrier.payment.infra.db.EventTable
import io.pleo.antaeus.retrier.payment.infra.db.PaymentDal
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.time.LocalDateTime

fun main() {
    val tables = arrayOf(EventTable)
    val db = Database
            .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
            .also {
                TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                transaction(it) {
                    addLogger(StdOutSqlLogger)
                    SchemaUtils.drop(*tables)
                    SchemaUtils.create(*tables)
                }
            }

    val dal = PaymentDal(db)
    val bus = Bus()

    val paymentService = PaymentService(dal, bus, LocalDateTime::now, 2)

    bus.registerHandler("retrier", "InvoicePayCommitFailedEvent", invoicePayCommitFailedHandler(paymentService))
    bus.registerHandler("retrier", "InvoicePayCommitSucceedEvent", invoicePayCommitSucceedHandler(paymentService))

    bus.run()

    val healthCheckService = HealthCheckService()
    healthCheckService.addHealthCheck("bus", bus::isHealthy)

    AntaeusRest(healthCheckService = healthCheckService).run()
}