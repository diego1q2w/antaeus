package io.pleo.antaeus.retrier.retry.domain

data class PaymentStatus(val invoiceId: Int, val timestamp: Long, val status: Status)