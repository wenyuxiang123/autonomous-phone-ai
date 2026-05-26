package com.autonomous.phone.device

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.nio.ByteBuffer

object ScreenCapture {
    private const val TAG = "ScreenCapture"
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    private const val VIRTUAL_DISPLAY_NAME = "AutoControlVirtualDisplay"
    private const val IMAGE_READER_MAX_IMAGES = 2

    fun initialize(context: Context, resultCode: Int, data: Intent) {
        try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (projectionManager == null) {
                Log.e(TAG, "MediaProjectionManager not available")
                return
            }
            
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            val displayMetrics = context.resources.displayMetrics
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
            
            handlerThread = HandlerThread("ScreenCaptureThread").apply { start() }
            handler = Handler(handlerThread!!.looper)
            Log.d(TAG, "ScreenCapture initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ScreenCapture", e)
        }
    }

    fun capture(): Bitmap? {
        val projection = mediaProjection ?: return null
        
        try {
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, IMAGE_READER_MAX_IMAGES)
            
            virtualDisplay = projection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null,
                handler
            )
            
            Thread.sleep(100)
            
            val image = imageReader!!.acquireLatestImage() ?: return null
            
            try {
                val buffer = image.planes[0].buffer
                val pixelStride = image.planes[0].pixelStride
                val rowStride = image.planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth
                
                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                val croppedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    screenWidth,
                    screenHeight
                )
                
                if (croppedBitmap != bitmap) {
                    bitmap.recycle()
                }
                
                return croppedBitmap
            } finally {
                image.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen", e)
            return null
        } finally {
            releaseVirtualDisplay()
        }
    }

    fun captureWithRect(rect: Rect): Bitmap? {
        val fullScreenshot = capture() ?: return null
        
        try {
            return Bitmap.createBitmap(
                fullScreenshot,
                rect.left.coerceAtLeast(0),
                rect.top.coerceAtLeast(0),
                rect.width().coerceAtMost(fullScreenshot.width),
                rect.height().coerceAtMost(fullScreenshot.height)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen with rect", e)
            return null
        } finally {
            fullScreenshot.recycle()
        }
    }

    fun isAvailable(): Boolean {
        return mediaProjection != null
    }

    fun release() {
        releaseVirtualDisplay()
        try {
            imageReader?.close()
            mediaProjection?.stop()
            handlerThread?.quitSafely()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ScreenCapture", e)
        }
        
        imageReader = null
        mediaProjection = null
        virtualDisplay = null
        handlerThread = null
        handler = null
    }

    private fun releaseVirtualDisplay() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtual display", e)
        }
    }
}
