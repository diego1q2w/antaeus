@file:JvmName("PleoRetrier")

package io.pleo.antaeus.retrier

import io.pleo.antaeus.retrier.payment.delivery.http.AntaeusRest

fun main() {
    AntaeusRest().run()
}