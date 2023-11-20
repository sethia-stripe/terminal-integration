package com.example.terminalintegration.payments

import android.annotation.SuppressLint
import android.content.Context
import com.example.terminalintegration.BuildConfig
import com.example.terminalintegration.StorageManager
import com.example.terminalintegration.Utils
import com.example.terminalintegration.payments.model.Payment
import com.example.terminalintegration.payments.model.PaymentPath
import com.example.terminalintegration.payments.model.ReaderInfo
import com.example.terminalintegration.payments.model.TerminalReaderEvent
import com.example.terminalintegration.payments.network.TokenProvider
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class PaymentSDK(
    private val context: Context,
    private val tokenProvider: TokenProvider,
    private val storageManager: StorageManager,
    private val coroutineScope: CoroutineScope
) : TerminalListener {

    private var discoveryCancelable: Cancelable? = null

    private val _lastActiveReader = MutableStateFlow<ReaderInfo?>(null)
    val lastActiveReader: StateFlow<ReaderInfo?> = _lastActiveReader

    companion object {
        private const val KEY_READER_ID = "pk-reader-id"
        private const val KEY_READER_LABEL = "pk-reader-label"
    }

    init {
        updateReader(
            storageManager.getString(KEY_READER_ID),
            storageManager.getString(KEY_READER_LABEL)
        )
    }

    @Inject
    constructor(
        @ApplicationContext context: Context,
        tokenProvider: TokenProvider,
        storageManager: StorageManager
    ) : this(
        context,
        tokenProvider,
        storageManager,
        CoroutineScope(SupervisorJob())
    )

    private val readerEvents = MutableSharedFlow<TerminalReaderEvent>()

    fun initialize() {
        if (!Terminal.isInitialized()) {
            Timber.tag(Utils.LOGTAG).d("Initialize sdk - sdk")
            Terminal.initTerminal(context, LogLevel.VERBOSE, tokenProvider, this)
        }
    }

    override fun onUnexpectedReaderDisconnect(reader: Reader) {
        publishEvent(TerminalReaderEvent.ReaderDisconnected)
    }

    private fun publishEvent(event: TerminalReaderEvent) {
        coroutineScope.launch { readerEvents.emit(event) }
    }

    suspend fun subscribe(onEvent: (TerminalReaderEvent) -> Unit) {
        readerEvents.collectLatest { event ->
            coroutineContext.ensureActive()
            Timber.tag(Utils.LOGTAG).d("reader event flow - sdk : $event")
            onEvent(event)
        }
    }

    @SuppressLint("MissingPermission")
    fun discover(paymentPath: PaymentPath) {
        Timber.tag(Utils.LOGTAG).d("Discover reader flow - sdk")
        onStop()
        val config = DiscoveryConfiguration.InternetDiscoveryConfiguration(
            isSimulated = false,
            location = BuildConfig.STRIPE_LOCATION_ID
        )
        discoveryCancelable = Terminal.getInstance().discoverReaders(
            config,
            object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    Timber.tag(Utils.LOGTAG).d("Discover reader flow - sdk : discovered")
                    publishEvent(TerminalReaderEvent.ReadersDiscovered(readers))
                }
            },
            object : Callback {
                override fun onSuccess() {

                }

                override fun onFailure(e: TerminalException) {
                    if (e.errorCode != TerminalException.TerminalErrorCode.CANCELED) {
                        Timber.tag(Utils.LOGTAG).e(e)
                        publishEvent(TerminalReaderEvent.ReadersDiscoveryFailure(e))
                    }
                }
            }
        )
    }

    fun connectReader(reader: Reader) {
        Timber.tag(Utils.LOGTAG).d("connect reader flow - sdk")
        val config = ConnectionConfiguration.InternetConnectionConfiguration(failIfInUse = true)
        Terminal.getInstance().connectInternetReader(reader, config, object : ReaderCallback {
            override fun onSuccess(reader: Reader) {
                Timber.tag(Utils.LOGTAG).d("connect reader flow - sdk : connected")
                updateReader(reader.id, reader.label)
                saveReader(reader)
                publishEvent(TerminalReaderEvent.ReaderConnected(reader))
            }

            override fun onFailure(e: TerminalException) {
                e.printStackTrace()
                publishEvent(TerminalReaderEvent.ReaderDisconnected)
            }
        })
    }

    private fun saveReader(reader: Reader) {
        storageManager.putString(KEY_READER_ID, reader.id)
        storageManager.putString(KEY_READER_LABEL, reader.label)
    }

    private fun updateReader(id: String?, label: String?) {
        if (!id.isNullOrBlank() && !label.isNullOrBlank()) {
            _lastActiveReader.value = ReaderInfo(id, label)
        }
    }

    fun makePayment(payment: Payment) {
        //
        Timber.tag(Utils.LOGTAG).d("make payment flow - sdk")
    }

    fun onStop() {
        // If you're leaving the activity or fragment without selecting a reader,
        // make sure you cancel the discovery process or the SDK will be stuck in
        // a discover readers phase
        discoveryCancelable?.cancel(object : Callback {
            override fun onSuccess() {}
            override fun onFailure(e: TerminalException) {}
        })
    }
}