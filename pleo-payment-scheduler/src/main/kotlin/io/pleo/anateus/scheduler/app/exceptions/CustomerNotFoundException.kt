package io.pleo.anateus.scheduler.app.exceptions

class CustomerNotFoundException(id: Int) : EntityNotFoundException("Customer", id)