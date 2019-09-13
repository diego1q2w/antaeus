/*
    Implements endpoints related to customers.
 */

package io.pleo.antaeus.scheduler.app.services

import io.pleo.antaeus.scheduler.app.exceptions.CustomerNotFoundException
import io.pleo.antaeus.scheduler.domain.Customer
import io.pleo.antaeus.scheduler.infra.db.AntaeusDal

class CustomerService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Customer> {
       return dal.fetchCustomers()
    }

    fun fetch(id: Int): Customer {
        return dal.fetchCustomer(id) ?: throw CustomerNotFoundException(id)
    }
}
