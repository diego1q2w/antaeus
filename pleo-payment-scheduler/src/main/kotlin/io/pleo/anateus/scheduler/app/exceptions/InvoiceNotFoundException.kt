package io.pleo.anateus.scheduler.app.exceptions

class InvoiceNotFoundException(id: Int) : EntityNotFoundException("Invoice", id)