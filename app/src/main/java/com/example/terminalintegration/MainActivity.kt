package com.example.terminalintegration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.terminalintegration.ui.theme.TerminalIntegrationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TerminalIntegrationTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Cart(1, 10, 5)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Cart(qty: Int, price: Int, maxQty: Int, modifier: Modifier = Modifier.padding(16.dp)) {
    var expanded by remember { mutableStateOf(false) }
    var selectedQty by remember { mutableStateOf(qty) }
    var total by remember { mutableStateOf(selectedQty * price) }

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
        Button(onClick = {
            pay(price)
        }) {
            Text("Pay $${total}")
        }
    }
}

fun pay(price: Int) {
    TODO("Not yet implemented")
}

@Preview(showBackground = true)
@Composable
fun CartPreview() {
    TerminalIntegrationTheme {
        Cart(1, 10, 5)
    }
}