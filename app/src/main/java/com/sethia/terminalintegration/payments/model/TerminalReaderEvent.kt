package com.sethia.terminalintegration.payments.model

import com.stripe.stripeterminal.external.models.Reader

sealed class TerminalReaderEvent {
    object ReaderDisconnected : TerminalReaderEvent()
    data class ReadersDiscovered(val list: List<Reader>) : TerminalReaderEvent()

    data class ReadersDiscoveryFailure(val e: Throwable) : TerminalReaderEvent()

    data class ReaderConnected(val reader: Reader) : TerminalReaderEvent()
    data class PaymentSuccess(val status: String) : TerminalReaderEvent()

    data class PaymentError(val error: Exception) : TerminalReaderEvent()
}