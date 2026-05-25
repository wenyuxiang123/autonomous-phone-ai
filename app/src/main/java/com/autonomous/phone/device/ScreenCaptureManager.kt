package com.autonomous.phone.device

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.nio.ByteBuffer

class ScreenCaptureManager(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var projectionResultCode: Int = 0
    private var projectionData: Intent? = null
    private val handler = Handler(Looper.getMainLooper())

    fun setProjectionResult(resultCode: Int, data: Intent) {
        projectionResultCode = resultCode
        projectionData = data
    }

    fun hasProjectionPermission(): Boolean {
        return projectionData != null
    }

    fun requestProjectionPermission(activity: Activity, requestCode: Int) {
        val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
        activity.startActivityForResult(projectionManager.createScreenCaptureIntent(), requestCode)
    }

    fun startCapture(): Boolean {
        if (mediaProjection != null) {
            return true
        }

        val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
        val data = projectionData ?: return false

        mediaProjection = projectionManager.getMediaProjection(projectionResultCode, data)

        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val density = displayMetrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )

        return true
    }

    fun capture(): Bitmap? {
        val reader = imageReader ?: return null

        try {
            val image = reader.acquireLatestImage() ?: return null

            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * reader.width

            val bitmap = Bitmap.createBitmap(
                reader.width + rowPadding / pixelStride,
                reader.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            image.close()

            return Bitmap.createBitmap(bitmap, 0, 0, reader.width, reader.height)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun stopCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    fun isCapturing(): Boolean {
        return mediaProjection != null
    }
}
