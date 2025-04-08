package com.example.gemma3chatbot

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class LlmService(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var initialized = false
    private var isGenerating = false
    private val systemPrompt = "You are a helpful AI chatbot using Gemma 3 model."


    suspend fun initialize(onStatusUpdate: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        if (initialized) return@withContext true

        try {
            onStatusUpdate("Initializing model...")
            val modelPath = copyModelFromAssetsIfNeeded("gemma3-1B-it-int4.task")
            Log.d("LlmInferenceHelper", "Model path: $modelPath")
            val options = LlmInferenceOptions.builder()
                .setMaxTokens(1000)
                .setModelPath(modelPath)
                .setPreferredBackend(LlmInference.Backend.CPU)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            initialized = true
            Log.d("LlmInferenceHelper", "Model initialized successfully")
            onStatusUpdate("Ready to chat!")
            true
        } catch (e: Exception) {
            Log.e("LlmInferenceHelper", "Initialization failed", e)
            onStatusUpdate("Initialization failed")
            false
        }
    }

    fun generateResponseAsync(userInput: String, onStatusUpdate: (String) -> Unit, onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!initialized) {
            onError("LLM not initialized yet. Please try again.")
            return
        }

        if (isGenerating) {
            onError("Please wait, generating previous response...")
            return
        }

        isGenerating = true
        onStatusUpdate("Generating response...")
        try {
            val formattedInput = "$systemPrompt\nUser: $userInput\nAssistant:"
            llmInference?.generateResponseAsync(formattedInput, object : ProgressListener<String> {
                private val stringBuilder = StringBuilder()

                override fun run(partialResult: String, done: Boolean) {
                    stringBuilder.append(partialResult)
                    onStatusUpdate(stringBuilder.toString())

                    if (done) {
                        isGenerating = false
                        onStatusUpdate("Ready to chat!")
                        onResult(stringBuilder.toString())
                    }
                }
            })
        } catch (e: Exception) {
            isGenerating = false
            Log.e("LlmInferenceHelper", "Generation failed (Ask Gemini)", e)
            onStatusUpdate("Error occurred")
            onError("An error occurred: ${e.message}")
        }
    }

    fun close() {
        llmInference?.close()
        initialized = false
    }

    private fun copyModelFromAssetsIfNeeded(assetFileName: String): String {
        val modelFile = File(context.filesDir, assetFileName)
        if (modelFile.exists()) return modelFile.absolutePath

        try {
            context.assets.open(assetFileName).use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
            return modelFile.absolutePath
        } catch (e: IOException) {
            Log.e("LlmInferenceHelper", "Model copy failed", e)
            throw IOException("Failed to copy model file from assets", e)
        }
    }
}
