package io.pleo.antaeus.rabbitmq

class Queue(val bucket: String, val topic: String) {

    fun name(): String {
        return "$bucket:$topic"
    }

    fun nameDlx(): String {
        return "$bucket:${topic}_dlx"
    }

    fun bucketDlx(): String {
        return "${bucket}-dlx"
    }

    fun arguments(): MutableMap<String?, Any?>?{
        return mutableMapOf(
                "x-expires" to 24*60*60*1000, // 1 day
                "x-dead-letter-exchange" to "bus-dlx",
                "x-dead-letter-routing-key" to name()
        )
    }
}