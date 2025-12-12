package com.roubao.autopilot.controller

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.math.max

object MediaProjectionScreenshotter {

    private val screenshotMutex = Mutex()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted

    @Volatile
    private var permissionResultCode: Int? = null

    @Volatile
    private var permissionData: Intent? = null

    @Volatile
    private var mediaProjection: MediaProjection? = null

    @Volatile
    private var handlerThread: HandlerThread? = null

    @Volatile
    private var handler: Handler? = null

    @Volatile
    private var imageReader: ImageReader? = null

    @Volatile
    private var virtualDisplay: VirtualDisplay? = null

    @Volatile
    private var width: Int = 0

    @Volatile
    private var height: Int = 0

    @Volatile
    private var densityDpi: Int = 0

    fun createScreenCaptureIntent(context: Context): Intent {
        val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return mgr.createScreenCaptureIntent()
    }

    fun setPermission(resultCode: Int, data: Intent) {
        permissionResultCode = resultCode
        permissionData = data
        _permissionGranted.value = true
    }

    fun clearPermission() {
        permissionResultCode = null
        permissionData = null
        _permissionGranted.value = false
        stop()
    }

    fun stop() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null

        val projection = mediaProjection
        mediaProjection = null
        try {
            projection?.stop()
        } catch (_: Exception) {
        }

        val thread = handlerThread
        handlerThread = null
        handler = null
        try {
            thread?.quitSafely()
        } catch (_: Exception) {
            try {
                thread?.quit()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun takeScreenshot(context: Context, timeoutMs: Long = 1500): Bitmap? {
        val appContext = context.applicationContext
        return screenshotMutex.withLock {
            if (!ensureSession(appContext)) {
                return@withLock null
            }
            val reader = imageReader ?: return@withLock null
            val image = reader.acquireLatestImage() ?: awaitImage(reader, timeoutMs) ?: return@withLock null
            try {
                imageToBitmap(image, width, height)
            } finally {
                try {
                    image.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun ensureSession(context: Context): Boolean {
        ensureHandlerThread()
        val projection = ensureProjection(context) ?: return false

        val (w, h, dpi) = getScreenSize(context)
        if (w <= 0 || h <= 0 || dpi <= 0) {
            return false
        }
        if (virtualDisplay != null && imageReader != null && w == width && h == height && dpi == densityDpi) {
            return true
        }

        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null

        width = w
        height = h
        densityDpi = dpi

        val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        virtualDisplay = projection.createVirtualDisplay(
            "baozi_screen",
            w,
            h,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )
        return true
    }

    private fun ensureHandlerThread() {
        if (handlerThread != null && handler != null) {
            return
        }
        val thread = HandlerThread("MediaProjectionScreenshot").also { it.start() }
        handlerThread = thread
        handler = Handler(thread.looper)
    }

    private fun ensureProjection(context: Context): MediaProjection? {
        mediaProjection?.let { return it }

        val resultCode = permissionResultCode ?: return null
        val data = permissionData ?: return null

        val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = try {
            mgr.getMediaProjection(resultCode, data)
        } catch (_: Exception) {
            null
        } ?: return null

        mediaProjection = projection
        try {
            projection.registerCallback(
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        stop()
                    }
                },
                handler
            )
        } catch (_: Exception) {
        }
        return projection
    }

    private fun getScreenSize(context: Context): Triple<Int, Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dpi = context.resources.displayMetrics.densityDpi
        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        return Triple(metrics.widthPixels, metrics.heightPixels, dpi)
    }

    private suspend fun awaitImage(reader: ImageReader, timeoutMs: Long): Image? {
        val callbackHandler = handler ?: return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val listener = ImageReader.OnImageAvailableListener {
                    reader.setOnImageAvailableListener(null, null)
                    val image = try {
                        reader.acquireLatestImage()
                    } catch (_: Exception) {
                        null
                    }
                    if (cont.isActive) {
                        cont.resume(image)
                    } else {
                        try {
                            image?.close()
                        } catch (_: Exception) {
                        }
                    }
                }

                reader.setOnImageAvailableListener(listener, callbackHandler)
                cont.invokeOnCancellation {
                    try {
                        reader.setOnImageAvailableListener(null, null)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = max(0, rowStride - pixelStride * width)
        val fullBitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        buffer.rewind()
        fullBitmap.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(fullBitmap, 0, 0, width, height)
        fullBitmap.recycle()
        return cropped
    }
}
