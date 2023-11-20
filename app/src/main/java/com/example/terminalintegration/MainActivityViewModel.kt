package com.example.terminalintegration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.terminalintegration.payments.PaymentSDK
import com.example.terminalintegration.payments.model.Payment
import com.example.terminalintegration.payments.model.PaymentPath
import com.example.terminalintegration.payments.model.PaymentState
import com.example.terminalintegration.payments.model.TerminalReaderEvent
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
class MainActivityViewModel @Inject constructor(private val paymentSDK: PaymentSDK) : ViewModel() {

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Init)
    val paymentState: StateFlow<PaymentState> = _paymentState

    private val pastPayments = mutableListOf<Payment>()
    private val _payments = MutableStateFlow<List<Payment>>(pastPayments)
    val payments: StateFlow<List<Payment>> = _payments

    val lastActiveReader = paymentSDK.lastActiveReader

    private val _toastFlow = Channel<String>()
    val toastFlow: Flow<String> = _toastFlow.receiveAsFlow()

    private var payment: Payment? = null
    private var path: PaymentPath? = null

    fun onLocationPermissionGranted() {
        Timber.tag(Utils.LOGTAG).d("Initialize sdk - viewmodel")
        initializePaymentSdk()
    }

    private fun onReaderEvent(event: TerminalReaderEvent) {
        Timber.tag(Utils.LOGTAG).d("reader event flow - viewmodel : $event")
        when (event) {
            TerminalReaderEvent.ReaderDisconnected -> onReaderDisconnected()
            is TerminalReaderEvent.ReadersDiscovered -> onReadersDiscovered(event.list)
            is TerminalReaderEvent.ReaderConnected -> onReaderConnected(event.reader)
            is TerminalReaderEvent.ReadersDiscoveryFailure -> onDiscoveryError(event.e)
            is TerminalReaderEvent.PaymentError -> onPaymentError(event.error)
            is TerminalReaderEvent.PaymentSuccess -> onPaymentResponse(event.status)
        }
    }

    private fun onDiscoveryError(e: Throwable) {
        updateState(PaymentState.Init)
        sendToast(e.message.toString())
    }

    private fun onPaymentResponse(status: String) {
        sendToast("Payment successful")
        payment?.run {
            this.status = status
            updateState(PaymentState.PaymentSuccess(this))
            pastPayments.add(this)
            _payments.value = pastPayments
            resetPaymentVariables()
        }
    }

    private fun onPaymentError(error: Exception) {
        sendToast("Payment failed: $error")
        updateState(PaymentState.PaymentError(error.toString()))
    }

    private fun resetPaymentVariables() {
        payment = null
        path = null
    }

    private fun sendToast(msg: String) {
        viewModelScope.launch { _toastFlow.send(msg) }
    }

    private fun onReaderDisconnected() {
        updateState(PaymentState.Init)
        sendToast("Reader disconnected")
    }

    private fun discoverReaders() {
        path?.also {
            Timber.tag(Utils.LOGTAG).d("Discover reader flow - viewmodel")
            updateState(PaymentState.Discovering)
            paymentSDK.discover(it)
        }
    }

    private fun onReaderConnected(reader: Reader) {
        updateState(PaymentState.Connected(reader))
        makePayment()
    }

    private fun makePayment() {
        payment?.also {
            Timber.tag(Utils.LOGTAG).d("make payment flow - viewmodel")
            sendToast("Make Payment")
            updateState(PaymentState.PaymentInitiated(it))
            paymentSDK.makePayment(it)
        }
    }

    private fun onReadersDiscovered(list: List<Reader>) {
        if (list.isNotEmpty()) {
            list.find { it.id == lastActiveReader.value?.id }?.run {
                onReaderSelected(this)
            } ?: kotlin.run {
                updateState(PaymentState.Discovered(list))
            }
        } else {
            sendToast("No readers found")
            updateState(PaymentState.Init)
        }
    }

    fun onPayClicked(amount: Double, path: PaymentPath) {
        payment = Payment(amount, "cad")
        this.path = path
        if (paymentSDK.isConnected()) {
            makePayment()
        } else {
            discoverReaders()
        }
    }

    private fun updateState(state: PaymentState) {
        viewModelScope.launch { _paymentState.emit(state) }
        Timber.tag(Utils.LOGTAG).d("payment state flow - viewmodel : $state")
    }

    fun onDiscoveredDialogDismissed() {
        updateState(PaymentState.Init)
    }

    fun onReaderSelected(reader: Reader) {
        updateState(PaymentState.Connecting(reader))
        paymentSDK.connectReader(reader)
    }

    private fun initializePaymentSdk() {
        Timber.tag(Utils.LOGTAG).d("Initialize sdk - activity")
        paymentSDK.initialize()
        viewModelScope.launch {
            paymentSDK.subscribe { onReaderEvent(it) }
        }
    }

    fun onStop() {
        paymentSDK.onStop()
    }

    fun removeSavedReader() {
        paymentSDK.removeSavedReader()
    }
}