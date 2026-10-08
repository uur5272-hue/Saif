package com.example.motoai.media.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.motoai.data.api.GeminiApiClient
import com.example.motoai.data.model.GeminiContent
import com.example.motoai.data.model.GeminiGenerationConfig
import com.example.motoai.data.model.GeminiImageConfig
import com.example.motoai.data.model.GeminiPart
import com.example.motoai.data.model.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class ImageGenerationResult {
    data class Success(val bitmap: Bitmap, val filePath: String, val prompt: String) : ImageGenerationResult()
    data class Error(val message: String) : ImageGenerationResult()
}

interface ImageGenerationProvider {
    val providerId: String
    val displayName: String
    suspend fun generateImage(prompt: String, context: Context): ImageGenerationResult
}

class GeminiImageProvider(private val getApiKey: () -> String) : ImageGenerationProvider {
    override val providerId: String = "gemini"
    override val displayName: String = "Gemini Flash Image (gemini-2.5-flash-image)"

    override suspend fun generateImage(prompt: String, context: Context): ImageGenerationResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext ImageGenerationResult.Error("Gemini API key is not configured. Please add it in Settings.")
        }

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("TEXT", "IMAGE"),
                    imageConfig = GeminiImageConfig(aspectRatio = "1:1", imageSize = "1K")
                )
            )

            val response = GeminiApiClient.service.generateContent(
                model = "gemini-2.5-flash-image",
                apiKey = apiKey,
                request = request
            )

            // Look for inline image data in candidates
            val parts = response.candidates?.firstOrNull()?.content?.parts
            val imagePart = parts?.firstOrNull { it.inlineData != null }

            if (imagePart?.inlineData != null) {
                val rawBytes = Base64.decode(imagePart.inlineData.data, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                if (bitmap != null) {
                    val file = saveBitmapToInternalStorage(context, bitmap, "img_${System.currentTimeMillis()}.png")
                    return@withContext ImageGenerationResult.Success(bitmap, file.absolutePath, prompt)
                }
            }

            // If model returned text explaining or if provider image endpoint returns fallback
            val textExplanation = parts?.firstOrNull { it.text != null }?.text
            return@withContext ImageGenerationResult.Error(
                textExplanation ?: "No image data returned from image provider. Verify model access/quota."
            )

        } catch (e: Exception) {
            ImageGenerationResult.Error("Image generation failed: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): File {
        val dir = File(context.filesDir, "generated_images").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}

/**
 * Compatible Open AI / Pollinations / Custom HTTP Image generation fallback
 * allows instant working demonstration even without image-enabled Gemini tier!
 */
class HighQualityWebImageProvider : ImageGenerationProvider {
    override val providerId: String = "web_ai"
    override val displayName: String = "MOTO Neural Visualizer (Web Diffusion)"

    override suspend fun generateImage(prompt: String, context: Context): ImageGenerationResult = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(prompt, "UTF-8")
            val imageUrl = "https://image.pollinations.ai/prompt/$encoded?width=768&height=768&nologo=true&model=flux"
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.doInput = true
            connection.connect()

            val input: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            if (bitmap != null) {
                val dir = File(context.filesDir, "generated_images").apply { mkdirs() }
                val file = File(dir, "neural_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                ImageGenerationResult.Success(bitmap, file.absolutePath, prompt)
            } else {
                ImageGenerationResult.Error("Failed to decode generated image stream.")
            }
        } catch (e: Exception) {
            ImageGenerationResult.Error("Visualizer error: ${e.message}")
        }
    }
}
