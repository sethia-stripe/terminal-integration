package com.example.terminalintegration.ui.components

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
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
import com.example.terminalintegration.Utils
import com.example.terminalintegration.payments.model.PaymentPath
import com.example.terminalintegration.payments.model.PaymentState
import com.example.terminalintegration.payments.model.ReaderInfo
import com.example.terminalintegration.ui.theme.TerminalIntegrationTheme
import com.stripe.stripeterminal.external.models.Reader
import timber.log.Timber


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cart(
    data: CartUIData,
    qty: Int,
    price: Int,
    maxQty: Int,
    onPayClicked: (Int, PaymentPath) -> Unit = { _, _ -> },
    onReaderDialogDismissed: () -> Unit = { },
    onReaderSelected: (Reader) -> Unit = { },
    onReaderRemoved: () -> Unit = { },
    modifier: Modifier = Modifier.padding(16.dp),
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedQty by remember { mutableStateOf(qty) }
    var total by remember { mutableStateOf(selectedQty * price) }
    var showPathDialog by remember { mutableStateOf(false) }
    var showDiscoveringDialog = data.paymentState is PaymentState.Discovering
    var showReaderDialog = data.paymentState is PaymentState.Discovered
    var showConnectingDialog = data.paymentState is PaymentState.Connecting
    var showPaymentInProcessDialog = data.paymentState is PaymentState.PaymentInitiated

    Timber.tag(Utils.LOGTAG)
        .d("UI update flow : payment state - ${data.paymentState::class.simpleName}")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text("Price per item: $${price}")
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
                repeat(maxQty) {
                    DropdownMenuItem(
                        text = { Text(text = it.plus(1).toString()) },
                        onClick = {
                            selectedQty = it.plus(1)
                            total = selectedQty * price
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text("Cart Total: $${total}", fontWeight = FontWeight(800))
        Spacer(modifier = Modifier.size(16.dp))
        Button(onClick = { showPathDialog = true }) {
            Text("Pay $${total}")
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
    }
    if (showPathDialog) {
        PaymentPathDialog() {
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
}

@Composable
fun PaymentPathDialog(onClick: (PaymentPath) -> Unit) {
    ListDialog(labels = PaymentPath.values().map { it.name }) {
        onClick(PaymentPath.values()[it])
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


@Preview(showBackground = true)
@Composable
fun CartPreview() {
    TerminalIntegrationTheme {
        Cart(CartUIData(PaymentState.Init, null), 1, 10, 5)
    }
}

data class CartUIData(val paymentState: PaymentState, val lastReader: ReaderInfo?)