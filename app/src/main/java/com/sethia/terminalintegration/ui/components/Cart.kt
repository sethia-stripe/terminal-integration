package com.sethia.terminalintegration.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sethia.terminalintegration.Utils
import com.sethia.terminalintegration.payments.model.Payment
import com.sethia.terminalintegration.payments.model.PaymentPath
import com.sethia.terminalintegration.payments.model.PaymentResult
import com.sethia.terminalintegration.payments.model.PaymentState
import com.sethia.terminalintegration.payments.model.ReaderInfo
import com.sethia.terminalintegration.ui.theme.TerminalIntegrationTheme
import com.stripe.stripeterminal.external.models.Reader
import timber.log.Timber


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cart(
    data: CartUIData,
    onPayClicked: (Long, PaymentPath) -> Unit = { _, _ -> },
    onReaderDialogDismissed: () -> Unit = { },
    onReaderSelected: (Reader) -> Unit = { },
    onReaderRemoved: () -> Unit = { },
    onPaymentErrorDismissed: () -> Unit = { },
    onPaymentRetryClicked: () -> Unit = { },
    modifier: Modifier = Modifier.padding(16.dp),
) {
    var expanded by remember { mutableStateOf(false) }
    var desiredResultExpanded by remember { mutableStateOf(false) }
    var selectedQty by remember { mutableStateOf(data.qty) }
    var desiredResult by remember { mutableStateOf(PaymentResult.SUCCESS) }
    var total by remember { mutableStateOf(((selectedQty * data.price) + desiredResult.addendum)) }
    var showPathDialog by remember { mutableStateOf(false) }
    var showDiscoveringDialog = data.paymentState is PaymentState.Discovering
    var showReaderDialog = data.paymentState is PaymentState.Discovered
    var showConnectingDialog = data.paymentState is PaymentState.Connecting
    var showPaymentInProcessDialog = data.paymentState is PaymentState.PaymentInitiated
    var showPaymentErrorDialog = data.paymentState is PaymentState.PaymentError

    Timber.tag(Utils.LOGTAG)
        .d("UI update flow : payment state - ${data.paymentState::class.simpleName}")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text("Price per item: $${data.price.toDisplayFormat()}")
        Spacer(modifier = Modifier.size(16.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {
            TextField(
                readOnly = true,
                value = selectedQty.toString(),
                onValueChange = { },
                label = { Text("Qty") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                repeat(data.maxQty) {
                    DropdownMenuItem(
                        text = { Text(text = it.plus(1).toString()) },
                        onClick = {
                            selectedQty = it.plus(1)
                            total = (selectedQty * data.price) + desiredResult.addendum
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        ExposedDropdownMenuBox(
            expanded = desiredResultExpanded,
            onExpandedChange = {
                desiredResultExpanded = !desiredResultExpanded
            }
        ) {
            TextField(
                readOnly = true,
                value = desiredResult.name,
                onValueChange = { },
                label = { Text("Desired Result") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = desiredResultExpanded
                    )
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = desiredResultExpanded,
                onDismissRequest = {
                    desiredResultExpanded = false
                }
            ) {
                PaymentResult.values().forEach {
                    DropdownMenuItem(
                        text = { Text(text = it.name) },
                        onClick = {
                            desiredResult = it
                            total = (selectedQty * data.price) + it.addendum
                            desiredResultExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text("Cart Total: $${total.toDisplayFormat()}", fontWeight = FontWeight(800))
        Spacer(modifier = Modifier.size(16.dp))
        Button(onClick = {
            if (data.paymentPaths.size > 1) {
                showPathDialog = true
            } else {
                onPayClicked(total, data.paymentPaths.first())
            }
        }) {
            Text("Pay $${total.toDisplayFormat()}")
        }

        if (data.lastReader != null) {
            Spacer(modifier = Modifier.size(16.dp))
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.LightGray)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Last Reader: ${data.lastReader.label}")
                Spacer(modifier = Modifier.size(16.dp))
                Button(onClick = { onReaderRemoved() }) {
                    Text("Remove")
                }
            }
        }

        if (data.payments.isNotEmpty()) {
            data.payments.forEach {
                Spacer(modifier = Modifier.size(16.dp))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.LightGray)
                )
                Text(
                    "Amount: $${it.amount.toDisplayFormat()}, status : ${it.status}",
                    modifier.padding(16.dp)
                )
            }
        }
    }
    if (showPathDialog) {
        PaymentPathDialog(data.paymentPaths) {
            showPathDialog = false
            onPayClicked(total, it)
        }
    }
    if (showDiscoveringDialog) {
        ProgressDialog(message = "Discovering...") {
            showDiscoveringDialog = false
        }
    }
    if (showReaderDialog) {
        ReaderDialog(
            readers = (data.paymentState as PaymentState.Discovered).list,
            onDismiss = {
                onReaderDialogDismissed()
                showReaderDialog = false
            },
            onClick = { onReaderSelected(it) }
        )
    }
    if (showConnectingDialog) {
        ProgressDialog(message = "Connecting...") {
            showConnectingDialog = false
        }
    }
    if (showPaymentInProcessDialog) {
        ProgressDialog(message = "Processing...") {
            showPaymentInProcessDialog = false
        }
    }
    if (showPaymentErrorDialog) {
        AlertDialog(
            modifier = Modifier.wrapContentHeight(),
            title = {
                Text(text = "Payment failed")
            },
            text = {
                Text(text = (data.paymentState as PaymentState.PaymentError).error)
            },
            onDismissRequest = {
                onPaymentErrorDismissed()
                showPaymentErrorDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPaymentRetryClicked()
                        showPaymentErrorDialog = false
                    }
                ) {
                    Text("Retry")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onPaymentErrorDismissed()
                        showPaymentErrorDialog = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun PaymentPathDialog(list: List<PaymentPath>, onClick: (PaymentPath) -> Unit) {
    ListDialog(labels = list.map { it.name }) {
        onClick(list[it])
    }
}

@Composable
fun ReaderDialog(readers: List<Reader>, onDismiss: () -> Unit, onClick: (Reader) -> Unit) {
    ListDialog(labels = readers.map {
        "${it.deviceType.deviceName} - ${it.label}"
    }, {
        onDismiss()
    }) {
        onClick(readers[it])
    }
}

@Composable
fun ListDialog(labels: List<String>, onDismiss: () -> Unit = { }, onClick: (Int) -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "Select path",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center,
            )
            labels.forEachIndexed { index, label ->
                Button(
                    onClick = {
                        onClick(index)
                    }, modifier = Modifier
                        .padding(16.dp, 0.dp, 16.dp, 4.dp)
                        .fillMaxWidth()
                ) {
                    Text(label)
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun ProgressDialog(message: String, cancellable: Boolean = false, onDismiss: () -> Unit = { }) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(cancellable, cancellable)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            CircularProgressIndicator(
                Modifier
                    .padding(16.dp)
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

private fun Long.toDisplayFormat() = this.toDouble().div(100)


@Preview(showBackground = true)
@Composable
fun CartPreview() {
    TerminalIntegrationTheme {
        Cart(CartUIData(1, 1000L, 5))
    }
}

data class CartUIData(
    val qty: Int,
    val price: Long,
    val maxQty: Int,
    val paymentPaths: List<PaymentPath> = PaymentPath.values().toList(),
    val paymentState: PaymentState = PaymentState.Init,
    val lastReader: ReaderInfo? = null,
    val payments: List<Payment> = emptyList(),
)