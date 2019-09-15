package io.pleo.antaeus.rabbitmq

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.viartemev.thewhiterabbit.exchange.BindExchangeSpecification
import com.viartemev.thewhiterabbit.exchange.bindExchange
import kotlinx.coroutines.runBlocking

fun Channel.createTopology(queue: Queue) {
    Topology(this, queue).create()
}

class Topology(private val channel: Channel, private val queue: Queue) {

    fun create() {
        createExchange()
        createExchangeDlx()
        createQueue()
        createQueueDlx()
    }

    private fun createExchange() {
        channel.exchangeDeclare("bus", BuiltinExchangeType.FANOUT, true)
        channel.exchangeDeclare(queue.bucket, BuiltinExchangeType.TOPIC, true)

        runBlocking {
            channel.bindExchange(BindExchangeSpecification(source = "bus", destination = queue.bucket, routingKey = ""))
        }
    }

    private fun createExchangeDlx() {
        channel.exchangeDeclare("bus-dlx", BuiltinExchangeType.FANOUT, true)
        channel.exchangeDeclare(queue.bucketDlx(), BuiltinExchangeType.TOPIC, true)

        runBlocking {
            channel.bindExchange(BindExchangeSpecification(source = "bus-dlx", destination = queue.bucketDlx(), routingKey = ""))
        }
    }

    private fun createQueue() {
        channel.queueDeclare(queue.name(), true, false, false,  queue.arguments())
        channel.queueBind(queue.name(), queue.bucket, queue.topic)
    }

    private fun createQueueDlx() {
        channel.queueDeclare(queue.nameDlx(), true, false, false,  queue.argumentsDlx())
        channel.queueBind(queue.name(), queue.bucketDlx(), queue.topic)
    }
}