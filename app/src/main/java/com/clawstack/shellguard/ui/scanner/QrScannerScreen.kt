package com.clawstack.shellguard.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clawstack.shellguard.engine.ParsedTotpData
import com.clawstack.shellguard.engine.TotpUriParser
import com.clawstack.shellguard.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onCodeScanned: (ParsedTotpData) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        var found = false
                        for (barcode in barcodes) {
                            if (barcode.valueType == Barcode.TYPE_TEXT || barcode.valueType == Barcode.TYPE_URL) {
                                barcode.rawValue?.let { code ->
                                    val parsed = TotpUriParser.parse(code)
                                    if (parsed != null) {
                                        found = true
                                        onCodeScanned(parsed)
                                    }
                                }
                            }
                        }
                        if (!found) {
                            scanErrorMessage = "No valid TOTP QR code found in selected image."
                        }
                    }
                    .addOnFailureListener {
                        scanErrorMessage = "Failed to process image: ${it.localizedMessage}"
                    }
            } catch (e: Exception) {
                scanErrorMessage = "Could not load image: ${e.localizedMessage}"
            }
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan TOTP QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val nextState = !isTorchOn
                            camera?.cameraControl?.enableTorch(nextState)
                            isTorchOn = nextState
                        }
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Torch",
                            tint = if (isTorchOn) BrandClawCyan else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OceanDark.copy(alpha = 0.85f),
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Color.Black,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val analyzer = QrCodeAnalyzer { rawQrString ->
                                val parsed = TotpUriParser.parse(rawQrString)
                                if (parsed != null) {
                                    onCodeScanned(parsed)
                                } else {
                                    scanErrorMessage = "Unsupported QR code format"
                                }
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, analyzer)
                                }

                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                scanErrorMessage = "Camera initialization failed: ${e.message}"
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                val infiniteTransition = rememberInfiniteTransition(label = "laser_anim")
                val scanLineProgress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scan_line"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val boxSize = 260.dp.toPx()
                    val left = (size.width - boxSize) / 2
                    val top = (size.height - boxSize) / 2 - 40.dp.toPx()
                    val right = left + boxSize
                    val bottom = top + boxSize

                    val cornerLength = 32.dp.toPx()
                    val strokeWidth = 4.dp.toPx()
                    val cornerColor = BrandClawCyan

                    drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
                    drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
                    drawLine(cornerColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
                    drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)
                    drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
                    drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)
                    drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
                    drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)

                    val currentLaserY = top + (boxSize * scanLineProgress)
                    drawLine(
                        color = ReefPink.copy(alpha = 0.85f),
                        start = Offset(left + 8.dp.toPx(), currentLaserY),
                        end = Offset(right - 8.dp.toPx(), currentLaserY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera Permission Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera access is needed to scan QR codes directly from your screen or paper backup.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = ReefPink)
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(OceanDark.copy(alpha = 0.90f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (scanErrorMessage != null) {
                    Text(
                        text = scanErrorMessage ?: "",
                        color = WarningText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Text(
                    text = "Align the QR code within the frame to scan",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandClawCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandClawCyan),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Image from Gallery", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
