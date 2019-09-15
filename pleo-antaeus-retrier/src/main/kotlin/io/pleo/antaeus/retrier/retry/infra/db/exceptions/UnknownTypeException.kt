package io.pleo.antaeus.retrier.retry.infra.db.exceptions

class UnknownTypeException(private val topic: String): Exception("Unknown payment type $topic")