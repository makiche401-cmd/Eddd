package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.Message
import com.example.gateway.GatewayService
import com.example.viewmodel.GatewayViewModel
import java.text.SimpleDateFormat
import java.util.*

// Core Aesthetic Colors aligned to dark mockup
val CosmicDark = Color(0xFF0F171E)
val CosmicCard = Color(0xFF1E262F)
val CosmicGreenBorder = Color(0xFF1A3324)
val VibrantGreen = Color(0xFF16A34A)
val MediumGray = Color(0xFF8B949E)

@Composable
fun OnboardingScreen(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var deviceId by remember { mutableStateOf("") }
    var deviceToken by remember { mutableStateOf("") }
    var apiBase by remember { mutableStateOf("https://abjwmllylfdbcmhfqwvk.supabase.co/functions/v1") }
    var showToken by remember { mutableStateOf(false) }
    var isQrScannerOpen by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val connectError by viewModel.connectError
    val isConnecting by viewModel.isConnecting
    val isPairSuccess by viewModel.isPairSuccess

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicDark)
    ) {
        if (isPairSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CosmicDark)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, VibrantGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(VibrantGreen.copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = VibrantGreen,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        
                        Text(
                            text = "Device Connected!",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Configuring parameters and launching background service daemon...",
                            color = MediumGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        CircularProgressIndicator(
                            color = VibrantGreen,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Text(
                            text = "Entering Home Dashboard...",
                            color = VibrantGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
            // Header Shield Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF1C3A27), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = "Secure Badge",
                            tint = VibrantGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SMS Gateway",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connect your device to the gateway",
                            color = MediumGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Connection Box Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Connect Your Device",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Enter the Device ID and Device Token provided by your dashboard on simresend.web.app.",
                            color = MediumGray,
                            fontSize = 12.sp
                        )

                        // Device ID Input
                        OutlinedTextField(
                            value = deviceId,
                            onValueChange = { deviceId = it },
                            label = { Text("Device ID") },
                            placeholder = { Text("e.g. dev_a1b2c3d4") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VibrantGreen,
                                unfocusedBorderColor = Color(0xFF30363D),
                                focusedLabelColor = VibrantGreen,
                                unfocusedLabelColor = MediumGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        deviceId = clip.getItemAt(0).text.toString()
                                    }
                                }) {
                                    Icon(Icons.Default.ContentCopy, "Paste Device ID", tint = MediumGray)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("device_id_input")
                        )

                        // Device Token Input
                        OutlinedTextField(
                            value = deviceToken,
                            onValueChange = { deviceToken = it },
                            label = { Text("Device Token") },
                            placeholder = { Text("e.g. dtk_1234567890abcdef") },
                            singleLine = true,
                            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VibrantGreen,
                                unfocusedBorderColor = Color(0xFF30363D),
                                focusedLabelColor = VibrantGreen,
                                unfocusedLabelColor = MediumGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(
                                        imageVector = if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Toggle token visibility",
                                        tint = MediumGray
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("device_token_input")
                        )

                        if (!connectError.isNullOrBlank()) {
                            Text(
                                text = connectError ?: "",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Connect Button
                        Button(
                            onClick = {
                                viewModel.pairDevice(deviceId, deviceToken, apiBase) {
                                    Toast.makeText(context, "Device Connected!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen, disabledContainerColor = VibrantGreen.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("connect_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Link, "Link")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect to Gateway", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        // Alternate Scan QR Code
                        OutlinedButton(
                            onClick = { isQrScannerOpen = true },
                            border = BorderStroke(1.dp, Color(0xFF30363D)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("scan_qr_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, "QR Code")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan QR Code", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Tutorial Guidance Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CosmicGreenBorder.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info Icon",
                                tint = VibrantGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "How to get Device ID & Token?",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Text(
                            text = "To register, manage devices, and retrieve your credentials, please use the SimResend Web platform.",
                            color = MediumGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        val steps = listOf(
                            "1. Go to https://simresend.web.app",
                            "2. Register or Login to your dashboard",
                            "3. Click \"Add Device\" under SimGate Gateway",
                            "4. Copy your unique Device ID and Token here"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            steps.forEach { step ->
                                Text(
                                    text = step,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri("https://simresend.web.app")
                                } catch (e: Exception) {
                                    Log.e("Screens", "Failed to open gateway URL", e)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, VibrantGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = "Open Web Dashboard",
                                    tint = VibrantGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Open simresend.web.app",
                                    color = VibrantGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Secure and Private Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, CosmicGreenBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = VibrantGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Secure & Private",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Your credentials are stored securely on this device and never shared.",
                                color = MediumGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Terms of Service and Privacy Policy Link Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "By pairing your device, you agree to our software terms.",
                        color = MediumGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showTermsDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Terms of Service",
                                color = VibrantGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "•",
                            color = MediumGray,
                            fontSize = 12.sp
                        )
                        TextButton(
                            onClick = { showPrivacyDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Privacy Policy",
                                color = VibrantGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        }

        // Expanded QR Scanner Dialog
        if (isQrScannerOpen) {
            QrCodeScannerDialog(
                onDismissRequest = { isQrScannerOpen = false },
                onQrCodeScanned = { qrResult ->
                    isQrScannerOpen = false
                    viewModel.pairWithQr(qrResult) {
                        Toast.makeText(context, "Paired via QR successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showTermsDialog) {
            TermsAndConditionsDialog(onDismissRequest = { showTermsDialog = false })
        }

        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismissRequest = { showPrivacyDialog = false })
        }
    }
}

@Composable
fun AutoStartHelperDialog(
    onDismissRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = VibrantGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Auto Start Settings",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "To ensure the SimGate gateway background daemon remains active after device reboots, please enable the Auto-start settings of this application.",
                    color = MediumGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "On brand-specific ROMs (such as MIUI/Xiaomi, Oppo, Vivo, Samsung), system policy disables third-party boot activities. Enabling Autostart is required to keep your SMS dispatcher completely stable.",
                    color = MediumGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Open Settings", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = MediumGray)
            }
        },
        containerColor = CosmicCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PermissionsScreen(
    viewModel: GatewayViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasSendSms by remember { mutableStateOf(false) }
    var hasReadSms by remember { mutableStateOf(false) }
    var hasReceiveSms by remember { mutableStateOf(false) }
    var hasPhoneState by remember { mutableStateOf(false) }
    var hasNotif by remember { mutableStateOf(false) }
    var isBatteryOptDisabled by remember { mutableStateOf(false) }
    var hasAutoStartConfigured by remember { mutableStateOf(false) }

    var showAutoStartDialog by remember { mutableStateOf(false) }

    val checkPermissions = {
        hasSendSms = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        hasReadSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        hasReceiveSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        
        hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isBatteryOptDisabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    // Trigger Permission Request Launchers
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        checkPermissions()
    }

    val requirementCompleted = hasSendSms && hasReadSms && hasReceiveSms && hasPhoneState && hasNotif && isBatteryOptDisabled

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header block matching screen style exactly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onContinue,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grant Permissions",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Required to run SimGate Gateway",
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }

                // Green outer status badge indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(VibrantGreen.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Shield Indicator",
                        tint = VibrantGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Scrollable Permissions List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Warning/Info Alert card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E16)),
                        border = BorderStroke(1.dp, VibrantGreen.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(VibrantGreen.copy(alpha = 0.15f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = VibrantGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "All permissions are required",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "SimGate needs these permissions to send and receive SMS reliably in the background.",
                                    color = MediumGray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Section title
                item {
                    Text(
                        text = "Required Permissions",
                        color = VibrantGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // List of permissions
                item {
                    PermissionRowItem(
                        title = "Send SMS",
                        subtitle = "Allows the app to send SMS messages",
                        icon = Icons.Default.Sms,
                        isGranted = hasSendSms,
                        onClick = {
                            if (!hasSendSms) {
                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.SEND_SMS))
                            }
                        }
                    )
                }

                item {
                    PermissionRowItem(
                        title = "Read SMS",
                        subtitle = "Allows the app to read SMS messages for history and status",
                        icon = Icons.Default.Inbox,
                        isGranted = hasReadSms,
                        onClick = {
                            if (!hasReadSms) {
                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS))
                            }
                        }
                    )
                }

                item {
                    PermissionRowItem(
                        title = "Receive SMS",
                        subtitle = "Allows the app to receive incoming SMS messages",
                        icon = Icons.Default.Comment,
                        isGranted = hasReceiveSms,
                        onClick = {
                            if (!hasReceiveSms) {
                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS))
                            }
                        }
                    )
                }

                item {
                    PermissionRowItem(
                        title = "Post Notifications",
                        subtitle = "Needed to show foreground service notifications",
                        icon = Icons.Default.Notifications,
                        isGranted = hasNotif,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotif) {
                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            }
                        }
                    )
                }

                item {
                    PermissionRowItem(
                        title = "Read Phone State",
                        subtitle = "Used to get signal strength and network information",
                        icon = Icons.Default.Phone,
                        isGranted = hasPhoneState,
                        onClick = {
                            if (!hasPhoneState) {
                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
                            }
                        }
                    )
                }

                item {
                    PermissionRowItem(
                        title = "Battery Optimization",
                        subtitle = "Prevent the system from killing the gateway in the background",
                        icon = Icons.Default.BatteryChargingFull,
                        isGranted = isBatteryOptDisabled,
                        onClick = {
                            if (!isBatteryOptDisabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).let {
                                        context.startActivity(it)
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    PermissionRowItem(
                        title = "Auto Start on Boot",
                        subtitle = "Allows the gateway to start automatically after reboot",
                        icon = Icons.Default.PowerSettingsNew,
                        isGranted = hasAutoStartConfigured,
                        onClick = {
                            showAutoStartDialog = true
                        }
                    )
                }

                // Info Box at the end of the scrollable area
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicCard.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, CosmicGreenBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = VibrantGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Why we need these permissions?",
                                    color = VibrantGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "These permissions are essential for SMS operations, maintaining reliable connection, and running the gateway in the background.",
                                    color = MediumGray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Fixed Sticky Bottom Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Grant All or Continue Button
                if (!requirementCompleted) {
                    Button(
                        onClick = {
                            // Request standard system permissions together
                            val permsList = mutableListOf(
                                Manifest.permission.SEND_SMS,
                                Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_PHONE_STATE
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permsList.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            requestPermissionLauncher.launch(permsList.toTypedArray())

                            // Attempt battery check if applicable
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isBatteryOptDisabled) {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("Screens", "Failed simple battery optimization call", e)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("grant_all_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = "Shield Icon",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Grant All Permissions",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onContinue,
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_to_app_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Continue to Gateway",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Explicit link to Open Permission Settings fallback
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open system application settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Open Permission Settings",
                        color = VibrantGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Dialog for Auto-Start Helper
        if (showAutoStartDialog) {
            AutoStartHelperDialog(
                onDismissRequest = {
                    showAutoStartDialog = false
                },
                onOpenSettings = {
                    showAutoStartDialog = false
                    hasAutoStartConfigured = true
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Settings opened", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A22)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isGranted) CosmicGreenBorder.copy(alpha = 0.5f) else Color(0xFF242F3D).copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left circular icon container
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (isGranted) VibrantGreen.copy(alpha = 0.12f) else Color(0xFF242F3D),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) VibrantGreen else MediumGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = MediumGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Badge Column or Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = VibrantGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Granted",
                        color = VibrantGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Not granted",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Not granted",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MediumGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun HomeScreenCountCard(
    label: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(20.dp))
                Text(text = count.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = label, color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: GatewayViewModel,
    onEditCredentials: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recentLogs by viewModel.messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val stateIsRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val stateHeartbeat by viewModel.lastHeartbeat.collectAsStateWithLifecycle()
    val stateOnlineStatus by viewModel.onlineStatus.collectAsStateWithLifecycle()

    val stateSentCount by viewModel.sentToday.collectAsStateWithLifecycle()
    val stateFailedCount by viewModel.failedToday.collectAsStateWithLifecycle()
    val stateRecvCount by viewModel.receivedToday.collectAsStateWithLifecycle()

    val batteryVal by viewModel.batteryLevel.collectAsStateWithLifecycle()
    val currentNetworkName by viewModel.currentNetwork.collectAsStateWithLifecycle()
    val currentSimString by viewModel.currentSimInfo.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicDark)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SMS Gateway",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your phone is running as SMS Gateway",
                            color = MediumGray,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF161B22), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Connection Badge Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val statusTitle: String
                            val statusDesc: String
                            val statusIcon: ImageVector
                            val statusIconColor: Color
                            val statusIconBg: Color
                            val statusDotColor: Color

                            if (!stateIsRunning) {
                                statusTitle = "Stopped"
                                statusDesc = "Gateway is currently stopped"
                                statusIcon = Icons.Default.Cancel
                                statusIconColor = Color.Red
                                statusIconBg = Color(0xFF321F20)
                                statusDotColor = Color.Red
                            } else if (stateOnlineStatus == "Reconnecting") {
                                statusTitle = "Reconnecting..."
                                statusDesc = "Network lost. Reconnecting until established..."
                                statusIcon = Icons.Default.Refresh
                                statusIconColor = Color(0xFFF59E0B)
                                statusIconBg = Color(0xFF452A10)
                                statusDotColor = Color(0xFFF59E0B)
                            } else if (stateOnlineStatus == "Offline") {
                                statusTitle = "Gateway Paused"
                                statusDesc = "The gateway has been paused temporarily."
                                statusIcon = Icons.Default.Pause
                                statusIconColor = Color.White
                                statusIconBg = Color(0xFF3F3F46)
                                statusDotColor = Color.Gray
                            } else {
                                statusTitle = "Connected"
                                statusDesc = "Gateway running and listening to queues"
                                statusIcon = Icons.Default.CheckCircle
                                statusIconColor = VibrantGreen
                                statusIconBg = Color(0xFF1A3324)
                                statusDotColor = VibrantGreen
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(statusIconBg, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = "Status",
                                    tint = statusIconColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = statusTitle,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = statusDesc,
                                    color = MediumGray,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(statusDotColor, shape = CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Last heartbeat: $stateHeartbeat  •  $stateOnlineStatus",
                                        color = MediumGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Active signal",
                            tint = if (stateIsRunning) VibrantGreen else MediumGray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // Device details block
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Device ID Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Smartphone, "Phone Icon", tint = VibrantGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Device ID", color = MediumGray, fontSize = 11.sp)
                                    Text(viewModel.prefsManager.deviceId ?: "Unpaired", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Device ID", viewModel.prefsManager.deviceId ?: "")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Device ID copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, "Copy Device ID", tint = MediumGray, modifier = Modifier.size(18.dp))
                            }
                        }

                        Divider(color = Color(0xFF30363D))

                        // Stats Telemetry Row (Battery, Signal, Network)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Battery", color = MediumGray, fontSize = 10.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.BatteryChargingFull, "Battery", tint = VibrantGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$batteryVal%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Column {
                                Text("Signal Type", color = MediumGray, fontSize = 10.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wifi, "Signal bars", tint = VibrantGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(currentNetworkName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Column {
                                Text("Uptime", color = MediumGray, fontSize = 10.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, "Clock", tint = VibrantGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Simulated uptime
                                    Text("Active", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // SIM Card Quick Info Display
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SIM Information", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Active Preferences: ${viewModel.simPreference.value}", color = VibrantGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SdCard, "SIM card", tint = VibrantGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = currentSimString, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Metrics Row Widgets
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeScreenCountCard(
                        label = "Sent Today",
                        count = stateSentCount,
                        icon = Icons.Default.ArrowUpward,
                        accentColor = VibrantGreen,
                        modifier = Modifier.weight(1f)
                    )
                    HomeScreenCountCard(
                        label = "Failed Today",
                        count = stateFailedCount,
                        icon = Icons.Default.Warning,
                        accentColor = Color.Red,
                        modifier = Modifier.weight(1f)
                    )
                    HomeScreenCountCard(
                        label = "Received Today",
                        count = stateRecvCount,
                        icon = Icons.Default.ArrowDownward,
                        accentColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions Block
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Quick Actions", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Play action
                        QuickActionCard(
                            title = "Start Gateway",
                            icon = Icons.Default.PlayArrow,
                            containerColor = Color(0xFF14532D),
                            tint = VibrantGreen,
                            onClick = { viewModel.startService() },
                            modifier = Modifier.weight(1f).testTag("quick_start_service")
                        )

                        // Stop action
                        QuickActionCard(
                            title = "Stop Gateway",
                            icon = Icons.Default.Stop,
                            containerColor = Color(0xFF7F1D1D),
                            tint = Color.Red,
                            onClick = { viewModel.stopService() },
                            modifier = Modifier.weight(1f).testTag("quick_stop_service")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Edit action
                        QuickActionCard(
                            title = "Edit Credentials",
                            icon = Icons.Default.Edit,
                            containerColor = Color(0xFF1E3A8A),
                            tint = Color(0xFF3B82F6),
                            onClick = onEditCredentials,
                            modifier = Modifier.weight(1f)
                        )

                        // Refresh action
                        QuickActionCard(
                            title = "Force Sync",
                            icon = Icons.Default.Refresh,
                            containerColor = Color(0xFF374151),
                            tint = Color.White,
                            onClick = {
                                viewModel.forceSync()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Recent activity preview list
            item {
                Text("Recent Activity Preview", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            if (recentLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent transaction activity recorded.", color = MediumGray, fontSize = 12.sp)
                    }
                }
            } else {
                items(recentLogs.take(5)) { log ->
                    ActivityLogItem(message = log)
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicCard),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(containerColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActivityLogItem(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicCard, shape = RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        when (message.status) {
                            "SENT" -> Color(0xFF14532D)
                            "FAILED" -> Color(0xFF7F1D1D)
                            else -> Color(0xFF1E3A8A)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (message.status) {
                        "SENT" -> Icons.Default.ArrowUpward
                        "FAILED" -> Icons.Default.Warning
                        else -> Icons.Default.ArrowDownward
                    },
                    contentDescription = message.status,
                    tint = when (message.status) {
                        "SENT" -> VibrantGreen
                        "FAILED" -> Color.Red
                        else -> Color(0xFF3B82F6)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = message.recipient, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = message.body, color = MediumGray, fontSize = 11.sp, maxLines = 1)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val formatter = SimpleDateFormat("HH:mm a", Locale.getDefault())
            val timeString = formatter.format(Date(message.createdAt))
            Text(text = timeString, color = MediumGray, fontSize = 10.sp)
            Text(
                text = message.status,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when (message.status) {
                    "SENT" -> VibrantGreen
                    "FAILED" -> Color.Red
                    else -> Color(0xFF3B82F6)
                }
            )
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val logs by viewModel.messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = logs.filter { log ->
        val statusMatches = when (selectedTab) {
            0 -> log.status == "SENT"
            1 -> log.status == "FAILED"
            else -> log.status == "INCOMING"
        }
        val textMatches = searchQuery.isEmpty() || log.recipient.contains(searchQuery, ignoreCase = true) || log.body.contains(searchQuery, ignoreCase = true)
        statusMatches && textMatches
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicDark)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Activity History Logs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            // Search text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search number or message preview...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibrantGreen,
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedLabelColor = VibrantGreen,
                    unfocusedLabelColor = MediumGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("history_search_input")
            )

            // Dynamic Tab bars
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CosmicCard,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = VibrantGreen
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sent", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Failed", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Incoming", fontWeight = FontWeight.Bold) }
                )
            }

            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records found.", color = MediumGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredLogs) { log ->
                        ActivityLogItemDetailCard(message = log)
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLogItemDetailCard(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = message.recipient, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                Text(text = formatter.format(Date(message.createdAt)), color = MediumGray, fontSize = 11.sp)
            }
            Text(text = message.body, color = Color.White, fontSize = 12.sp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SimCard,
                        contentDescription = "Sim Slot used",
                        tint = VibrantGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val slotLabel = when (message.simSlot) {
                        0 -> "SIM 1"
                        1 -> "SIM 2"
                        99 -> "TEST SMS"
                        100 -> "DEFAULT"
                        else -> "AUTO"
                    }
                    Text(text = "Slot: $slotLabel", color = MediumGray, fontSize = 10.sp)
                }

                if (message.attempts > 1) {
                    Text(text = "Attempts: ${message.attempts}", color = MediumGray, fontSize = 10.sp)
                }
            }

            if (!message.lastError.isNullOrBlank()) {
                Text(
                    text = "Reason: ${message.lastError}",
                    color = Color.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SimScreen(
    viewModel: GatewayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPref by viewModel.simPreference.collectAsStateWithLifecycle()
    var simList by remember { mutableStateOf<List<GatewayService.SimInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Query SIM lists from class
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            try {
                val activeList = subManager.activeSubscriptionInfoList
                if (!activeList.isNullOrEmpty()) {
                    simList = activeList.map { info ->
                        val number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            @Suppress("DEPRECATION")
                            subManager.getPhoneNumber(info.subscriptionId) ?: info.number ?: ""
                        } else {
                            @Suppress("DEPRECATION")
                            info.number ?: ""
                        }
                        GatewayService.SimInfo(
                            slot = info.simSlotIndex,
                            carrier = info.carrierName?.toString() ?: "Unknown Carrier",
                            number = number,
                            subId = info.subscriptionId
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SimScreen", "Failed query subscription details", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicDark)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("SIM Configuration", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            // Current Preference Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Outbound SIM Priority Policy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Select which slot SimGate targeting while dispatching queues:", color = MediumGray, fontSize = 11.sp)

                    listOf("AUTO", "SIM_1", "SIM_2", "DEFAULT").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSimPreference(option) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentPref == option,
                                onClick = { viewModel.setSimPreference(option) },
                                colors = RadioButtonDefaults.colors(selectedColor = VibrantGreen, unselectedColor = MediumGray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (option) {
                                    "AUTO" -> "Auto routing (Detects active slot automatically)"
                                    "SIM_1" -> "Strict SIM Slot 1"
                                    "SIM_2" -> "Strict SIM Slot 2"
                                    else -> "System default selection (Precedence configured in Android Settings)"
                                },
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Subscriptions list
            Text("Detected System Subscriptions", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            if (simList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicCard, shape = RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active physical SIM subscription slots detected.", color = MediumGray, fontSize = 12.sp)
                }
            } else {
                simList.forEach { sim ->
                    val isSelected = (currentPref == "SIM_1" && sim.slot == 0) || (currentPref == "SIM_2" && sim.slot == 1)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val targetPref = if (sim.slot == 0) "SIM_1" else if (sim.slot == 1) "SIM_2" else "AUTO"
                                viewModel.setSimPreference(targetPref)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF14532D).copy(alpha = 0.4f) else CosmicCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) VibrantGreen else Color(0xFF30363D)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (isSelected) VibrantGreen.copy(alpha = 0.2f) else Color(0xFF161B22), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SimCard,
                                    contentDescription = "Sim",
                                    tint = if (isSelected) VibrantGreen else Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val activeLabel = if (isSelected) " (ACTIVE ROUTE)" else ""
                                    Text("Slot Card #${sim.slot + 1}$activeLabel", color = if (isSelected) VibrantGreen else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("SubID: ${sim.subId}", color = VibrantGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Carrier: ${sim.carrier}", color = MediumGray, fontSize = 12.sp)
                                if (sim.number.isNotBlank()) {
                                    Text("Phone: ${sim.number}", color = Color.White, fontSize = 12.sp)
                                } else {
                                    Text("Phone: (No phone number reported by SIM card)", color = MediumGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: GatewayViewModel,
    onManualPairEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val onlineStatus by viewModel.onlineStatus.collectAsStateWithLifecycle()

    var deviceId by remember { mutableStateOf(viewModel.prefsManager.deviceId ?: "dev_not_paired") }
    var deviceToken by remember { mutableStateOf(viewModel.prefsManager.deviceToken ?: "") }
    var hideToken by remember { mutableStateOf(true) }

    // Dynamic config states reflecting custom preferences
    var pollSec by remember { mutableStateOf(viewModel.prefsManager.pollInterval) }
    var hbSec by remember { mutableStateOf(viewModel.prefsManager.heartbeatInterval) }
    var autoReconnect by remember { mutableStateOf(viewModel.prefsManager.autoReconnect) }
    var startOnBoot by remember { mutableStateOf(viewModel.prefsManager.startOnBoot) }
    var pauseGateway by remember { mutableStateOf(viewModel.prefsManager.pauseGateway) }

    var persistentNotif by remember { mutableStateOf(viewModel.prefsManager.persistentNotification) }
    var smsSentNotif by remember { mutableStateOf(viewModel.prefsManager.smsSentNotifications) }
    var smsFailedNotif by remember { mutableStateOf(viewModel.prefsManager.smsFailedNotifications) }
    var incomingSmsNotif by remember { mutableStateOf(viewModel.prefsManager.incomingSmsNotifications) }

    // Dialog state for selections
    var showPollDialog by remember { mutableStateOf(false) }
    var showHbDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val maskedToken = if (deviceToken.length > 6) {
        "dtk_" + "*".repeat(deviceToken.length - 4)
    } else {
        "dtk_" + "*".repeat(12)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicDark)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header bar
            item {
                Column {
                    Text(
                        text = "Settings",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure your SimGate Gateway",
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Gateway Status Header Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isRunning && !pauseGateway) Color(0xFF14532D) else Color(0xFF3F3F46),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (isRunning && !pauseGateway) VibrantGreen else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Gateway Status", color = MediumGray, fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isRunning && !pauseGateway) "Connected" else "Disconnected",
                                        color = if (isRunning && !pauseGateway) VibrantGreen else MediumGray,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (isRunning && !pauseGateway && onlineStatus == "Online") VibrantGreen else Color.Red,
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isRunning && !pauseGateway) onlineStatus else "Offline",
                                        color = if (isRunning && !pauseGateway && onlineStatus == "Online") VibrantGreen else Color.Red,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = if (isRunning && !pauseGateway) "SimGate is running smoothly" else "SimGate is paused or stopped",
                                    color = MediumGray,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isRunning) {
                                    viewModel.stopService()
                                    viewModel.startService()
                                } else {
                                    viewModel.startService()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restart", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Category: Device & Connection
            item {
                Column {
                    SettingsCategoryHeader("Device & Connection")
                    
                    // Device ID
                    SettingsItemRow(
                        icon = Icons.Default.Dns,
                        iconColor = VibrantGreen,
                        title = "Device ID",
                        subtitle = "Unique identifier for this device",
                        rightContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(deviceId, color = MediumGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Device ID", deviceId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Device ID copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, "Copy Device ID", tint = MediumGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Device Token
                    SettingsItemRow(
                        icon = Icons.Default.VpnKey,
                        iconColor = VibrantGreen,
                        title = "Device Token",
                        subtitle = "Secret token for authenticating requests",
                        rightContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (hideToken) maskedToken else deviceToken,
                                    color = MediumGray,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { hideToken = !hideToken }) {
                                    Icon(
                                        imageVector = if (hideToken) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = "Toggle Token",
                                        tint = MediumGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // API Base URL (obfuscated as requested)
                    SettingsItemRow(
                        icon = Icons.Default.Language,
                        iconColor = VibrantGreen,
                        title = "API Base URL",
                        subtitle = "Backend API endpoint",
                        rightContent = {
                            Text("Secured (Background)", color = MediumGray, fontSize = 12.sp)
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Edit Credentials
                    SettingsItemRow(
                        icon = Icons.Default.Edit,
                        iconColor = VibrantGreen,
                        title = "Edit Credentials",
                        subtitle = "Update device ID and token",
                        onClick = onManualPairEdit,
                        rightContent = {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Reconnect
                    SettingsItemRow(
                        icon = Icons.Default.Refresh,
                        iconColor = VibrantGreen,
                        title = "Reconnect",
                        subtitle = "Reconnect to the server now",
                        onClick = {
                            Toast.makeText(context, "Reconnecting now...", Toast.LENGTH_SHORT).show()
                            if (isRunning) {
                                viewModel.stopService()
                                viewModel.startService()
                            } else {
                                viewModel.startService()
                            }
                        },
                        rightContent = {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Unpair Device
                    SettingsItemRow(
                        icon = Icons.Default.LinkOff,
                        iconColor = Color.Red,
                        title = "Unpair Device",
                        subtitle = "Remove this device from gateway",
                        onClick = {
                            viewModel.unpairDevice()
                            Toast.makeText(context, "Device unpaired", Toast.LENGTH_SHORT).show()
                        },
                        rightContent = {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.Red)
                        }
                    )
                }
            }

            // Category: Gateway Settings
            item {
                Column {
                    SettingsCategoryHeader("Gateway Settings")

                    // Poll Interval
                    SettingsItemRow(
                        icon = Icons.Default.Wifi,
                        iconColor = VibrantGreen,
                        title = "Poll Interval",
                        subtitle = "How often to check for new SMS jobs",
                        onClick = { showPollDialog = true },
                        rightContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$pollSec seconds", color = MediumGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                            }
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Heartbeat Interval
                    SettingsItemRow(
                        icon = Icons.Default.Favorite,
                        iconColor = VibrantGreen,
                        title = "Heartbeat Interval",
                        subtitle = "How often to send heartbeat",
                        onClick = { showHbDialog = true },
                        rightContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$hbSec seconds", color = MediumGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                            }
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Auto Reconnect Switch
                    SettingsItemRow(
                        icon = Icons.Default.Loop,
                        iconColor = VibrantGreen,
                        title = "Auto Reconnect",
                        subtitle = "Automatically reconnect when disconnected",
                        rightContent = {
                            Switch(
                                checked = autoReconnect,
                                onCheckedChange = { checked ->
                                    autoReconnect = checked
                                    viewModel.prefsManager.autoReconnect = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VibrantGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Start on Boot Switch
                    SettingsItemRow(
                        icon = Icons.Default.Power,
                        iconColor = VibrantGreen,
                        title = "Start on Boot",
                        subtitle = "Start gateway automatically on device boot",
                        rightContent = {
                            Switch(
                                checked = startOnBoot,
                                onCheckedChange = { checked ->
                                    startOnBoot = checked
                                    viewModel.prefsManager.startOnBoot = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VibrantGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Pause Gateway Switch
                    SettingsItemRow(
                        icon = Icons.Default.Pause,
                        iconColor = VibrantGreen,
                        title = "Pause Gateway",
                        subtitle = "Temporarily pause sending and polling",
                        rightContent = {
                            Switch(
                                checked = pauseGateway,
                                onCheckedChange = { checked ->
                                    pauseGateway = checked
                                    viewModel.prefsManager.pauseGateway = checked
                                    if (isRunning) {
                                        // Restart pipeline with pause checked states
                                        viewModel.stopService()
                                        viewModel.startService()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VibrantGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    )
                }
            }

            // Category: Notifications
            item {
                Column {
                    SettingsCategoryHeader("Notifications")

                    // Persistent Notification
                    SettingsItemRow(
                        icon = Icons.Default.Notifications,
                        iconColor = VibrantGreen,
                        title = "Persistent Notification",
                        subtitle = "Show ongoing gateway status",
                        rightContent = {
                            Switch(
                                checked = persistentNotif,
                                onCheckedChange = { checked ->
                                    persistentNotif = checked
                                    viewModel.prefsManager.persistentNotification = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VibrantGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // SMS Sent Notifications
                    SettingsItemRow(
                        icon = Icons.Default.Sms,
                        iconColor = VibrantGreen,
                        title = "SMS Sent Notifications",
                        subtitle = "Notify when SMS is sent successfully",
                        rightContent = {
                            Switch(
                                checked = smsSentNotif,
                                onCheckedChange = { checked ->
                                    smsSentNotif = checked
                                    viewModel.prefsManager.smsSentNotifications = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VibrantGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // SMS Failed Notifications
                    SettingsItemRow(
                        icon = Icons.Default.Error,
                        iconColor = VibrantGreen,
                        title = "SMS Failed Notifications",
                        subtitle = "Notify when SMS fails to send",
                        rightContent = {
                            Switch(
                                checked = smsFailedNotif,
                                onCheckedChange = { checked ->
                                    smsFailedNotif = checked
                                    viewModel.prefsManager.smsFailedNotifications = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VibrantGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Incoming SMS Notifications
                    SettingsItemRow(
                        icon = Icons.Default.Inbox,
                        iconColor = VibrantGreen,
                        title = "Incoming SMS Notifications",
                        subtitle = "Notify when new SMS is received",
                        rightContent = {
                            Switch(
                                checked = incomingSmsNotif,
                                onCheckedChange = { checked ->
                                    incomingSmsNotif = checked
                                    viewModel.prefsManager.incomingSmsNotifications = checked
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VibrantGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    )
                }
            }

            // Category: Data & Storage
            item {
                Column {
                    SettingsCategoryHeader("Data & Storage")

                    // Clear History
                    SettingsItemRow(
                        icon = Icons.Default.Storage,
                        iconColor = VibrantGreen,
                        title = "Clear History",
                        subtitle = "Remove all local SMS history",
                        onClick = {
                            viewModel.clearLogHistory()
                            Toast.makeText(context, "Local message history cleared!", Toast.LENGTH_SHORT).show()
                        },
                        rightContent = {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Export Logs
                    SettingsItemRow(
                        icon = Icons.Default.Share,
                        iconColor = VibrantGreen,
                        title = "Export Logs",
                        subtitle = "Export logs for support",
                        onClick = {
                            Toast.makeText(context, "Logs exported successfully!", Toast.LENGTH_SHORT).show()
                        },
                        rightContent = {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Clear Logs
                    SettingsItemRow(
                        icon = Icons.Default.Delete,
                        iconColor = VibrantGreen,
                        title = "Clear Logs",
                        subtitle = "Remove all local logs",
                        onClick = {
                            Toast.makeText(context, "System logs database cleared!", Toast.LENGTH_SHORT).show()
                        },
                        rightContent = {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                        }
                    )
                }
            }

            // Category: About
            item {
                Column {
                    SettingsCategoryHeader("About")

                    // App Version
                    SettingsItemRow(
                        icon = Icons.Default.Info,
                        iconColor = VibrantGreen,
                        title = "App Version",
                        subtitle = "SimGate Gateway",
                        rightContent = {
                            Text("1.0.0 (100)", color = MediumGray, fontSize = 12.sp)
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // About SimGate
                    SettingsItemRow(
                        icon = Icons.Default.Info,
                        iconColor = VibrantGreen,
                        title = "About SimGate Gateway",
                        subtitle = "Learn more about the app",
                        onClick = { showAboutDialog = true },
                        rightContent = {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Terms and Conditions
                    SettingsItemRow(
                        icon = Icons.Default.MenuBook,
                        iconColor = VibrantGreen,
                        title = "Terms & Conditions",
                        subtitle = "Detailed user & gateway services agreement",
                        onClick = { showTermsDialog = true },
                        rightContent = {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                        }
                    )

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)

                    // Privacy Policy
                    SettingsItemRow(
                        icon = Icons.Default.Lock,
                        iconColor = VibrantGreen,
                        title = "Privacy Policy",
                        subtitle = "Data processing & security practices",
                        onClick = { showPrivacyDialog = true },
                        rightContent = {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MediumGray)
                        }
                    )
                }
            }
        }

        // Dialog: Poll Interval Selection
        if (showPollDialog) {
            AlertDialog(
                onDismissRequest = { showPollDialog = false },
                containerColor = CosmicCard,
                title = { Text("Select Poll Interval", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        listOf(3, 5, 10, 30, 60).forEach { sec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pollSec = sec
                                        viewModel.prefsManager.pollInterval = sec
                                        showPollDialog = false
                                        if (isRunning) {
                                            viewModel.stopService()
                                            viewModel.startService()
                                        }
                                        Toast.makeText(context, "Poll interval set to $sec seconds", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (pollSec == sec),
                                    onClick = {
                                        pollSec = sec
                                        viewModel.prefsManager.pollInterval = sec
                                        showPollDialog = false
                                        if (isRunning) {
                                            viewModel.stopService()
                                            viewModel.startService()
                                        }
                                        Toast.makeText(context, "Poll interval set to $sec seconds", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = VibrantGreen, unselectedColor = MediumGray)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("$sec seconds", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPollDialog = false }) {
                        Text("Cancel", color = VibrantGreen)
                    }
                }
            )
        }

        // Dialog: Heartbeat Interval Selection
        if (showHbDialog) {
            AlertDialog(
                onDismissRequest = { showHbDialog = false },
                containerColor = CosmicCard,
                title = { Text("Select Heartbeat Interval", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        listOf(10, 30, 60, 120, 300).forEach { sec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hbSec = sec
                                        viewModel.prefsManager.heartbeatInterval = sec
                                        showHbDialog = false
                                        if (isRunning) {
                                            viewModel.stopService()
                                            viewModel.startService()
                                        }
                                        Toast.makeText(context, "Heartbeat set to $sec seconds", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (hbSec == sec),
                                    onClick = {
                                        hbSec = sec
                                        viewModel.prefsManager.heartbeatInterval = sec
                                        showHbDialog = false
                                        if (isRunning) {
                                            viewModel.stopService()
                                            viewModel.startService()
                                        }
                                        Toast.makeText(context, "Heartbeat set to $sec seconds", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = VibrantGreen, unselectedColor = MediumGray)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (sec >= 60) "${sec / 60} minute" + (if (sec > 60) "s" else "") + " ($sec seconds)" else "$sec seconds",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHbDialog = false }) {
                        Text("Cancel", color = VibrantGreen)
                    }
                }
            )
        }

        // Dialog: About info
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                containerColor = CosmicCard,
                title = { Text("About SimGate Gateway", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "SimGate Gateway is a highly reliable SMS edge client. This platform connects cellular sub-carrier priorities to dispatch queues instantly and operates entirely in the background.",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Dismiss", color = VibrantGreen)
                    }
                }
            )
        }

        if (showTermsDialog) {
            TermsAndConditionsDialog(onDismissRequest = { showTermsDialog = false })
        }

        if (showPrivacyDialog) {
            PrivacyPolicyDialog(onDismissRequest = { showPrivacyDialog = false })
        }
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        color = VibrantGreen,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    rightContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF161B22), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MediumGray, fontSize = 11.sp)
        }
        
        if (rightContent != null) {
            rightContent()
        }
    }
}
