package io.pleo.antaeus.retrier.payment.domain

class Payment(private val initEvents: List<PayEvent>) {
    private var status: PaymentStatus? = null
    private var newEvents = mutableListOf<PayEvent>()

    init {
        apply()
    }

    fun add(event: PayEvent) {
        newEvents.add(event)
        apply()
    }

    private fun apply() {
        initEvents.plus(newEvents).sortedBy { it.timestamp }.forEach {
            when(it) {
                is InvoicePayCommitSucceedEvent -> apply(it)
                is InvoicePayCommitFailedEvent -> apply(it)
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

    fun difference(): MutableList<PayEvent> {
        return newEvents
    }

    fun finalStatus(): PaymentStatus? {
        return status
    }
}