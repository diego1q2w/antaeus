package io.pleo.antaeus.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.*
import java.lang.Exception

typealias handler = suspend (Delivery) -> Unit

class Bus(private val prefetchSize: Int = 10) {
    private lateinit var connection: Connection
    private var handlers = mutableMapOf<String, handler >()
    private var isRunning = false

    init {
        val factory = ConnectionFactory()
        val uri = System.getenv("RABBITMQ_HOSTNAME") ?: throw Exception("No RabbitMQ uri provided")
        factory.setUri(uri)
        connection = factory.newConnection()
    }

    fun isHealthy(): Boolean {
        return connection.isOpen
    }

    fun registerHandler(bucket: String, topic: String, handler: handler) {
        handlers["$bucket:$topic"] = handler
    }

    fun run() {
        if (isRunning) {
            return
        }

        isRunning = true
        handlers.forEach {
            GlobalScope.launch {
                connection.channel {
                    val handler = it.value
                    consume(it.key, prefetchSize) {
                        consumeMessageWithConfirm(handler)
                    }
                }
            }
        }
    }

    fun publishMessage(event: Event) {
        runBlocking {
            connection.confirmChannel {
                publish {
                    val message = OutboundMessage(exchange = "bus", routingKey = event.topic(), msg = event.toJSON(), properties = AMQP.BasicProperties())
                    publishWithConfirm(message)
                }
            }
        }
    }
}