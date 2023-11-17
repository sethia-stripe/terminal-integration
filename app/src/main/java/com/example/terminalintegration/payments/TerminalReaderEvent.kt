package com.example.terminalintegration.payments

import com.stripe.stripeterminal.external.models.Reader

sealed class TerminalReaderEvent {
    object ReaderDisconnected : TerminalReaderEvent()
    data class ReadersDiscovered(val list: List<Reader>) : TerminalReaderEvent()
    data class ReaderConnected(val reader: Reader) : TerminalReaderEvent()
    data class PaymentResponse(val success: Boolean, val error: Any?) : TerminalReaderEvent()
}