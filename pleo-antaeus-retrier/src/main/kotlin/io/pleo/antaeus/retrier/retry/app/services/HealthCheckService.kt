package io.pleo.antaeus.retrier.retry.app.services

import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list

typealias health = () -> Boolean

@Serializable
private data class ServiceHealth(val name: String, val isHealthy: Boolean)

class HealthCheckService {
    private var services = mutableMapOf<String, health>()

    fun addHealthCheck(name: String, isHealthy: health) {
        services[name] = isHealthy
    }

    fun isHealthy(): Pair<Boolean, String> {
        val checkedServices = services.map { (name, isHealthy) -> Pair(name, isHealthy()) }

        return checkedServices.fold(Pair(true, listOf<ServiceHealth>()))
        { (accStatus, accServices), (name, isHealthy) ->
            Pair(accStatus && isHealthy, accServices.plus(ServiceHealth(name, isHealthy)))
        }.let { (isHealthy, body) ->
            Pair(isHealthy, Json(JsonConfiguration.Stable).stringify(ServiceHealth.serializer().list, body))
        }

    }
}