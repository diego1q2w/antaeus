package io.pleo.antaeus.scheduler.app.exceptions

class CustomerNotFoundException(id: Int) : EntityNotFoundException("Customer", id)