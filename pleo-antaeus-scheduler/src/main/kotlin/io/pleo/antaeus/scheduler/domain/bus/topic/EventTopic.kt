package io.pleo.antaeus.scheduler.domain.bus.topic

enum class EventTopic{
    MonthlyEvent,
    InvoiceScheduledEvent,
    InvoicePayCommitSucceedEvent,
    InvoicePayCommitFailedEvent,
    InvoicePayRetryApprovedEvent,
    InvoicePayRetryDisApprovedEvent
}