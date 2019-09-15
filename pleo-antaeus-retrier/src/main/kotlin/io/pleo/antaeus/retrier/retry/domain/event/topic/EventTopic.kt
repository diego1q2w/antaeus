package io.pleo.antaeus.retrier.retry.domain.event.topic

enum class EventTopic {
    InvoicePayCommitFailedEvent,
    InvoicePayCommitSucceedEvent,
    InvoicePayRetryApprovedEvent,
    InvoicePayRetryDisApprovedEvent
}