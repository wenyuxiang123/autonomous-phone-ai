package com.autonomous.phone.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ModelConfig(
    val modelName: String,
    val modelUrl: String,
    val modelPath: String,
    val maxContextSize: Int,
    val temperature: Float,
    val modelSize: Long
)

data class DownloadProgress(
    val downloaded: Long,
    val total: Long,
    val percentage: Int
)

object ModelManager {
    private const val TAG = "ModelManager"
    
    val TEST_MODEL = ModelConfig(
        modelName = "TestModel",
        modelUrl = "https://github.com/wenyuxiang123/autonomous-phone-ai/raw/main/README.md",
        modelPath = "",
        maxContextSize = 512,
        temperature = 0.7f,
        modelSize = 10L * 1024 // 10KB minimum
    )

    val MINICPM_V2_5_INT4 = ModelConfig(
        modelName = "MiniCPM-V-2_5-Int4",
        modelUrl = "https://huggingface.co/openbmbai/MiniCPM-V-2_5-Int4/resolve/main/model.gguf",
        modelPath = "",
        maxContextSize = 4096,
        temperature = 0.7f,
        modelSize = 3L * 1024 * 1024 * 1024 // ~3GB
    )

    val QWEN2_5_1_5B_INT4 = ModelConfig(
        modelName = "Qwen2.5-1.5B-Int4",
        modelUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5_1.5b_instruct_q4_0.gguf",
        modelPath = "",
        maxContextSize = 2048,
        temperature = 0.7f,
        modelSize = 1L * 1024 * 1024 * 1024 // ~1GB
    )

    private var currentConfig: ModelConfig? = null
    private var downloadProgressListener: ((DownloadProgress) -> Unit)? = null

    fun init(context: Context) {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        Log.d(TAG, "Models directory: ${modelsDir.absolutePath}")
    }

    fun getModelPath(context: Context, modelName: String): String {
        return File(context.getExternalFilesDir(null), "models/$modelName").absolutePath
    }

    fun isModelDownloaded(context: Context, config: ModelConfig): Boolean {
        val modelFile = File(getModelPath(context, config.modelName))
        val exists = modelFile.exists() && modelFile.length() >= config.modelSize
        Log.d(TAG, "Checking if model ${config.modelName} is downloaded: $exists (size: ${modelFile.length()} / ${config.modelSize})")
        return exists
    }

    suspend fun downloadModel(
        context: Context,
        config: ModelConfig,
        progressListener: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        downloadProgressListener = progressListener
        currentConfig = config
        
        val modelFile = File(getModelPath(context, config.modelName))
        val tempFile = File(modelFile.absolutePath + ".tmp")
        
        // Delete existing temp file if any
        if (tempFile.exists()) {
            tempFile.delete()
        }
        
        try {
            Log.d(TAG, "Starting download from ${config.modelUrl}")
            val url = URL(config.modelUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error code: $responseCode")
                Handler(Looper.getMainLooper()).post {
                    progressListener(DownloadProgress(0, config.modelSize, 0))
                }
                return@withContext false
            }
            
            val contentLength = connection.contentLength.toLong()
            val totalSize = if (contentLength > 0) contentLength else config.modelSize
            Log.d(TAG, "Content length: $contentLength, using total size: $totalSize")

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)
            
            val buffer = ByteArray(8192)
            var downloaded = 0L
            var bytesRead: Int
            var lastReportedProgress = -1
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                
                // Calculate and report progress
                val percentage = if (totalSize > 0) {
                    ((downloaded * 100) / totalSize).toInt().coerceIn(0, 100)
                } else {
                    ((downloaded * 100) / config.modelSize).toInt().coerceIn(0, 99)
                }
                
                // Only report progress if it changed
                if (percentage != lastReportedProgress) {
                    lastReportedProgress = percentage
                    val progress = DownloadProgress(
                        downloaded = downloaded,
                        total = totalSize,
                        percentage = percentage
                    )
                    Handler(Looper.getMainLooper()).post {
                        downloadProgressListener?.invoke(progress)
                    }
                    Log.d(TAG, "Download progress: $percentage% ($downloaded / $totalSize)")
                }
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()
            
            Log.d(TAG, "Download completed, temp file size: ${tempFile.length()}")
            
            // Verify downloaded file
            if (tempFile.exists() && tempFile.length() > 0) {
                if (modelFile.exists()) {
                    modelFile.delete()
                }
                val success = tempFile.renameTo(modelFile)
                if (success) {
                    Log.d(TAG, "Model downloaded successfully: ${modelFile.absolutePath}, size: ${modelFile.length()}")
                    // Report 100% progress
                    Handler(Looper.getMainLooper()).post {
                        downloadProgressListener?.invoke(DownloadProgress(config.modelSize, config.modelSize, 100))
                    }
                    return@withContext true
                } else {
                    Log.e(TAG, "Failed to rename temp file to model file")
                    return@withContext false
                }
            } else {
                Log.e(TAG, "Downloaded file is empty or not found")
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Handler(Looper.getMainLooper()).post {
                progressListener(DownloadProgress(0, config.modelSize, 0))
            }
            return@withContext false
        }
    }

    fun deleteModel(context: Context, config: ModelConfig) {
        val modelFile = File(getModelPath(context, config.modelName))
        if (modelFile.exists()) {
            modelFile.delete()
            Log.d(TAG, "Deleted model: ${modelFile.absolutePath}")
        }
    }

    fun getAvailableModels(context: Context): List<ModelConfig> {
        return listOf(TEST_MODEL, MINICPM_V2_5_INT4, QWEN2_5_1_5B_INT4)
    }

    fun getDownloadedModels(context: Context): List<ModelConfig> {
        return getAvailableModels(context).filter { isModelDownloaded(context, it) }
    }

    data class ModelStatus(
        val modelName: String,
        val downloaded: Boolean,
        val fileSize: Long?,
        val requiredSpace: Long
    )

    fun getModelStatus(context: Context, config: ModelConfig): ModelStatus {
        val modelFile = File(getModelPath(context, config.modelName))
        return ModelStatus(
            modelName = config.modelName,
            downloaded = isModelDownloaded(context, config),
            fileSize = if (modelFile.exists()) modelFile.length() else null,
            requiredSpace = config.modelSize
        )
    }
}
