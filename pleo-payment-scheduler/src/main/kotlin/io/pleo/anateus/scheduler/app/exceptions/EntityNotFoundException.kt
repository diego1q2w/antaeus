package io.pleo.anateus.scheduler.app.exceptions

abstract class EntityNotFoundException(entity: String, id: Int) : Exception("$entity '$id' was not found")