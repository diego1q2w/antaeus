package io.pleo.antaeus.retrier.retry.domain

import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitFailedEvent
import io.pleo.antaeus.retrier.retry.domain.event.InvoicePayCommitSucceedEvent
import io.pleo.antaeus.retrier.retry.domain.event.PayEvent

class Payment(private val initEvents: List<PaymentEvent>) {
    private var status: PaymentStatus? = null
    private var newEvents = mutableListOf<PaymentEvent>()

    init {
        apply()
    }

    fun add(event: PaymentEvent) {
        newEvents.add(event)
        apply()
    }

    private fun apply() {
        initEvents.plus(newEvents).sortedBy { it.event.timestamp }.forEach {
            when(it.event) {
                is InvoicePayCommitSucceedEvent -> apply(it.event)
                is InvoicePayCommitFailedEvent -> apply(it.event)
            }
        }

    }

    private fun apply(event: InvoicePayCommitSucceedEvent) {
        status = PaymentStatus(invoiceId = event.invoiceID, timestamp = event.timestamp, status = Status.SUCCEED)
    }

    private fun apply(event: InvoicePayCommitFailedEvent) {
        status = PaymentStatus(invoiceId = event.invoiceID, timestamp = event.timestamp, status = Status.FAILED)
    }

    fun totalChanges(): Int {
        return initEvents.size + newEvents.size
    }

    fun difference(): List<PaymentEvent> {
        return newEvents
    }

    fun initialSet(): List<PaymentEvent> {
        return initEvents
    }

    fun finalStatus(): Status? {
        return status?.status
    }
}