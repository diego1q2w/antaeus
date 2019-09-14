package io.pleo.antaeus.rabbitmq

import com.rabbitmq.client.*
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.channel.confirmChannel
import com.viartemev.thewhiterabbit.channel.consume
import com.viartemev.thewhiterabbit.channel.publish
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import io.pleo.antaeus.rabbitmq.exceptions.RejectedMessageException
import kotlinx.coroutines.*
import java.lang.Exception

typealias pleoHandler = (Message) -> Unit
typealias handler = suspend (Delivery) -> Unit

class Bus(private val prefetchSize: Int = 10) {
    private var connection: Connection
    private var handlers = mutableMapOf<String, pleoHandler >()
    private var isRunning = false

    init {
        val factory = ConnectionFactory()
        val uri = System.getenv("RABBITMQ_HOSTNAME") ?: throw Exception("No RabbitMQ uri provided")
        factory.setUri(uri)
        connection = factory.newConnection()
    }

    fun isHealthy(): Boolean {
        return connection.isOpen && isRunning
    }

    private fun deliverHandler(h: pleoHandler, channel: Channel): handler {
        return {
            try {
                h(Message(msg = it.body.toString(Charsets.UTF_8), topic = it.envelope.routingKey))
            } catch (e: RejectedMessageException) {
                channel.basicReject(it.envelope.deliveryTag, true)
                throw e
            }
        }
    }

    fun registerHandler(bucket: String, topic: String, handler: pleoHandler) {
        handlers["$bucket:$topic"] = handler
    }

    fun run() {
        if (isRunning) {
            return
        }

        isRunning = true
        handlers.forEach {(topic, handler) ->
            GlobalScope.launch {
                runHandler(topic, handler)
            }
        }
    }

    private suspend fun runHandler(topic: String, handler: pleoHandler) {
        connection.channel {
            val channel = this
            consume(topic, prefetchSize) {
                consumeAlways@ while (isRunning) {
                    try {
                        consumeMessageWithConfirm(deliverHandler(handler, channel))
                    } catch (e: Exception) {
                        when(e) {
                            is CancellationException -> break@consumeAlways
                            else -> continue@consumeAlways
                        }
                    }
                }
                // When any of the consumers fails, will stop the rest of them plus will mark the service as unhealthy
                isRunning = false
            }
        }
    }

    fun publishMessage(event: Event) {
        runBlocking {
            connection.confirmChannel {
                publish {
                    OutboundMessage(
                            exchange = "bus",
                            routingKey = event.topic(),
                            msg = event.toJSON(),
                            properties = AMQP.BasicProperties()
                    ).let {
                        publishWithConfirm(it)
                    }
                }
            }
        }
    }
}