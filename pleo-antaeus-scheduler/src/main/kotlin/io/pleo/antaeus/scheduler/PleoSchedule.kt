/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("PleoSchedule")

package io.pleo.antaeus.scheduler

import io.pleo.antaeus.rabbitmq.Bus
import io.pleo.antaeus.scheduler.app.services.BillingService
import io.pleo.antaeus.scheduler.app.services.CustomerService
import io.pleo.antaeus.scheduler.app.services.HealthCheckService
import io.pleo.antaeus.scheduler.app.services.InvoiceService
import io.pleo.antaeus.scheduler.delivery.bus.invoiceScheduledHandler
import io.pleo.antaeus.scheduler.delivery.bus.monthlyHandler
import io.pleo.antaeus.scheduler.delivery.http.AntaeusRest
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal
import io.pleo.antaeus.scheduler.infra.db.CustomerTable
import io.pleo.antaeus.scheduler.infra.db.InvoiceTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.time.LocalDateTime
import kotlin.concurrent.fixedRateTimer

fun main() {
    //TODO: move it to an env var
    val serviceName = "scheduler"

    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
            .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
            .also {
                TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                transaction(it) {
                    addLogger(StdOutSqlLogger)
                    // Drop all existing tables to ensure a clean slate on each run
                    SchemaUtils.drop(*tables)
                    // Create all tables
                    SchemaUtils.create(*tables)
                }
            }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()

    //Set up bus
    val bus = Bus()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(
            paymentProvider = paymentProvider,
            dal = dal,
            bus = bus,
            now = LocalDateTime::now
    )

    // Bus handlers
    bus.registerHandler(serviceName, "InvoiceScheduledEvent", invoiceScheduledHandler(billingService))
    bus.registerHandler(serviceName, "MonthlyEvent", monthlyHandler(billingService))

    // Process pending payments every 5 minutes
    fixedRateTimer("processPayments", true, 2000L, 5000){
        billingService.processPayments()
    }

    // Run the bus handlers async
    bus.run()

    // Set up the health check
    val healthCheckService = HealthCheckService()

    healthCheckService.addHealthCheck("bus", bus::isHealthy)

    // Create REST web service
    AntaeusRest(
            invoiceService = invoiceService,
            customerService = customerService,
            healthCheckService = healthCheckService
    ).run()
}