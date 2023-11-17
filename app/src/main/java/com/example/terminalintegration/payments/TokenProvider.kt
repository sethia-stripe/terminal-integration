package com.example.terminalintegration.payments

import com.example.terminalintegration.PaymentConfigProvider
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenProvider(
    private val configProvider: PaymentConfigProvider,
    private val coroutineScope: CoroutineScope
) : ConnectionTokenProvider {

    @Inject
    constructor(provider: PaymentConfigProvider) : this(provider, CoroutineScope(SupervisorJob()))

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        try {
            // Your backend should call /v1/terminal/connection_tokens and return the
            // JSON response from Stripe. When the request to your backend succeeds,
            // return the `secret` from the response to the SDK.
            coroutineScope.launch {
                delay(200)
                callback.onSuccess(configProvider.config.stripeApiKey)
            }
        } catch (e: Exception) {
            callback.onFailure(
                ConnectionTokenException("Failed to fetch connection token", e)
            )
        }
    }
}