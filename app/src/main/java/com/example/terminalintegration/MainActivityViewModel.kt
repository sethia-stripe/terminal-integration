package com.example.terminalintegration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.terminalintegration.payments.Payment
import com.example.terminalintegration.payments.PaymentPath
import com.example.terminalintegration.payments.PaymentState
import com.example.terminalintegration.payments.TerminalReaderEvent
import com.stripe.stripeterminal.external.models.Reader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {

    private val _initializeTerminalFlow = Channel<Unit>()
    val initializeTerminalFlow: Flow<Unit> = _initializeTerminalFlow.receiveAsFlow()

    private val _discoverReaderFlow = Channel<PaymentPath>()
    val discoverReaderFlow: Flow<PaymentPath> = _discoverReaderFlow.receiveAsFlow()

    private val _connectReaderFlow = Channel<Reader>()
    val connectReaderFlow: Flow<Reader> = _connectReaderFlow.receiveAsFlow()

    private val _makePaymentFlow = Channel<Payment>()
    val makePaymentFlow: Flow<Payment> = _makePaymentFlow.receiveAsFlow()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Init)
    val paymentState: StateFlow<PaymentState> = _paymentState

    private var payment: Payment? = null
    private var path: PaymentPath? = null

    fun onLocationPermissionGranted() {
        Timber.tag(Utils.LOGTAG).d("Initialize sdk - viewmodel")
        viewModelScope.launch { _initializeTerminalFlow.send(Unit) }
    }

    fun onReaderEvent(event: TerminalReaderEvent) {
        Timber.tag(Utils.LOGTAG).d("reader event flow - viewmodel : $event")
        when (event) {
            TerminalReaderEvent.ReaderDisconnected -> onReaderDisconnected()
            is TerminalReaderEvent.ReadersDiscovered -> onReadersDiscovered(event.list)
            is TerminalReaderEvent.ReaderConnected -> onReaderConnected(event.reader)
            is TerminalReaderEvent.PaymentResponse -> onPaymentResponse(event)
        }
    }

    private fun onPaymentResponse(response: TerminalReaderEvent.PaymentResponse) {
        if (response.success) {
            updateState(PaymentState.PaymentSuccess(payment!!))
        } else {
            updateState(PaymentState.PaymentError(response.error.toString()))
        }
    }

    private fun onReaderDisconnected() {
        discoverReaders()
    }

    private fun discoverReaders() {
        path?.also {
            Timber.tag(Utils.LOGTAG).d("Discover reader flow - viewmodel")
            viewModelScope.launch {
                _discoverReaderFlow.send(it)
            }
        }
    }

    private fun onReaderConnected(reader: Reader) {
        payment?.also {
            updateState(PaymentState.PaymentInitiated(it))
            Timber.tag(Utils.LOGTAG).d("make payment flow - viewmodel")
            viewModelScope.launch { _makePaymentFlow.send(it) }
        }
    }

    private fun onReadersDiscovered(list: List<Reader>) {
        updateState(PaymentState.Discovered(list))
    }

    fun onPayClicked(amount: Int, path: PaymentPath) {
        payment = Payment(amount)
        this.path = path
        discoverReaders()
    }

    private fun updateState(state: PaymentState) {
        viewModelScope.launch { _paymentState.emit(state) }
        Timber.tag(Utils.LOGTAG).d("payment state flow - viewmodel : $state")
    }

    fun onDiscoveredDialogDismissed() {
        updateState(PaymentState.Init)
    }

    fun onReaderSelected(reader: Reader) {
        viewModelScope.launch { _connectReaderFlow.send(reader) }
    }
}