package com.autonomous.phone.model

import android.content.Context
import android.os.Environment
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
    }

    fun getModelPath(context: Context, modelName: String): String {
        return File(context.getExternalFilesDir(null), "models/$modelName.gguf").absolutePath
    }

    fun isModelDownloaded(context: Context, config: ModelConfig): Boolean {
        val modelFile = File(getModelPath(context, config.modelName))
        return modelFile.exists() && modelFile.length() >= config.modelSize * 0.9 // Allow 10% margin
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
        
        try {
            val url = URL(config.modelUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            
            val contentLength = connection.contentLength.toLong()
            if (contentLength <= 0) {
                Log.e(TAG, "Content length is 0 or negative")
                return@withContext false
            }

            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)
            
            val buffer = ByteArray(8192)
            var downloaded = 0L
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                
                val progress = DownloadProgress(
                    downloaded = downloaded,
                    total = contentLength,
                    percentage = ((downloaded * 100) / contentLength).toInt()
                )
                downloadProgressListener?.invoke(progress)
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            if (tempFile.length() >= config.modelSize * 0.9) {
                if (modelFile.exists()) {
                    modelFile.delete()
                }
                tempFile.renameTo(modelFile)
                Log.d(TAG, "Model downloaded successfully: ${modelFile.absolutePath}")
                return@withContext true
            } else {
                Log.e(TAG, "Downloaded file too small: ${tempFile.length()} / ${config.modelSize}")
                tempFile.delete()
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            if (tempFile.exists()) {
                tempFile.delete()
            }
            return@withContext false
        }
    }

    fun deleteModel(context: Context, config: ModelConfig) {
        val modelFile = File(getModelPath(context, config.modelName))
        if (modelFile.exists()) {
            modelFile.delete()
        }
    }

    fun getAvailableModels(context: Context): List<ModelConfig> {
        return listOf(MINICPM_V2_5_INT4, QWEN2_5_1_5B_INT4)
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
