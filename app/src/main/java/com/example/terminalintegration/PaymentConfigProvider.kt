package com.example.terminalintegration

import com.example.terminalintegration.payments.PaymentConfig
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PaymentConfigProvider @Inject constructor() {
    val config = PaymentConfig(
        "",
        ""
    )
}