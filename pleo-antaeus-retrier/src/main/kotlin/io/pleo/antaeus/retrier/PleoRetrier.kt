@file:JvmName("PleoRetrier")

package io.pleo.antaeus.retrier

import io.pleo.antaeus.retrier.delivery.http.AntaeusRest

fun main() {
    AntaeusRest().run()
}