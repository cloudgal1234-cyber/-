package com.taskalarm.app.ui.task

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.taskalarm.app.data.ImageFeatures
import java.util.concurrent.Executors

/** One analysed camera frame: its features and (optionally) an upright thumbnail. */
class Frame(val features: ImageFeatures, val thumbnail: Bitmap?)

/**
 * Live back-camera preview that reports the [ImageFeatures] of about two frames per second.
 * Everything runs on the device (ML Kit's bundled labeling model); frames are never stored
 * unless the caller keeps a [Frame.thumbnail].
 */
@Composable
fun PhotoCameraView(modifier: Modifier = Modifier, keepThumbnails: Boolean = false, onFrame: (Frame) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onFrame)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder().setConfidenceThreshold(0.3f).build()
        )
        val preview = Preview.Builder().build()
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
        analysis.setAnalyzer(executor, FeatureAnalyzer(labeler, keepThumbnails) { callback(it) })

        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            try {
                val p = providerFuture.get().also { provider = it }
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val selector = when {
                    p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }
                if (selector == null) {
                    error = "לא נמצאה מצלמה במכשיר"
                } else {
                    p.unbind(preview, analysis)
                    camera = p.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                }
            } catch (e: Exception) {
                Log.e("PhotoCamera", "Camera bind failed", e)
                error = "לא ניתן להפעיל את המצלמה"
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { provider?.unbind(preview, analysis) }
            analysis.clearAnalyzer()
            executor.shutdown()
            labeler.close()
            camera = null
        }
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            FilledIconButton(
                onClick = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                },
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    if (torchOn) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn,
                    contentDescription = if (torchOn) "כבה פנס" else "הדלק פנס",
                    tint = Color.White
                )
            }
        }
        error?.let {
            Text(
                it, color = Color.White, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        }
    }
}

private class FeatureAnalyzer(
    private val labeler: ImageLabeler,
    private val keepThumbnails: Boolean,
    private val onFrame: (Frame) -> Unit,
) : ImageAnalysis.Analyzer {
    private var last = 0L
    @Volatile private var busy = false

    override fun analyze(proxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (busy || now - last < 450) {
            proxy.close(); return
        }
        last = now
        val rotation = proxy.imageInfo.rotationDegrees
        val raw = try {
            proxy.toBitmap()
        } catch (e: Exception) {
            proxy.close(); return
        }
        proxy.close()

        val upright = if (rotation != 0) {
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        } else raw

        // Centre 80 % of the frame, downscaled: where the user aims the object.
        val side = (minOf(upright.width, upright.height) * 0.8f).toInt()
        val crop = Bitmap.createBitmap(upright, (upright.width - side) / 2, (upright.height - side) / 2, side, side)
        val small = Bitmap.createScaledBitmap(crop, 48, 48, true)
        val pixels = IntArray(48 * 48).also { small.getPixels(it, 0, 48, 0, 0, 48, 48) }
        val hist = ImageFeatures.histogram(pixels, 48, 48)
        val thumb = if (keepThumbnails) Bitmap.createScaledBitmap(crop, 480, 480, true) else null

        busy = true
        labeler.process(InputImage.fromBitmap(crop, 0))
            .addOnSuccessListener { labels ->
                onFrame(Frame(ImageFeatures(labels.associate { it.text to it.confidence }, hist), thumb))
            }
            .addOnFailureListener { onFrame(Frame(ImageFeatures(emptyMap(), hist), thumb)) }
            .addOnCompleteListener { busy = false }
    }
}
