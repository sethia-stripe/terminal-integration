package com.example.terminalintegration

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.terminalintegration.payments.PaymentSDK
import com.example.terminalintegration.ui.components.Cart
import com.example.terminalintegration.ui.theme.TerminalIntegrationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_CODE_LOCATION = 1
    }

    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var paymentSDK: PaymentSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val paymentState by viewModel.paymentState.collectAsStateWithLifecycle()
            TerminalIntegrationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Cart(paymentState, 1, 10, 5, { total, path ->
                        viewModel.onPayClicked(total, path)
                    }, {
                        viewModel.onDiscoveredDialogDismissed()
                    }, {
                        viewModel.onReaderSelected(it)
                    })
                }
            }
        }
        launchAndRepeatWithViewLifecycle(Lifecycle.State.RESUMED) {
            launch {
                viewModel.initializeTerminalFlow.collectLatest { initializePaymentSdk() }
            }
            launch {
                viewModel.discoverReaderFlow.collectLatest {
                    Timber.tag(Utils.LOGTAG).d("Discover reader flow - activity")
                    paymentSDK.discover(it)
                }
            }
            launch {
                viewModel.connectReaderFlow.collectLatest { paymentSDK.connectReader(it) }
            }
            launch {
                viewModel.makePaymentFlow.collectLatest {
                    Timber.tag(Utils.LOGTAG).d("make payment - activity")
                    paymentSDK.makePayment(it)
                    Toast.makeText(this@MainActivity, "Make payment", Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            // REQUEST_CODE_LOCATION should be defined on your app level
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_LOCATION)
        } else {
            viewModel.onLocationPermissionGranted()
        }
    }

    private fun initializePaymentSdk() {
        Timber.tag(Utils.LOGTAG).d("Initialize sdk - activity")
        paymentSDK.initialize()
        lifecycleScope.launch {
            paymentSDK.subscribe { viewModel.onReaderEvent(it) }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_LOCATION && grantResults.isNotEmpty()
            && grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
            throw RuntimeException("Location services are required in order to " + "connect to a reader.")
        } else {
            viewModel.onLocationPermissionGranted()
        }
    }

    override fun onStop() {
        super.onStop()
        paymentSDK.onStop()
    }
}

inline fun ComponentActivity.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
): Job {
    return lifecycleScope.launch {
        repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}