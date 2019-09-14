package io.pleo.antaeus.retrier.payment.domain

data class PaymentStatus(val invoiceId: Int, val timestamp: Long, val status: Status)