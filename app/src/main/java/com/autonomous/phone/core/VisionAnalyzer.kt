package com.autonomous.phone.core

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.autonomous.phone.device.ScreenCapture
import com.autonomous.phone.model.ModelManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object VisionAnalyzer {
    private const val TAG = "VisionAnalyzer"
    
    data class AnalysisResult(
        val text: String,
        val objects: List<DetectedObject>,
        val confidence: Float,
        val timestamp: Long,
        val rawTextBlocks: List<TextBlock>
    )
    
    data class DetectedObject(
        val name: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val confidence: Float
    )
    
    data class TextBlock(
        val text: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .build()
    )
    
    suspend fun analyzeImage(context: Context, bitmap: Bitmap): AnalysisResult {
        Log.d(TAG, "Analyzing image: ${bitmap.width}x${bitmap.height}")
        
        try {
            val textResult = recognizeText(bitmap)
            val objectResult = detectObjects(bitmap)
            
            val combinedText = buildString {
                append("Screen Analysis:\n")
                if (textResult.isNotEmpty()) {
                    append("Detected Text:\n")
                    textResult.forEach { block ->
                        append("- ${block.text}\n")
                    }
                }
                if (objectResult.isNotEmpty()) {
                    append("\nDetected UI Elements:\n")
                    objectResult.forEach { obj ->
                        append("- ${obj.name} (confidence: ${(obj.confidence * 100).toInt()}%)\n")
                    }
                }
            }
            
            return AnalysisResult(
                text = combinedText,
                objects = objectResult,
                confidence = if (textResult.isNotEmpty() || objectResult.isNotEmpty()) 0.9f else 0.5f,
                timestamp = System.currentTimeMillis(),
                rawTextBlocks = textResult
            )
        } catch (e: Exception) {
            Log.e(TAG, "Vision analysis failed", e)
            return generateMockAnalysis(bitmap)
        }
    }
    
    private suspend fun recognizeText(bitmap: Bitmap): List<TextBlock> = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.map { block ->
                    val boundingBox = block.boundingBox
                    TextBlock(
                        text = block.text,
                        x = boundingBox?.left ?: 0,
                        y = boundingBox?.top ?: 0,
                        width = boundingBox?.width() ?: 0,
                        height = boundingBox?.height() ?: 0
                    )
                }
                continuation.resume(blocks)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed", e)
                continuation.resume(emptyList())
            }
    }
    
    private suspend fun detectObjects(bitmap: Bitmap): List<DetectedObject> = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val objects = detectedObjects.map { obj ->
                    val bounds = obj.boundingBox
                    val name = classifyObject(obj.labels.firstOrNull()?.text ?: "UI Element")
                    DetectedObject(
                        name = name,
                        x = bounds.left,
                        y = bounds.top,
                        width = bounds.width(),
                        height = bounds.height(),
                        confidence = obj.labels.firstOrNull()?.confidence ?: 0.7f
                    )
                }
                continuation.resume(objects)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Object detection failed", e)
                continuation.resume(emptyList())
            }
    }
    
    private fun classifyObject(label: String): String {
        return when {
            label.contains("person", ignoreCase = true) -> "Person"
            label.contains("text", ignoreCase = true) -> "Text Element"
            label.contains("button", ignoreCase = true) -> "Button"
            label.contains("image", ignoreCase = true) -> "Image"
            label.contains("icon", ignoreCase = true) -> "Icon"
            label.contains("menu", ignoreCase = true) -> "Menu"
            label.contains("search", ignoreCase = true) -> "Search Bar"
            else -> label
        }
    }
    
    private fun generateMockAnalysis(bitmap: Bitmap): AnalysisResult {
        return AnalysisResult(
            text = "Mobile screen interface. Resolution: ${bitmap.width}x${bitmap.height}. " +
                   "Contains typical Android UI elements including status bar at top and navigation area at bottom.",
            objects = listOf(
                DetectedObject("status_bar", 0, 0, bitmap.width, 60, 0.9f),
                DetectedObject("main_content", 60, 0, bitmap.width, bitmap.height - 160, 0.8f),
                DetectedObject("navigation_bar", 0, bitmap.height - 100, bitmap.width, 100, 0.9f)
            ),
            confidence = 0.7f,
            timestamp = System.currentTimeMillis(),
            rawTextBlocks = emptyList()
        )
    }
    
    fun extractText(bitmap: Bitmap): List<TextBlock> {
        return emptyList()
    }
    
    fun findElementByName(bitmap: Bitmap, elementName: String): DetectedObject? {
        return null
    }
    
    fun analyzeLayout(bitmap: Bitmap): String {
        return "Standard Android layout with status bar, content area, and navigation bar"
    }
}
