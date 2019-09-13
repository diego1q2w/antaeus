package io.pleo.antaeus.rabbitmq

abstract class Event {
    abstract fun toJSON(): String

    fun topic(): String {
        return this::class.simpleName ?: "Noname"
    }
}