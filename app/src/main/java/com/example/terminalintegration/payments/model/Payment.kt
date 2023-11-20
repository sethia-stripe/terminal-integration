package com.example.terminalintegration.payments.model

data class Payment(val amount: Double, val currency: String, var status: String = "")
