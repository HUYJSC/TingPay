package com.tinhocgenz.tingpay.ui.screens.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tinhocgenz.tingpay.core.common.toVndFormat
import com.tinhocgenz.tingpay.ui.components.FastPosKeypad
import com.tinhocgenz.tingpay.ui.components.VietQrCard
import com.tinhocgenz.tingpay.ui.theme.DarkBackground
import com.tinhocgenz.tingpay.ui.theme.PrimaryEmerald
import com.tinhocgenz.tingpay.ui.theme.SecondaryBlue

@Composable
fun CashierModeScreen(
    viewModel: CashierViewModel = viewModel(),
    onExitCashierMode: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var pinInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "CHẾ ĐỘ THU NGÂN POS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                IconButton(
                    onClick = {
                        if (uiState.isLocked) {
                            viewModel.showPinPrompt()
                        } else {
                            onExitCashierMode()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Exit / Unlock",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Middle: QR Display or Success Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val acc = uiState.defaultAccount
                val ord = uiState.currentOrder
                val amount = uiState.amountString.toLongOrNull() ?: 0L

                if (acc != null && ord != null && amount > 0) {
                    VietQrCard(
                        bin = acc.bin,
                        bankName = acc.bankName,
                        accountNumber = acc.accountNumber,
                        accountName = acc.accountName,
                        amount = amount,
                        orderCode = ord.orderCode
                    )
                } else if (uiState.lastSuccessAmount != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryEmerald.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ĐÃ NHẬN TIỀN THÀNH CÔNG",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryEmerald,
                                fontSize = 18.sp
                            )
                            Text(
                                text = uiState.lastSuccessAmount!!.toVndFormat(),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 32.sp
                            )
                        }
                    }
                } else if (acc != null) {
                    VietQrCard(
                        bin = acc.bin,
                        bankName = acc.bankName,
                        accountNumber = acc.accountNumber,
                        accountName = acc.accountName,
                        amount = 0,
                        orderCode = "POS"
                    )
                } else {
                    Text(
                        text = "Vui lòng thiết lập tài khoản nhận tiền trước",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Keypad for Quick Cashier Input
            FastPosKeypad(
                onDigitClick = { viewModel.onDigit(it) },
                onBackspaceClick = { viewModel.onBackspace() },
                onClearClick = { viewModel.onClear() },
                onSubmitClick = { /* Real-time QR updates immediately on typing */ }
            )
        }

        // PIN Dialog for exiting Cashier mode
        if (uiState.pinPromptVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.hidePinPrompt() },
                title = { Text("Nhập mã PIN để thoát chế độ thu ngân", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("Mã PIN (4 số)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (uiState.pinError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = uiState.pinError!!, color = Color.Red, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onPinEntered(pinInput) {
                                onExitCashierMode()
                            }
                        }
                    ) {
                        Text("XÁC NHẬN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hidePinPrompt() }) {
                        Text("HỦY")
                    }
                }
            )
        }
    }
}
