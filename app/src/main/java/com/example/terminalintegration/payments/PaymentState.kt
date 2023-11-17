package com.example.terminalintegration.payments

import com.stripe.stripeterminal.external.models.Reader

sealed class PaymentState {
    object Init: PaymentState()
    data class Discovered(val list: List<Reader>): PaymentState()

    data class Connected(val reader: Reader): PaymentState()

    data class PaymentInitiated(val payment: Payment): PaymentState()

    data class PaymentSuccess(val payment: Payment): PaymentState()

    data class PaymentError(val error: String): PaymentState()
}