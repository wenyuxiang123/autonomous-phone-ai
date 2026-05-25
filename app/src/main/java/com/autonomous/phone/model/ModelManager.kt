package com.autonomous.phone.model

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ModelConfig(
    val modelName: String = "MiniCPM-V-2_5-Int4",
    val maxContextSize: Int = 4096,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f
)

class ModelManager(private val context: Context) {

    private val modelDir: File by lazy {
        File(context.getExternalFilesDir(null), "models").apply {
            if (!exists()) mkdirs()
        }
    }

    val defaultConfig = ModelConfig()

    fun getModelPath(config: ModelConfig): File {
        return File(modelDir, "${config.modelName}.gguf")
    }

    fun isModelDownloaded(config: ModelConfig): Boolean {
        return getModelPath(config).exists()
    }

    suspend fun downloadModel(
        config: ModelConfig,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val modelPath = getModelPath(config)
            
            val downloadUrls = listOf(
                "https://hf-mirror.com/openbmb/MiniCPM-V-2_5-gguf/resolve/main/MiniCPM-V-2_5-int4.gguf",
                "https://modelscope.cn/models/OpenBMB/MiniCPM-V-2_5-gguf/resolve/master/MiniCPM-V-2_5-int4.gguf"
            )

            var success = false
            var lastException: Exception? = null

            for (url in downloadUrls) {
                try {
                    downloadFile(url, modelPath, onProgress)
                    success = true
                    break
                } catch (e: Exception) {
                    lastException = e
                    continue
                }
            }

            if (success) {
                Result.success(modelPath)
            } else {
                Result.failure(lastException ?: Exception("All download sources failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun downloadFile(url: String, destination: File, onProgress: (Int) -> Unit) {
        val tempFile = File(destination.parent, "${destination.name}.tmp")
        
        java.net.URL(url).openConnection().apply {
            connectTimeout = 30000
            readTimeout = 30000
        }.getInputStream().use { input ->
            tempFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Long = 0
                val contentLength = java.net.URL(url).openConnection().contentLengthLong
                var read: Int

                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesRead += read
                    if (contentLength > 0) {
                        val progress = (bytesRead * 100 / contentLength).toInt()
                        onProgress(progress)
                    }
                }
            }
        }

        tempFile.renameTo(destination)
    }

    fun deleteModel(config: ModelConfig): Boolean {
        return getModelPath(config).delete()
    }
}
