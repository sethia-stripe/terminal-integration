package com.example.terminalintegration.payments.model

import java.util.UUID

data class Payment(val amount: Double, val currency: String, var status: String = "") {
    val id = UUID.randomUUID()
}
