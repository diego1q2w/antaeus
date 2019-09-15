package io.pleo.antaeus.retrier.notification.app

import mu.KotlinLogging

class NotificationService {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    // This could be an email or a push notification :p
    fun paymentTriesExceed(invoiceId: Int, tries: Int) {
        logger.info { "Dear customer your invoice with ID '$invoiceId' has exceed the limit of $tries payment attempts. Please check your account balance"
        }
    }
}