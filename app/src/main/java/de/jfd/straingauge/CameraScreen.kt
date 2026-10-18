package de.jfd.straingauge

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun CameraScreen(model:AppModel) {
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted=it}
    val gallery=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){it?.let(model::pickPhoto)}
    val preview=remember{PreviewView(context)}
    val capture=remember{ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()}
    var provider by remember{mutableStateOf<ProcessCameraProvider?>(null)}
    var ready by remember{mutableStateOf(false)}
    var taking by remember{mutableStateOf(false)}
    val cameraActive=remember{java.util.concurrent.atomic.AtomicBoolean(true)}
    LaunchedEffect(Unit){if(!granted)permission.launch(Manifest.permission.CAMERA)}
    DisposableEffect(granted,owner){
        var active=true
        cameraActive.set(true)
        if(granted){
            val future=ProcessCameraProvider.getInstance(context)
            future.addListener({if(active)try{
                provider=future.get()
                val p=Preview.Builder().build().also{it.surfaceProvider=preview.surfaceProvider}
                provider?.unbindAll();provider?.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,p,capture)
                ready=true
            }catch(e:Exception){model.message="Kamera nicht verfügbar: ${e.message}"}},ContextCompat.getMainExecutor(context))
        }
        onDispose{active=false;cameraActive.set(false);provider?.unbindAll();ready=false}
    }
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Wochenblatt scannen",style=MaterialTheme.typography.headlineSmall)
        Text("Linke OnePager-Seite vollständig und möglichst eben aufnehmen. Alle vier L-Marken und der Punkt oben links müssen sichtbar sein.")
        if(granted)AndroidView(factory={preview},modifier=Modifier.weight(1f).fillMaxWidth())
        else {
            Text("Für die Aufnahme bitte den Kamerazugriff erlauben. Alternativ kannst du ein vorhandenes Foto öffnen.")
            Button(onClick={permission.launch(Manifest.permission.CAMERA)}){Text("Kamerazugriff erlauben")}
            Spacer(Modifier.weight(1f))
        }
        Button(enabled=ready && !taking && !model.busy,onClick={
            taking=true
            capture.targetRotation=preview.display?.rotation ?: android.view.Surface.ROTATION_0
            val file=File.createTempFile("capture-",".jpg",context.cacheDir)
            capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),ContextCompat.getMainExecutor(context),object:ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(result:ImageCapture.OutputFileResults){taking=false;if(cameraActive.get())model.analyze(file) else file.delete()}
                override fun onError(exception:ImageCaptureException){taking=false;file.delete();if(cameraActive.get())model.message=exception.message}
            })
        },modifier=Modifier.fillMaxWidth()){Text(if(taking)"Aufnahme …" else "Foto aufnehmen")}
        OutlinedButton(onClick={gallery.launch("image/*")},enabled=!taking && !model.busy,modifier=Modifier.fillMaxWidth()){Text("Vorhandenes Foto öffnen")}
    }
}
