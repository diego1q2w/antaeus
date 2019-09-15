@file:JvmName("PleoRetrier")

package io.pleo.antaeus.retrier

import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.retrier.retry.app.services.HealthCheckService
import io.pleo.antaeus.retrier.retry.app.services.RetryService
import io.pleo.antaeus.retrier.retry.delivery.bus.invoicePayCommitFailedHandler
import io.pleo.antaeus.retrier.retry.delivery.bus.invoicePayCommitSucceedHandler
import io.pleo.antaeus.retrier.retry.delivery.http.AntaeusRest
import io.pleo.antaeus.retrier.retry.domain.event.topic.EventTopic
import io.pleo.antaeus.retrier.retry.infra.db.EventTable
import io.pleo.antaeus.retrier.retry.infra.db.PaymentDal
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.Exception
import java.sql.Connection
import java.time.LocalDateTime

fun main() {
    val serviceName = System.getenv("SERVICE_NAME") ?: throw Exception("No SERVICE_NAME env provided")

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

    val retryService = RetryService(dal, bus, LocalDateTime::now, 2)

    bus.registerHandler(serviceName,
            EventTopic.InvoicePayCommitFailedEvent.name, invoicePayCommitFailedHandler(retryService))
    bus.registerHandler(serviceName,
            EventTopic.InvoicePayCommitSucceedEvent.name, invoicePayCommitSucceedHandler(retryService))

    bus.run()

    val healthCheckService = HealthCheckService()
    healthCheckService.addHealthCheck("bus", bus::isHealthy)

    AntaeusRest(healthCheckService = healthCheckService).run()
}