package com.sethia.terminalintegration.payments.model

enum class PaymentResult(val addendum: Int) {
    SUCCESS(0),
    DECLINED(1),
    PIN_REQUIRED_OFFLINE(2),
    PIN_REQUIRED_ANY(3),
    DECLINED_GENERIC(5),
    DECLINED_INCORRECT_PIN(55),
    DECLINED_WITHDRAWAL_LIMIT(65),
    DECLINED_PIN_TRY_EXCEEDED(75),
}