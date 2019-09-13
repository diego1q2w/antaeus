package io.pleo.antaeus.rabbitmq

import com.rabbitmq.client.Delivery

typealias pleoHandler = (Message) -> Unit
typealias handler = suspend (Delivery) -> Unit

fun NewHandler(h: pleoHandler): handler {
    return {
        h(Message(msg = it.body.toString(), topic = it.envelope.routingKey))
    }
}