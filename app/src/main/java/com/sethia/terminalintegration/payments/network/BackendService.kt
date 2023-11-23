package com.sethia.terminalintegration.payments.network

import com.sethia.terminalintegration.payments.model.ConnectionToken
import com.stripe.stripeterminal.external.models.PaymentIntent
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * The `BackendService` interface handles the two simple calls we need to make to our backend.
 */
interface BackendService {

    /**
     * Get a connection token string from the backend
     */
    @POST("/connection_token")
    fun getConnectionToken(): Call<ConnectionToken>

    @FormUrlEncoded
    @POST("/create_payment_intent")
    fun createPaymentIntent(
        @Field("amount") amount: Int,
        @Field("currency") currency: String,
        @Field(value = "payment_method_types[]") paymentMethods: List<String>? = null
    ): Call<PaymentIntent>

    /**
     * Capture a specific payment intent on our backend
     */
    @FormUrlEncoded
    @POST("/capture_payment_intent")
    fun capturePaymentIntent(@Field("payment_intent_id") id: String): Call<Void>
}