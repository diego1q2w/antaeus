package io.pleo.antaeus.scheduler.domain

enum class InvoiceStatus {
    PENDING,
    SCHEDULED,
    PROCESSING,
    RETRY,
    PAID
}
