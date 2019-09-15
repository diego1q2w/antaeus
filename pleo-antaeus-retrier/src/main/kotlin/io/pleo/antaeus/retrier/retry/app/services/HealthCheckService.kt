package io.pleo.antaeus.retrier.retry.app.services

typealias health = () -> Boolean

class HealthCheckService {
    private var services = mutableMapOf<String, health>()

    fun addHealthCheck(name: String, isHealthy: health) {
        services[name] = isHealthy
    }

    fun isHealthy(): Pair<Boolean, List<Map<String, Boolean>>> {
        return services
                .map { (name, isHealthy) -> Pair(name, isHealthy()) }
                .fold(Pair(true, listOf()))
                { (accStatus, accServices), (name, isHealthy) ->
                    Pair(accStatus && isHealthy, accServices.plus(mapOf(name to isHealthy)))
                }
    }
}