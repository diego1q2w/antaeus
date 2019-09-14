package io.pleo.antaeus.scheduler.app.services

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
        return services.
                map { (name, isHealthy) -> Pair(name, isHealthy()) }
                .fold(Pair(true, mutableListOf<ServiceHealth>())) {
                    (accStatus, accServices), (name, isHealthy) ->
                    accServices.add(ServiceHealth(name, isHealthy))
                    Pair(accStatus && isHealthy, accServices)
                }.let { (isHealthy, statuses) ->
                    val json = Json(JsonConfiguration.Stable)
                    Pair(isHealthy, json.stringify(ServiceHealth.serializer().list, statuses))
                }
    }
}