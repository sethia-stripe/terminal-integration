package com.sethia.terminalintegration.payments.network

import com.sethia.terminalintegration.BuildConfig
import com.stripe.Stripe
import com.stripe.model.PaymentIntent
import com.stripe.model.terminal.ConnectionToken
import com.stripe.net.RequestOptions
import com.stripe.stripeterminal.external.models.ConnectionTokenException

/**
 * The `ApiClient` is a singleton object used to fake calls to a merchant backend and return
 * their results
 */
object ApiClient {
    private val DEFAULT_KEY = BuildConfig.STRIPE_API_KEY

    private const val INTERAC_PRESENT = "interac_present"
    private const val CARD_PRESENT = "card_present"

    private var secretKey: String = DEFAULT_KEY
    private var directChargeAccountId: String = ""

    @Throws(ConnectionTokenException::class)
    internal fun createConnectionToken(): String {
        try {
            Stripe.apiKey = secretKey

            val requestOptionsBuilder = RequestOptions.builder()

            if (directChargeAccountId.isNotBlank()) {
                requestOptionsBuilder.stripeAccount = directChargeAccountId
            }

            val connectionToken = ConnectionToken.create(mapOf(), requestOptionsBuilder.build())
            return connectionToken.secret
        } catch (e: Exception) {
            throw ConnectionTokenException("Creating connection token failed", e)
        }
    }

    internal fun cancelPaymentIntent(id: String): PaymentIntent {
        val requestOptions = RequestOptions.builder().run {
            if (directChargeAccountId.isNotBlank()) {
                stripeAccount = directChargeAccountId
            }

            build()
        }

        return PaymentIntent.retrieve(id, requestOptions).cancel(requestOptions)
    }

    internal fun capturePaymentIntent(id: String): PaymentIntent {
        val requestOptions = RequestOptions.builder().run {
            if (directChargeAccountId.isNotBlank()) {
                stripeAccount = directChargeAccountId
            }

            build()
        }

        return PaymentIntent.retrieve(id, requestOptions).capture(requestOptions)
    }

    class ApiException(cause: Throwable) : Exception(cause)
}