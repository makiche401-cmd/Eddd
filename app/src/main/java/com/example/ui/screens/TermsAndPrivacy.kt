package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TermsAndConditionsDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .background(Color.Transparent),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            border = BorderStroke(1.dp, CosmicGreenBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(VibrantGreen.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Terms Icon",
                                tint = VibrantGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Terms of Service",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Last updated: June 2026",
                                color = MediumGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .background(Color(0xFF242F3D), shape = CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Dialog",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Acceptance of Terms",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "By installing, activating, or connecting SimGate Gateway (\"the App\"), you acknowledge that you have read, understood, and agreed to be bound by these corporate, regulatory, and technical Terms of Service. If you do not agree to these terms, you must not connect your credentials and should delete the App from your device immediately.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "2. Service Boundaries & Capabilities",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "The App functions as a direct SMS edge client and gateway tool. It allows cellular sub-carrier parameters to synchronize with cloud-to-local priority dispatch queues continuously in the background. The App does NOT provide SMS packages or wireless subscription credits; it strictly relays outgoing or incoming operations onto the physical SIM card installed inside the hosting mobile equipment.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "3. Carrier Usage & Cost Liability",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Because transmission utilizes cellular networks via the active physical SIM slot chosen, any SMS dispatched via SimGate is billed directly against your standard telecommunications agreement. The App is NOT responsible and carries zero liability for tariff rate shifts, overage fee collection, roaming rates, subscription suspensions, or monthly wireless invoices. You must ensure your mobile plan has appropriate rates before high-volume polling is triggered.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "4. Acceptable & Legal Use Rules",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You agree to utilize this cellular gateway strictly in compliance with all relevant international and localized telecommunications rules, mobile anti-spam laws (e.g., CAN-SPAM, TCPA, and GDPR regulations), and cellular carrier terms of usage. Sending massive unrequested commercials, automatic spam links, fraudulent bait, high-velocity bulk harassment, or malicious scripts is strictly forbidden. We reserve the absolute right to terminate individual sync linkages if abuse is detected.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "5. Background Services & Battery Impact",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This App runs persistent background daemons, foreground tasks, wakelocks, and constant internet polling to synchronize tasks with your external dashboard instantly. Deactivating resource management limitations or adjusting pool frequencies to minimal targets will impact hardware thermal status and increase device battery usage significantly. You acknowledge and accept this performance profile as a fundamental technological requirement of real-time SMS gateways.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "6. Limitation of Liability & Warranties",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "The software is provided \"AS IS\" and \"AS AVAILABLE\" without expression or implication of perfect operability. We do not guarantee uninterrupted, timely, and secure cellular message deliveries, especially during cases of signal dropouts, carrier gateway blocks, low SIM balances, internet link degradation, or localized power failures. In no event shall SimGate developers be liable for lost profits, data erasure, or incidental damages originating from gateway outages.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action buttons
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "I Understand & Accept", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .background(Color.Transparent),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            border = BorderStroke(1.dp, CosmicGreenBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(VibrantGreen.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacy Icon",
                                tint = VibrantGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Privacy Policy",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Last updated: June 2026",
                                color = MediumGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .background(Color(0xFF242F3D), shape = CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Dialog",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Our Committment to Privacy",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This Privacy Policy details how SimGate Gateway (\"We\", \"Our\", or \"Us\") manages, collects, structures, and protects credentials, SMS contents, and telemetry logs. We respect your digital privacy as our absolute technical priority. All local database structures are encrypted and processed matching international best practices.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "2. Data We Collect and Process",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "To offer robust SMS gateway features, SimGate retrieves specific system information, including:\n" +
                                "• Outgoing message requirements: Recipient digits, textual bodies, and routing token sequences.\n" +
                                "• Device telemetry: Active carrier names, radio signal types, cell metrics, battery efficiency classes, and active SIM slots.\n" +
                                "• Incoming messages: Text received on the underlying SIM which we map onwards to your verified API webhook, supporting automatic inbound system triggers.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "3. Where does your Data go?",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This App acts purely as a secure bridge. Any telemetry data, inbound SMS alerts, and transmission metrics are uploaded directly and secure-encrypted (using HTTPS SSL/TLS) into the unique Supabase Edge API or custom backend link configured upon pairing. We never transmit your logs, phone directories, location coordinates, or personal SMS contents to our own infrastructure. You keep 100% unilateral ownership and data stewardship.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "4. Local SQLite / Room Database Security",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "All historical dispatch statuses, outgoing fail counts, queued outbox payloads, and telemetry checkpoints reside locally in a secure Android Room SQLite structure. No remote diagnostic programs or third-party analytical SDKs are embedded inside this package. Furthermore, you can instantly clear all stored logs, outbox histories, and device parameters via the Settings screen with a single tap.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "5. Requested Permissions & Usage Purpose",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "To activate its fundamental functionality, the App requests permissions from the Android OS:\n" +
                                "• SEND_SMS & RECEIVE_SMS: Used to trigger physical transmission via mobile transceivers and to relay incoming replies to your central servers.\n" +
                                "• READ_PHONE_STATE: Used to detect installed carrier subscriptions, active slot numbers, and signal parameters.\n" +
                                "• POST_NOTIFICATIONS: Kept active to maintain a foreground notification thread ensuring the OS does not terminate the relay service randomly.\n" +
                                "• REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: Prevents doze state suspension to ensure messages arrive immediately.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "6. Security Countermeasures",
                        color = VibrantGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "We enforce several technical layers of protection. Device credentials (tokens) are stored in the private namespace using SharedPreferences protected under app package Isolation. In addition, transmission uses secure SSL/TLS standards to suppress man-in-the-middle sniffing.",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action buttons
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Accept & Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
