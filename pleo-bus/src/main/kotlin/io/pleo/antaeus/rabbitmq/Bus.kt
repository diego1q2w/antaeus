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
    private var handlers = mutableMapOf<String, Pair<Queue, pleoHandler> >()
    private var isRunning = false
    private lateinit var bucket: String

    init {
        val factory = ConnectionFactory()
        val uri = System.getenv("RABBITMQ_HOSTNAME") ?: throw Exception("No RABBITMQ_HOSTNAME env provided")
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
                channel.basicNack(it.envelope.deliveryTag, false, false)
                throw e
            }
        }
    }

    fun registerHandler(bucket: String, topic: String, handler: pleoHandler) {
        this.bucket = bucket
        handlers[topic] = (Queue(bucket, topic) to handler)
    }

    fun run() {
        if (isRunning && handlers.keys.size == 0) {
            return
        }

        connection.createTopology(bucket)

        isRunning = true
        handlers.forEach {(_, register) ->
            GlobalScope.launch {
                runHandler(register)
            }
        }
    }

    private suspend fun runHandler(register: Pair<Queue, pleoHandler>) {
        val (queue, handler) = register
        connection.channel {
            val channel = this
            this.createQueue(queue)

            consume(queue.name(), prefetchSize) {
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
                // When any of the consumers fails, will stop the rest of them and mark the service as unhealthy
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