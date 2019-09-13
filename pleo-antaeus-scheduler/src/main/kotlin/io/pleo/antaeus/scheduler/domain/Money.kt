package io.pleo.antaeus.scheduler.domain

import java.math.BigDecimal

data class Money(
    val value: BigDecimal,
    val currency: Currency
)
