package io.pleo.antaeus.scheduler.app.exceptions

class InvoiceNotFoundException(id: Int) : EntityNotFoundException("Invoice", id)