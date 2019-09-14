package io.pleo.antaeus.retrier.payment.infra.db.exceptions

class UnknownTypeException(private val topic: String): Exception("Unknown payment type $topic")