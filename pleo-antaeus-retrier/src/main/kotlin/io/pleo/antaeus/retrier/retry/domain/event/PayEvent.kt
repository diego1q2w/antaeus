package io.pleo.antaeus.retrier.retry.domain.event

interface PayEvent {
    val timestamp: Long
    val invoiceID: Int
}