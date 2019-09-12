package io.pleo.anateus.scheduler.app.external

interface Bus {
    fun publish(message: String)
}