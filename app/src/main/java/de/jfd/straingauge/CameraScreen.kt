package de.jfd.straingauge

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraScreen(
    model: AppModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showHint by rememberSaveable { mutableStateOf(true) }
    var ready by remember { mutableStateOf(false) }
    var taking by remember { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        granted = it
    }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(model::pickPhoto)
    }

    val preview = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER

            // Die Vorschau lässt sich zuverlässig mit Compose überlagern.
            implementationMode =
                PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val capture = remember {
        ImageCapture.Builder()
            .setCaptureMode(
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            )
            .build()
    }

    val cameraActive = remember { AtomicBoolean(false) }

    LaunchedEffect(Unit) {
        if (!granted) {
            permission.launch(Manifest.permission.CAMERA)
        }
    }

    // Weiße Systemsymbole passen zur Kameraansicht.
    // Beim Verlassen stellen wir den vorherigen Zustand wieder her.
    DisposableEffect(view) {
        val window = context.findCameraActivity()?.window

        val controller = window?.let {
            WindowInsetsControllerCompat(it, view)
        }

        val previousStatus =
            controller?.isAppearanceLightStatusBars
        val previousNavigation =
            controller?.isAppearanceLightNavigationBars

        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false

        onDispose {
            if (controller != null) {
                previousStatus?.let {
                    controller.isAppearanceLightStatusBars = it
                }
                previousNavigation?.let {
                    controller.isAppearanceLightNavigationBars = it
                }
            }
        }
    }

    // Kamera an den Lebenszyklus des Bildschirms binden.
    DisposableEffect(granted, owner, preview) {
        var active = true
        var provider: ProcessCameraProvider? = null

        ready = false
        cameraActive.set(granted)

        if (granted) {
            val future = ProcessCameraProvider.getInstance(context)

            future.addListener(
                {
                    if (active) {
                        try {
                            val cameraProvider = future.get()
                            provider = cameraProvider

                            val livePreview = Preview.Builder()
                                .build()
                                .also {
                                    it.surfaceProvider =
                                        preview.surfaceProvider
                                }

                            cameraProvider.unbindAll()

                            cameraProvider.bindToLifecycle(
                                owner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                livePreview,
                                capture
                            )

                            ready = true
                        } catch (exception: Exception) {
                            model.message =
                                tr(R.string.camera_camera_unavailable, exception.message)
                        }
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }

        onDispose {
            active = false
            cameraActive.set(false)
            provider?.unbindAll()
            ready = false
        }
    }

    fun takePhoto() {
        if (!ready || taking || model.busy) return

        taking = true

        capture.targetRotation =
            preview.display?.rotation ?: Surface.ROTATION_0

        try {
            val file = File.createTempFile(
                "capture-",
                ".jpg",
                context.cacheDir
            )

            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(
                        result: ImageCapture.OutputFileResults
                    ) {
                        taking = false

                        if (cameraActive.get()) {
                            model.analyze(file)
                        } else {
                            file.delete()
                        }
                    }

                    override fun onError(
                        exception: ImageCaptureException
                    ) {
                        taking = false
                        file.delete()

                        if (cameraActive.get()) {
                            model.message =
                                tr(R.string.camera_could_not_take_the_photo, exception.message)
                        }
                    }
                }
            )
        } catch (exception: Exception) {
            taking = false
            model.message =
                tr(R.string.camera_could_not_take_the_photo, exception.message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Ebene 1: Das Live-Bild füllt den gesamten Bildschirm.
        if (granted) {
            AndroidView(
                factory = { preview },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Ebene 2: Bedienelemente mit Abstand zu den Systemleisten.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp)
        ) {
            // Zurück-Pfeil und schließbarer Hinweis.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = !taking && !model.busy,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = tr(R.string.action_back),
                        tint = Color.White
                    )
                }

                if (showHint) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 8.dp,
                                end = 4.dp,
                                bottom = 12.dp
                            ),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 8.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tr(R.string.camera_scan_weekly_sheet),
                                    style =
                                        MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = tr(R.string.camera_capture_the_entire_left_onepager_page_as_flat),
                                    style =
                                        MaterialTheme.typography.bodyMedium
                                )
                            }

                            IconButton(
                                onClick = { showHint = false }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = tr(R.string.camera_dismiss_instructions),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Ersatzansicht bei fehlender Kameraberechtigung.
            if (!granted) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(vertical = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = tr(R.string.camera_allow_camera_access_or_use_the_image_icon),
                        color = Color.White
                    )

                    Button(
                        onClick = {
                            permission.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Text(tr(R.string.camera_allow_camera_access))
                    }
                }
            }

            // Untere Bedienleiste: symmetrische Plätze halten
            // den Auslöser unabhängig vom Bild-Button mittig.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.size(56.dp))

                FilledIconButton(
                    onClick = { takePhoto() },
                    enabled = ready && !taking && !model.busy,
                    modifier = Modifier
                        .size(80.dp)
                        .semantics {
                            contentDescription = tr(R.string.camera_take_photo)
                        }
                        .border(
                            width = 3.dp,
                            color = Color.White,
                            shape = CircleShape
                        ),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            Color.White.copy(alpha = 0.35f)
                    )
                ) {
                    if (taking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(
                                    color = LocalContentColor.current,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { gallery.launch("image/*") },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White
                ) {
                    IconButton(
                        onClick = { gallery.launch("image/*") },
                        enabled = !taking && !model.busy
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = tr(R.string.camera_open_existing_photo)
                        )
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findCameraActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findCameraActivity()
        else -> null
    }
}