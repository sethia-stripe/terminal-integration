package com.example.terminalintegration.payments.model

enum class PaymentResult(val addendum: Double) {
    SUCCESS(0.0),
    DECLINED(.01),
    PIN_REQUIRED_OFFLINE(.02),
    PIN_REQUIRED_ANY(.03),
    DECLINED_GENERIC(.05),
    DECLINED_INCORRECT_PIN(.55),
    DECLINED_WITHDRAWAL_LIMIT(.65),
    DECLINED_PIN_TRY_EXCEEDED(.75),
}