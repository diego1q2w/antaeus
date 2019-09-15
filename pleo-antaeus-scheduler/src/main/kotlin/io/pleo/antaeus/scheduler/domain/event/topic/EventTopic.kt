package io.pleo.antaeus.scheduler.domain.event.topic

enum class EventTopic{
    MonthlyEvent,
    InvoiceScheduledEvent,
    InvoicePayCommitSucceedEvent,
    InvoicePayCommitFailedEvent,
    InvoicePayRetryApprovedEvent,
    InvoicePayRetryDisApprovedEvent
}