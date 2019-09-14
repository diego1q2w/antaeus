package io.pleo.antaeus.retrier.payment.domain

class Payment(private val events: List<PayEvent>) {
    private var changes: Int = 0
    private var status: PaymentStatus? = null

    init {
        events.forEach {
            when(it) {
                is InvoicePayCommitSucceedEvent -> apply(it)
                is InvoicePayCommitFailedEvent -> apply(it)
            }
        }
    }

    fun apply(event: InvoicePayCommitSucceedEvent) {
        changes++
        status = PaymentStatus(invoiceId = event.invoiceID, timestamp = event.timestamp, status = Status.SUCCEED)
    }

    fun apply(event: InvoicePayCommitFailedEvent) {
        changes++
        status = PaymentStatus(invoiceId = event.invoiceID, timestamp = event.timestamp, status = Status.FAILED)
    }

    fun totalChanges(): Int {
        return changes
    }

    fun finalStatus(): PaymentStatus? {
        return status
    }
}