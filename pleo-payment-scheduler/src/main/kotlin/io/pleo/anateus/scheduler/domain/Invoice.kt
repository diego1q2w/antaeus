package io.pleo.anateus.scheduler.domain

data class Invoice(
    val id: Int,
    val customerId: Int,
    val amount: Money,
    val status: InvoiceStatus
)
