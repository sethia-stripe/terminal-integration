package com.example.terminalintegration.payments.network

import com.example.terminalintegration.BuildConfig
import com.stripe.Stripe
import com.stripe.model.Account
import com.stripe.model.PaymentIntent
import com.stripe.model.SetupIntent
import com.stripe.model.terminal.ConnectionToken
import com.stripe.net.RequestOptions
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.param.PaymentIntentCreateParams.CaptureMethod.AUTOMATIC
import com.stripe.param.PaymentIntentCreateParams.CaptureMethod.MANUAL
import com.stripe.param.SetupIntentCreateParams
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.RoutingPriority

/**
 * The `ApiClient` is a singleton object used to fake calls to a merchant backend and return
 * their results
 */
object ApiClient {

    // Make sure there's a default value here, in case `stripe_api_key` isn't
    // provided in the `local.properties` file.
    private val DEFAULT_KEY = BuildConfig.STRIPE_API_KEY

    private const val INTERAC_PRESENT = "interac_present"
    private const val CARD_PRESENT = "card_present"

    private var secretKey: String = DEFAULT_KEY
    private var directChargeAccountId: String = ""

    @Throws(ApiException::class)
    internal fun updateAccountInfo(secretKey: String, directChargeAccountId: String) {
        ApiClient.secretKey = secretKey
        ApiClient.directChargeAccountId = directChargeAccountId

        try {
            createConnectionToken()
        } catch (e: ConnectionTokenException) {
            // Reset to default so that everything continues to work
            ApiClient.secretKey = DEFAULT_KEY

            throw ApiException(e)
        }
    }

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

    internal fun retrieveAccount(): Account {
        return try {
            Account.retrieve()
        } catch (e: Exception) {
            Account().apply {
                id = "N/A"
                country = "N/A"
                defaultCurrency = "N/A"
            }
        }
    }

    internal fun createPaymentIntent(
        amount: Long,
        currency: String,
        extendedAuth: Boolean,
        incrementalAuth: Boolean,
        destinationAccountId: String,
        applicationFee: Long,
        directChargeAccountId: String,
        autocapture: Boolean,
        routingPriority: RoutingPriority?,
    ): PaymentIntent {
        val cardPresentParams = buildMap<String, Any> {
            if (extendedAuth) {
                put("request_extended_authorization", true)
            }
            if (incrementalAuth) {
                put("request_incremental_authorization_support", true)
            }
            if (routingPriority != null) {
                put("routing", mapOf("requested_priority" to routingPriority.name.lowercase()))
            }
        }

        val paymentMethodOptions =
            PaymentIntentCreateParams.PaymentMethodOptions.Builder()
                .putExtraParam("card_present", cardPresentParams)
                .build()

        val paramBuilder = PaymentIntentCreateParams.Builder()
            .setAmount(amount)
            .setCurrency(currency)
            .addPaymentMethodType(CARD_PRESENT)
            .setPaymentMethodOptions(paymentMethodOptions)
            .setCaptureMethod(if (autocapture) AUTOMATIC else MANUAL)

        if (destinationAccountId.isNotEmpty()) {
            paramBuilder.setTransferData(
                PaymentIntentCreateParams.TransferData.Builder()
                    .setDestination(destinationAccountId).build()
            )
            paramBuilder.setOnBehalfOf(destinationAccountId)
        }

        if (applicationFee > 0) {
            paramBuilder.setApplicationFeeAmount(applicationFee)
        }

        if (currency.lowercase() == "cad") {
            paramBuilder.addPaymentMethodType(INTERAC_PRESENT)
        }

        val requestOptionsBuilder = RequestOptions.builder()
        if (directChargeAccountId.isNotBlank()) {
            requestOptionsBuilder.stripeAccount = directChargeAccountId
        }

        return PaymentIntent.create(paramBuilder.build(), requestOptionsBuilder.build())
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

    internal fun createSetupIntent(): SetupIntent {
        val paramBuilder = SetupIntentCreateParams.Builder()
            .addPaymentMethodType("card_present")
        return SetupIntent.create(paramBuilder.build())
    }

    class ApiException(cause: Throwable) : Exception(cause)
}