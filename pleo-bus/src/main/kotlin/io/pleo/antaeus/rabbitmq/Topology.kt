package io.pleo.antaeus.rabbitmq

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.viartemev.thewhiterabbit.channel.channel
import com.viartemev.thewhiterabbit.exchange.BindExchangeSpecification
import com.viartemev.thewhiterabbit.exchange.bindExchange
import kotlinx.coroutines.runBlocking

fun Connection.createTopology(bucket: String) {
    val connection = this
    runBlocking {
        connection.channel {
            Topology(this, bucket).create()
        }

    }
}

fun Channel.createQueue(queue: Queue){
    this.queueDeclare(queue.name(), true, false, false,  queue.arguments())
    this.queueBind(queue.name(), queue.bucket, queue.topic)

    this.queueDeclare(queue.nameDlx(), true, false, false,  null)
    this.queueBind(queue.nameDlx(), queue.bucketDlx(), queue.name())
}

class Topology(private val channel: Channel, private val bucket: String) {

    fun create() {
        createExchange()
        createExchangeDlx()
    }

    private fun createExchange() {
        channel.exchangeDeclare("bus", BuiltinExchangeType.FANOUT, true)
        channel.exchangeDeclare(bucket, BuiltinExchangeType.TOPIC, true)

        runBlocking {
            channel.bindExchange(BindExchangeSpecification(source = "bus", destination = bucket, routingKey = ""))
        }
    }

    private fun createExchangeDlx() {
        channel.exchangeDeclare("bus-dlx", BuiltinExchangeType.FANOUT, true)
        channel.exchangeDeclare("$bucket-dlx", BuiltinExchangeType.TOPIC, true)

        runBlocking {
            channel.bindExchange(BindExchangeSpecification(source = "bus-dlx", destination = "$bucket-dlx", routingKey = ""))
        }
    }
}