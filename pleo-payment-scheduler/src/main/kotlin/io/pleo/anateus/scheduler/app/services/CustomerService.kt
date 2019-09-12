/*
    Implements endpoints related to customers.
 */

package io.pleo.anateus.scheduler.app.services

import io.pleo.anateus.scheduler.app.exceptions.CustomerNotFoundException
import io.pleo.anateus.scheduler.domain.Customer
import io.pleo.anateus.scheduler.infra.db.AntaeusDal

class CustomerService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Customer> {
       return dal.fetchCustomers()
    }

    fun fetch(id: Int): Customer {
        return dal.fetchCustomer(id) ?: throw CustomerNotFoundException(id)
    }
}
